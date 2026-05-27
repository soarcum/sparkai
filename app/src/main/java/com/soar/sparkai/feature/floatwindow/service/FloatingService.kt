package com.soar.sparkai.feature.floatwindow.service

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.soar.sparkai.MainActivity
import com.soar.sparkai.core.theme.AppTheme
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.feature.floatwindow.ui.FloatingWidget
import com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity
import com.soar.sparkai.feature.floatwindow.util.NotificationHelper
import com.soar.sparkai.feature.floatwindow.util.ScreenshotCache
import com.soar.sparkai.feature.floatwindow.util.ScreenshotHelper
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * 悬浮窗核心前台服务
 * 
 * 作用：承载系统的常驻通知，管理悬浮窗在 WindowManager 中的挂载与卸载，
 * 并响应来自 Compose UI 的拖动、吸边、展开控制和截图逻辑。
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
    }

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var composeView: ComposeView

    // 实现自定义 LifecycleOwner 以保证 ComposeView 挂载在 WindowManager 时能正常进行生命周期驱动
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        // 1. 声明并注册前台服务通知，以符合 Android 保活要求
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

        // 2. 初始化生命周期和状态恢复组件，使 Compose 能够在 Service 级别正常渲染
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        AppLogger.i("FloatingService", "悬浮服务已创建 (onCreate)，正在初始化系统级悬浮窗口。")

        // 3. 初始化窗口管理器和布局参数
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initLayoutParams()

        // 4. 创建 ComposeView 并挂载到 WindowManager
        initComposeView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        val action = intent?.action ?: ACTION_START

        AppLogger.i("FloatingService", "收到前台服务启动指令 (onStartCommand)，动作类型: $action")

        when (action) {
            ACTION_START -> {
                showFloatingWindow()
            }
            ACTION_TAKE_SCREENSHOT -> {
                // 截图第一步：隐藏悬浮窗，避免其自身被截图捕获
                hideFloatingWindow()
                // Android 14+ (UPSIDE_DOWN_CAKE) 强制要求每次录屏投影前必须重新请求用户授权，不可复用缓存 token
                val useCache = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ScreenshotCache.hasPermission
                if (useCache) {
                    // 已有缓存授权，直接跳过弹窗执行截图
                    AppLogger.i("FloatingService", "命中授权缓存，直接开始截图，无需弹窗。")
                    performScreenCapture(ScreenshotCache.getResultCode(), ScreenshotCache.getResultData())
                } else {
                    // 无缓存或运行在 Android 14+，启动透明 Activity 弹出系统录屏授权对话框
                    AppLogger.i("FloatingService", "需要重新获取授权，弹出系统录屏授权对话框。")
                    val startActIntent = Intent(this, ScreenshotActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    }
                    startActivity(startActIntent)
                }
            }
            ACTION_TAKE_SCREENSHOT_RESULT -> {
                // 截图第二步：获取到用户授权的 Intent 数据后，先缓存再截图
                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
                val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != 0 && resultData != null) {
                    // 保存授权结果，后续截图可直接复用，无需再次弹窗
                    ScreenshotCache.save(resultCode, resultData)
                    performScreenCapture(resultCode, resultData)
                } else {
                    showFloatingWindow()
                }
            }
            ACTION_CANCEL_SCREENSHOT -> {
                // 用户拒绝了授权，恢复悬浮球显示
                showFloatingWindow()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * 执行实际截图逻辑（供缓存命中和首次授权两个路径共用）
     *
     * 负责前台服务类型升级、调用截图工具、截图完成后降级并恢复悬浮球。
     * 若授权已失效（Android 14+ 单次使用限制），自动清除缓存并提示用户重新授权。
     */
    private fun performScreenCapture(resultCode: Int, resultData: Intent) {
        // Android 10+ 强制要求截图前将前台服务升级为 mediaProjection 类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notification = NotificationHelper.buildNotification(this)
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }
        ScreenshotHelper.captureScreen(this, resultCode, resultData) { success ->
            if (!success) {
                // 截图失败可能是因为旧的授权 Token 在 Android 14+ 上已单次失效，清除缓存
                AppLogger.w("FloatingService", "截图失败，清除授权缓存，下次将重新请求授权。")
                ScreenshotCache.clear()
            }
            // 截图完成后，将前台服务降级还原，释放媒体投影资源占用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notification = NotificationHelper.buildNotification(this)
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val notification = NotificationHelper.buildNotification(this)
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
            // 截图完成或异常退出后，重新拉起悬浮球
            showFloatingWindow()
        }
    }

    /**
     * 初始化 WindowManager 布局参数
     *
     * 关键配置：
     * - TYPE_APPLICATION_OVERLAY：Android 8.0 之后统一的悬浮窗层级类型
     * - FLAG_NOT_FOCUSABLE：不能获取焦点，以避免拦截底层其他应用的软键盘输入与系统返回键
     * - PixelFormat.TRANSLUCENT：透明通道以支持精美的圆角和毛玻璃效果
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
            // 初始挂载位置：屏幕右侧靠中
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
            // 建立完整的 ViewTree 所有者依赖，这对 Compose 必不可少
            setViewTreeLifecycleOwner(this@FloatingService)
            setViewTreeViewModelStoreOwner(this@FloatingService)
            setViewTreeSavedStateRegistryOwner(this@FloatingService)

            setContent {
                AppTheme {
                    FloatingWidget(
                        onDrag = { dx, dy ->
                            // 拖拽手势响应：更新悬浮窗位置
                            updateWindowPosition(this@FloatingService.layoutParams.x + dx, this@FloatingService.layoutParams.y + dy)
                        },
                        onDragEnd = {
                            // 拖拽抬手响应：平滑吸附到最近的屏幕边缘
                            performSnappingAnimation()
                        },
                        onActionScreenshot = {
                            // 触发截图流程
                            val startSelfIntent = Intent(this@FloatingService, FloatingService::class.java).apply {
                                action = ACTION_TAKE_SCREENSHOT
                            }
                            startService(startSelfIntent)
                        },
                        onActionClose = {
                            // 关闭服务
                            stopSelf()
                        },
                        onActionBackToApp = {
                            // 返回主界面
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

    /**
     * 更新悬浮窗在屏幕上的坐标位置
     */
    private fun updateWindowPosition(x: Int, y: Int) {
        if (::composeView.isInitialized && composeView.parent != null) {
            val metrics = resources.displayMetrics
            // 简单限制坐标越界，保持悬浮球在有效可视范围之内
            layoutParams.x = x.coerceIn(0, metrics.widthPixels - composeView.width)
            layoutParams.y = y.coerceIn(0, metrics.heightPixels - composeView.height)
            windowManager.updateViewLayout(composeView, layoutParams)
        }
    }

    /**
     * 执行平滑的智能吸边动画
     */
    private fun performSnappingAnimation() {
        if (!::composeView.isInitialized || composeView.parent == null) return
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val viewWidth = composeView.width

        // 判断当前位置距离左半屏近还是右半屏近，计算出目标 X 坐标
        val targetX = if (layoutParams.x + viewWidth / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - viewWidth
        }

        AppLogger.i("FloatingService", "悬浮球智能吸边动画触发。当前 X: ${layoutParams.x}，目标 X: $targetX")

        // 使用属性动画进行平滑的平移过渡
        val animator = ValueAnimator.ofInt(layoutParams.x, targetX).apply {
            duration = 300
            interpolator = DecelerateInterpolator() // 减速插值器，提供高端优雅的吸附回弹感
            addUpdateListener { animation ->
                val animX = animation.animatedValue as Int
                updateWindowPosition(animX, layoutParams.y)
            }
        }
        animator.start()
    }

    /**
     * 隐藏悬浮窗（设为不可见且不占触摸通道）
     */
    private fun hideFloatingWindow() {
        if (::composeView.isInitialized && composeView.parent != null) {
            AppLogger.i("FloatingService", "隐藏悬浮窗口。状态 -> 不可见 (GONE)")
            composeView.visibility = View.GONE
        }
    }

    /**
     * 显示悬浮窗
     */
    private fun showFloatingWindow() {
        if (::composeView.isInitialized) {
            if (composeView.parent == null) {
                AppLogger.i("FloatingService", "初次挂载 Compose 悬浮卡片视图到系统窗口层。")
                windowManager.addView(composeView, layoutParams)
            } else {
                AppLogger.i("FloatingService", "显示悬浮球。状态 -> 可见 (VISIBLE)")
                composeView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        AppLogger.i("FloatingService", "前台服务即将销毁 (onDestroy)，正在释放悬浮窗及系统窗口资源。")
        isServiceRunning = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        // 销毁时清理挂载的窗口，释放内存，避免 Activity/Service 泄漏
        if (::composeView.isInitialized && composeView.parent != null) {
            windowManager.removeView(composeView)
        }
        super.onDestroy()
    }
}
