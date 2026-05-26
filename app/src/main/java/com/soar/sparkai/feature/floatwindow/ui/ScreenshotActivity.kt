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

/**
 * 屏幕截图授权透明中转 Activity
 * 
 * 作用：由于系统的 MediaProjectionManager 录屏/截图授权对话框必须依附于 Activity 弹出，
 * 我们设计了这个透明且无感知的 Activity。它在启动后立即申请授权，并在得到结果后立即将数据回传给
 * FloatingService 前台服务，随后自动销毁。
 */
class ScreenshotActivity : ComponentActivity() {

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
            // 延迟 300ms 销毁透明中转 Activity，确保前台服务有充分的时间在系统 Binder 中注册投影，防止时序竞争导致 Binder Token 失效
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finish()
                overridePendingTransition(0, 0)
            }, 300)
        } else {
            // 用户取消了授权，通知悬浮窗服务重新展示悬浮球
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
}
