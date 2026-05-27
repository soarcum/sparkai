package com.soar.sparkai.feature.floatwindow.util

import android.content.Intent

/**
 * 屏幕录制授权结果全局缓存
 *
 * 作用：将用户第一次同意的授权数据（resultCode + resultData）持久保存在内存中，
 * 后续截图操作直接复用，避免每次都弹出系统授权对话框。
 *
 * 注意：缓存在进程退出后会自动清除，不存在安全风险。
 */
object ScreenshotCache {

    private var cachedResultCode: Int = 0
    private var cachedResultData: Intent? = null

    /** 是否已持有有效授权 */
    val hasPermission: Boolean
        get() = cachedResultCode != 0 && cachedResultData != null

    /** 保存一次授权结果 */
    fun save(resultCode: Int, resultData: Intent) {
        cachedResultCode = resultCode
        cachedResultData = resultData
    }

    /** 获取缓存的授权码，仅在 hasPermission 为 true 时调用 */
    fun getResultCode(): Int = cachedResultCode

    /** 获取缓存的授权 Intent，仅在 hasPermission 为 true 时调用 */
    fun getResultData(): Intent = requireNotNull(cachedResultData)

    /** 主动清除缓存（例如授权失效时调用） */
    fun clear() {
        cachedResultCode = 0
        cachedResultData = null
    }
}
