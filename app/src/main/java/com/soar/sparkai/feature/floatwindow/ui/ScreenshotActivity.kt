package com.soar.sparkai.feature.floatwindow.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.soar.sparkai.feature.floatwindow.service.FloatingService
import java.lang.ref.WeakReference

/**
 * 屏幕截图授权透明中转 Activity
 * 
 * 作用：由于系统的 MediaProjectionManager 录屏/截图授权对话框必须依附于 Activity 弹出，
 * 我们设计了这个透明且无感知的 Activity。它在启动后立即申请授权，并在得到结果后立即将数据回传给
 * FloatingService 前台服务，由前台服务初始化投屏后再回调销毁自身。
 */
class ScreenshotActivity : ComponentActivity() {

    companion object {
        private var activeActivityRef: WeakReference<ScreenshotActivity>? = null

        /**
         * 静态方法：供前台服务在成功创建 MediaProjection 并升级前台服务后安全销毁此透明 Activity。
         * 这确保了 Activity 能够 100% 保持在最前台，直到 MediaProjection 实例被系统绑定完成，彻底规避 Android 14+ 启动 FGS 的时序竞争。
         */
        fun finishActivity() {
            activeActivityRef?.get()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.runOnUiThread {
                        activity.finish()
                        activity.overridePendingTransition(0, 0)
                    }
                }
            }
            activeActivityRef = null
        }
    }

    // 注册 Activity 结果协议，用来替代老旧的 onActivityResult
    private val requestScreenshotLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data

        if (resultCode == Activity.RESULT_OK && data != null) {
            // 用户点击了“立即开始”，授权成功
            val serviceIntent = Intent(this, FloatingService::class.java).apply {
                action = FloatingService.ACTION_TAKE_SCREENSHOT_RESULT
                putExtra(FloatingService.EXTRA_RESULT_CODE, resultCode)
                putExtra(FloatingService.EXTRA_RESULT_DATA, data)
            }
            // 回传授权数据给 FloatingService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            // 注意：此处不再使用不稳定的 300ms 延时销毁。
            // 我们在 FloatingService 成功获取到 MediaProjection 实例后，再主动回调 finishActivity() 销毁此 Activity。
        } else {
            // 用户取消了授权，通知悬浮窗服务重新展示悬浮球并立即销毁
            val cancelIntent = Intent(this, FloatingService::class.java).apply {
                action = FloatingService.ACTION_CANCEL_SCREENSHOT
            }
            startService(cancelIntent)
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 禁用界面动画
        overridePendingTransition(0, 0)
        
        // 保存当前的弱引用实例
        activeActivityRef = WeakReference(this)

        // 请求录屏/截屏授权
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            val captureIntent = projectionManager.createScreenCaptureIntent()
            requestScreenshotLauncher.launch(captureIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果异常，通知服务恢复悬浮窗并销毁
            val cancelIntent = Intent(this, FloatingService::class.java).apply {
                action = FloatingService.ACTION_CANCEL_SCREENSHOT
            }
            startService(cancelIntent)
            finish()
        }
    }

    override fun onDestroy() {
        // 清理当前持有的弱引用，防止内存泄露
        if (activeActivityRef?.get() == this) {
            activeActivityRef = null
        }
        super.onDestroy()
    }
}
