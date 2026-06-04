package com.soar.sparkai.feature.floatwindow.service

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.soar.sparkai.MainActivity
import com.soar.sparkai.core.theme.AppTheme
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.feature.floatwindow.ui.FloatingWidget
import com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity
import com.soar.sparkai.feature.floatwindow.util.NotificationHelper
import com.soar.sparkai.feature.floatwindow.util.ScreenshotCache
import com.soar.sparkai.feature.floatwindow.util.ScreenshotHelper
import com.soar.sparkai.feature.ai.util.AamsModuleManager
import com.soar.sparkai.feature.floatwindow.util.AamsFullscreenOverlayManager
import com.soar.sparkai.feature.floatwindow.util.AamsMatchTesterManager
import com.soar.sparkai.feature.floatwindow.util.AamsPipelineExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 悬浮窗核心前台服务
 * 
 * 作用：承载系统的常驻通知，管理悬浮窗在 WindowManager 中的挂载与卸载，
 * 并驱动 AI 声明式自定义脚本模块（AAMS）分析、屏幕物理坐标霓虹圈画标注与全屏毛玻璃中控渲染。
 */
class FloatingService : Service(), ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        var isServiceRunning = false

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TAKE_SCREENSHOT = "ACTION_TAKE_SCREENSHOT"
        const val ACTION_TAKE_SCREENSHOT_RESULT = "ACTION_TAKE_SCREENSHOT_RESULT"
        const val ACTION_CANCEL_SCREENSHOT = "ACTION_CANCEL_SCREENSHOT"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        
        // 动态执行 AI 自定义模块动作标记
        const val ACTION_EXECUTE_MODULE = "ACTION_EXECUTE_MODULE"
        const val EXTRA_MODULE_ID = "EXTRA_MODULE_ID"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var composeView: ComposeView
    
    // AI 正在后台分析状态标识，用于悬浮球 Loading 指示
    private var isWidgetLoading by mutableStateOf(false)
    
    // 当前正在执行的 AAMS 自定义模块 ID
    private var activeModuleId: String = ""

    // 全局维持单例 MediaProjection 以及常驻虚拟投屏长连接管道
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = false
        val notification = NotificationHelper.buildNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        AppLogger.i("FloatingService", "悬浮服务已创建 (onCreate)，正在初始化系统级悬浮窗口。")

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initLayoutParams()
        initComposeView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        val action = intent?.action ?: ACTION_START

        AppLogger.i("FloatingService", "收到前台服务启动指令 (onStartCommand)，动作类型: $action")

        when (action) {
            ACTION_START -> {
                isServiceRunning = true
                showFloatingWindow()
            }
            ACTION_TAKE_SCREENSHOT -> {
                hideFloatingWindow()
                activeModuleId = ""
                
                val reader = imageReader
                val display = virtualDisplay
                if (reader != null && display != null) {
                    AppLogger.i("FloatingService", "复用全局常驻投屏长连接管道，直接进行后台静默截图")
                    performFastScreenCapture(isFirstTime = false)
                } else {
                    val useCache = ScreenshotCache.hasPermission
                    var projectionFromCache: MediaProjection? = null
                    if (useCache) {
                        try {
                            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            projectionFromCache = projectionManager.getMediaProjection(
                                ScreenshotCache.getResultCode(),
                                ScreenshotCache.getResultData()
                            )
                        } catch (e: Exception) {
                            AppLogger.e("FloatingService", "尝试通过缓存获取 MediaProjection 发生异常: ${e.message}", e)
                        }
                    }

                    if (projectionFromCache != null && initProjectionPipeline(projectionFromCache)) {
                        AppLogger.i("FloatingService", "根据缓存重新建立投屏长连接流水线，直接开始截图。")
                        performFastScreenCapture(isFirstTime = true)
                    } else {
                        AppLogger.i("FloatingService", "投屏长连接不存在且授权缓存失效，弹出系统录屏授权对话框。")
                        val startActIntent = Intent(this, ScreenshotActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        }
                        startActivity(startActIntent)
                    }
                }
            }
            ACTION_TAKE_SCREENSHOT_RESULT -> {
                AppLogger.i("FloatingService", "[SparkAI-Capture-v2] 收到屏幕截图授权回调 ACTION_TAKE_SCREENSHOT_RESULT。")
                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
                val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                
                if (resultCode != 0 && resultData != null) {
                    ScreenshotCache.save(resultCode, resultData)
                    
                    try {
                        val notification = NotificationHelper.buildNotification(this)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            startForeground(
                                NotificationHelper.NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(
                                NotificationHelper.NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                            )
                        } else {
                            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
                        }

                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val projection = projectionManager.getMediaProjection(resultCode, resultData)
                        
                        if (projection != null) {
                            if (initProjectionPipeline(projection)) {
                                // 核心时序保护：由于 MediaProjection 成功初始化且服务顺利提权，安全销毁透明中转 Activity
                                com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()
                                if (activeModuleId.isNotEmpty()) {
                                    performModuleCapture(activeModuleId, isFirstTime = true)
                                } else {
                                    performFastScreenCapture(isFirstTime = true)
                                }
                            } else {
                                com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()
                                Toast.makeText(this, "[SparkAI] 截图管道建立失败，请重试", Toast.LENGTH_LONG).show()
                                showFloatingWindow()
                            }
                        } else {
                            com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()
                            Toast.makeText(this, "[SparkAI] 获取媒体投屏实例失败，请重新授权", Toast.LENGTH_LONG).show()
                            showFloatingWindow()
                        }
                    } catch (e: SecurityException) {
                        com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()
                        AppLogger.e("FloatingService", "[SparkAI-Capture-v2] 发生系统安全异常 SecurityException", e)
                        Toast.makeText(this, "截图失败: 缺少媒体投屏特权，请查看日志详情", Toast.LENGTH_LONG).show()
                        showFloatingWindow()
                    } catch (e: Exception) {
                        com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()
                        AppLogger.e("FloatingService", "[SparkAI-Capture-v2] 截图回调发生未知运行时异常: ${e.message}", e)
                        Toast.makeText(this, "截图失败: 运行时异常 ${e.message}", Toast.LENGTH_LONG).show()
                        showFloatingWindow()
                    }
                } else {
                    com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()
                    showFloatingWindow()
                }
            }
            ACTION_CANCEL_SCREENSHOT -> {
                showFloatingWindow()
            }
            ACTION_EXECUTE_MODULE -> {
                hideFloatingWindow()
                val moduleId = intent?.getStringExtra(EXTRA_MODULE_ID) ?: ""
                AppLogger.i("FloatingService", "准备执行 AAMS 自定义脚本模块，ID: $moduleId")
                triggerModulePipeline(moduleId)
            }
            ACTION_STOP -> {
                isServiceRunning = false
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * 设置 MediaProjection 的生命周期监听，在其被系统停止时及时重置状态，保证下次截图能重新授权
     */
    private fun setupMediaProjectionCallback(projection: MediaProjection) {
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                AppLogger.w("FloatingService", "检测到 MediaProjection 已由系统或用户主动终止，清理本地长连接引用。")
                if (mediaProjection == projection) {
                    try {
                        virtualDisplay?.release()
                        imageReader?.close()
                    } catch (e: Exception) {
                        AppLogger.e("FloatingService", "释放投屏长连接资源时发生异常: ${e.message}", e)
                    }
                    virtualDisplay = null
                    imageReader = null
                    mediaProjection = null
                    projectionCallback = null

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val notification = NotificationHelper.buildNotification(this@FloatingService)
                        startForeground(
                            NotificationHelper.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    }
                }
            }
        }
        projection.registerCallback(callback, Handler(Looper.getMainLooper()))
        projectionCallback = callback
    }

    /**
     * 初始化常驻投屏长连接流水线（ImageReader & VirtualDisplay）
     */
    private fun initProjectionPipeline(projection: MediaProjection): Boolean {
        AppLogger.i("FloatingService", "[SparkAI-Capture-v2] 进入 initProjectionPipeline，开始构建投屏流水线管道...")
        return try {
            mediaProjection = projection
            setupMediaProjectionCallback(projection)

            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val dpi = metrics.densityDpi
            AppLogger.i("FloatingService", "[SparkAI-Capture-v2] 获取物理屏幕尺寸成功: width=$width, height=$height, dpi=$dpi")

            AppLogger.i("FloatingService", "[SparkAI-Capture-v2] 正在创建 ImageReader 实例 (RGBA_8888)...")
            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            AppLogger.i("FloatingService", "[SparkAI-Capture-v2] 正在调用 projection.createVirtualDisplay 创建虚拟显示...")
            val display = projection.createVirtualDisplay(
                "SparkAIScreenCapture",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                reader.surface,
                null,
                null
            )
            virtualDisplay = display
            AppLogger.i("FloatingService", "[SparkAI-Capture-v2] 已成功建立全局屏幕投屏长连接流水线（VirtualDisplay 开启常驻）。")
            true
        } catch (e: SecurityException) {
            AppLogger.e("FloatingService", "[SparkAI-Capture-v2] createVirtualDisplay 抛出安全异常(SecurityException)！异常原因: ${e.message}", e)
            false
        } catch (e: Exception) {
            AppLogger.e("FloatingService", "[SparkAI-Capture-v2] 建立全局投屏流水线时发生未知崩溃: ${e.message}", e)
            false
        }
    }

    /**
     * 利用全局长连接管道在后台执行极速抓图并保存
     */
    private fun performFastScreenCapture(isFirstTime: Boolean) {
        val reader = imageReader ?: return
        ScreenshotHelper.captureScreenFromPipeline(this, reader, isFirstTime) { success ->
            if (success) {
                AppLogger.i("FloatingService", "极速静默截图执行成功，图片已写入相册。")
            } else {
                AppLogger.w("FloatingService", "极速静默截图执行失败。")
            }
            showFloatingWindow()
        }
    }

    /**
     * 清理所有已挂载的全屏覆盖层
     */
    private fun clearOverlays() {
        AamsFullscreenOverlayManager.removeFullscreenOverlay(this)
        AamsMatchTesterManager.removeTesterOverlay(this)
    }

    /**
     * 触发特定 AI 模块求和/处理流程
     */
    private fun triggerModulePipeline(moduleId: String) {
        activeModuleId = moduleId
        val reader = imageReader
        val display = virtualDisplay
        if (reader != null && display != null) {
            AppLogger.i("FloatingService", "[AAMS] 复用全局管道，执行模块 ID: $moduleId")
            performModuleCapture(moduleId, isFirstTime = false)
        } else {
            val useCache = ScreenshotCache.hasPermission
            var projectionFromCache: MediaProjection? = null
            if (useCache) {
                try {
                    val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    projectionFromCache = projectionManager.getMediaProjection(
                        ScreenshotCache.getResultCode(),
                        ScreenshotCache.getResultData()
                    )
                } catch (e: Exception) {
                    AppLogger.e("FloatingService", "尝试通过缓存获取 MediaProjection 发生异常: ${e.message}", e)
                }
            }

            if (projectionFromCache != null && initProjectionPipeline(projectionFromCache)) {
                AppLogger.i("FloatingService", "[AAMS] 根据缓存重新建立管道，执行模块 ID: $moduleId")
                performModuleCapture(moduleId, isFirstTime = true)
            } else {
                AppLogger.i("FloatingService", "[AAMS] 弹出录屏授权，模块 ID: $moduleId")
                val startActIntent = Intent(this, ScreenshotActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivity(startActIntent)
            }
        }
    }

    private fun performModuleCapture(moduleId: String, isFirstTime: Boolean) {
        val module = AamsModuleManager.getAllModules(this).find { it.id == moduleId }
        if (module == null) {
            Toast.makeText(this, "未找到指定的 AI 自定义模块！", Toast.LENGTH_SHORT).show()
            showFloatingWindow()
            return
        }

        if (moduleId == "sys_alignment_tester") {
            clearOverlays()
            AamsMatchTesterManager.showAlignmentTesterOverlay(this, this)
            return
        }

        // 显示加载层并清空其他层
        clearOverlays()
        AamsFullscreenOverlayManager.showLoadingOverlay(this, this, module.name)

        CoroutineScope(Dispatchers.Main).launch {
            if (isFirstTime) {
                kotlinx.coroutines.delay(450)
            } else {
                kotlinx.coroutines.delay(100)
            }

            val accessibilityService = com.soar.sparkai.feature.accessibility.service.SparkAccessibilityService.instance
            val rootNode = accessibilityService?.rootInActiveWindow
            val textNodes = if (rootNode != null) {
                com.soar.sparkai.feature.accessibility.util.AccessibilityTextExtractor.extractVisibleTexts(rootNode)
            } else {
                emptyList()
            }

            // 博彩抽水模块：赔率为图像渲染，无障碍只会抓到状态栏等系统文本误导路由，故强制走视觉管道
            if (module.id == "sys_rake_calculator") {
                AppLogger.i("FloatingService", "[AAMS] 博彩抽水模块强制走视觉管道，跳过无障碍路由")
                val reader = imageReader
                if (reader != null) {
                    AamsPipelineExecutor.executeRakeVisionPipeline(
                        context = this@FloatingService,
                        service = this@FloatingService,
                        reader = reader,
                        module = module,
                        isFirstTime = isFirstTime,
                        setWidgetLoading = { isWidgetLoading = it },
                        showFloatingWindow = { showFloatingWindow() }
                    )
                } else {
                    Toast.makeText(this@FloatingService, "投屏截图管道未就绪，无法分析屏幕", Toast.LENGTH_SHORT).show()
                    AamsFullscreenOverlayManager.removeFullscreenOverlay(this@FloatingService)
                    showFloatingWindow()
                }
                return@launch
            }

            if (textNodes.isNotEmpty()) {
                AppLogger.i("FloatingService", "[AAMS] 检测到无障碍服务可用，已提取 ${textNodes.size} 个文本节点，进入高精度纯文本匹配管道")
                AamsPipelineExecutor.executeTextModePipeline(
                    context = this@FloatingService,
                    service = this@FloatingService,
                    module = module,
                    textNodes = textNodes,
                    setWidgetLoading = { isWidgetLoading = it },
                    showFloatingWindow = { showFloatingWindow() }
                )
            } else {
                AppLogger.i("FloatingService", "[AAMS] 无障碍服务未开启或未提取到文字，Fallback 传统截图多模态视觉管道")
                val reader = imageReader
                if (reader != null) {
                    AamsPipelineExecutor.executeVisionModePipeline(
                        context = this@FloatingService,
                        service = this@FloatingService,
                        reader = reader,
                        module = module,
                        isFirstTime = isFirstTime,
                        setWidgetLoading = { isWidgetLoading = it },
                        showFloatingWindow = { showFloatingWindow() }
                    )
                } else {
                    Toast.makeText(this@FloatingService, "投屏截图管道未就绪，无法分析屏幕", Toast.LENGTH_SHORT).show()
                    AamsFullscreenOverlayManager.removeFullscreenOverlay(this@FloatingService)
                    showFloatingWindow()
                }
            }
        }
    }

    /**
     * 初始化 WindowManager 布局参数
     */
    private fun initLayoutParams() {
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val metrics = resources.displayMetrics
            x = metrics.widthPixels - 150
            y = metrics.heightPixels / 2
        }
    }

    /**
     * 创建并注入 ComposeView 到系统的窗口层
     */
    private fun initComposeView() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingService)
            setViewTreeViewModelStoreOwner(this@FloatingService)
            setViewTreeSavedStateRegistryOwner(this@FloatingService)

            setContent {
                AppTheme {
                    FloatingWidget(
                        isLoading = this@FloatingService.isWidgetLoading,
                        onDrag = { dx, dy ->
                            updateWindowPosition(this@FloatingService.layoutParams.x + dx, this@FloatingService.layoutParams.y + dy)
                        },
                        onDragEnd = {
                            performSnappingAnimation()
                        },
                        onActionScreenshot = {
                            val startSelfIntent = Intent(this@FloatingService, FloatingService::class.java).apply {
                                action = ACTION_TAKE_SCREENSHOT
                            }
                            startService(startSelfIntent)
                        },
                        onActionExecuteModule = { moduleId ->
                            val startSelfIntent = Intent(this@FloatingService, FloatingService::class.java).apply {
                                action = ACTION_EXECUTE_MODULE
                                putExtra(EXTRA_MODULE_ID, moduleId)
                            }
                            startService(startSelfIntent)
                        },
                        onActionClose = {
                            stopSelf()
                        },
                        onActionBackToApp = {
                            val mainIntent = Intent(this@FloatingService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(mainIntent)
                        }
                    )
                }
            }
        }
    }

    private fun updateWindowPosition(x: Int, y: Int) {
        if (::composeView.isInitialized && composeView.parent != null) {
            val metrics = resources.displayMetrics
            layoutParams.x = x.coerceIn(0, metrics.widthPixels - composeView.width)
            layoutParams.y = y.coerceIn(0, metrics.heightPixels - composeView.height)
            windowManager.updateViewLayout(composeView, layoutParams)
        }
    }

    private fun performSnappingAnimation() {
        if (!::composeView.isInitialized || composeView.parent == null) return
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val viewWidth = composeView.width

        val targetX = if (layoutParams.x + viewWidth / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - viewWidth
        }

        val animator = ValueAnimator.ofInt(layoutParams.x, targetX).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val animX = animation.animatedValue as Int
                updateWindowPosition(animX, layoutParams.y)
            }
        }
        animator.start()
    }

    private fun hideFloatingWindow() {
        if (::composeView.isInitialized && composeView.parent != null) {
            composeView.visibility = View.GONE
        }
    }

    private fun showFloatingWindow() {
        if (::composeView.isInitialized) {
            if (composeView.parent == null) {
                windowManager.addView(composeView, layoutParams)
            } else {
                composeView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        AppLogger.i("FloatingService", "悬浮窗前台服务注销中...")
        isServiceRunning = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        
        clearOverlays()
        
        try {
            virtualDisplay?.release()
            imageReader?.close()
            projectionCallback?.let {
                mediaProjection?.unregisterCallback(it)
            }
            mediaProjection?.stop()
        } catch (e: Exception) {
            AppLogger.e("FloatingService", "断开投屏资源错误", e)
        }
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        projectionCallback = null

        if (::composeView.isInitialized && composeView.parent != null) {
            windowManager.removeView(composeView)
        }
        super.onDestroy()
    }
}
