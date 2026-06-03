package com.soar.sparkai.feature.accessibility.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.soar.sparkai.feature.accessibility.service.SparkAccessibilityService

/**
 * 无障碍服务辅助工具类
 */
object AccessibilityUtils {

    /**
     * 判断 SparkAI 自动化授权服务（无障碍服务）是否已经开启
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedService = context.packageName + "/" + SparkAccessibilityService::class.java.canonicalName
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            // 忽略
        }

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val enabledServicesSetting = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (enabledServicesSetting != null) {
                colonSplitter.setString(enabledServicesSetting)
                while (colonSplitter.hasNext()) {
                    val enabledService = colonSplitter.next()
                    if (enabledService.equals(expectedService, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
