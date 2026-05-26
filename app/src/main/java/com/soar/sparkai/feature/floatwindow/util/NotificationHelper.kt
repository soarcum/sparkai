package com.soar.sparkai.feature.floatwindow.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.soar.sparkai.MainActivity

/**
 * 悬浮窗前台服务通知辅助工具类
 * 
 * 作用：为 FloatingService 提供常驻前台所必需的通知（Notification）及通知通道（NotificationChannel）。
 * 依据：从 Android 8.0 (API 26) 开始，所有前台服务都必须绑定一个通知通道，否则系统会抛出异常。
 */
object NotificationHelper {

    private const val CHANNEL_ID = "sparkai_float_window_channel"
    private const val CHANNEL_NAME = "SparkAI 悬浮窗服务"
    private const val CHANNEL_DESC = "用于保持 SparkAI 悬浮窗后台常驻的系统通知"
    const val NOTIFICATION_ID = 10086

    /**
     * 创建前台服务通知
     * 
     * @param context 上下文
     * @return 构建完毕的 Notification 实例
     */
    fun buildNotification(context: Context): Notification {
        // 首先确保通知通道已在系统注册（仅针对 API 26+）
        createNotificationChannel(context)

        // 设置点击通知时的意图：直接返回应用主页面 MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建高颜值的通知样式
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("SparkAI 悬浮窗已启用")
            .setContentText("悬浮面板常驻运行中，点击可返回应用")
            // 使用系统默认小图标，或者项目内置的 ic_launcher
            .setSmallIcon(android.R.drawable.ic_menu_compass) 
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 设置为正在运行的通知，防止用户侧滑消除
            .setPriority(NotificationCompat.PRIORITY_LOW) // 设为低优先级，避免过度打扰用户
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * 创建并注册通知通道
     * 
     * 说明：Android 8.0+ 必须显式创建 NotificationChannel。
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 避免重复创建同一配置的通道
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW // 使用低重要度，防止弹出横幅，仅保留在通知栏
                ).apply {
                    description = CHANNEL_DESC
                    setShowBadge(false) // 悬浮窗服务不需要桌面图标角标
                    lockscreenVisibility = Notification.VISIBILITY_SECRET // 锁屏时不展示敏感内容
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
