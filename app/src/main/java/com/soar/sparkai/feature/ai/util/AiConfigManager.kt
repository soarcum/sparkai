package com.soar.sparkai.feature.ai.util

import android.content.Context

/**
 * AI 大模型设置项持久化管理器
 *
 * 作用：基于 Android 的 SharedPreferences 对 API Key、Base URL、
 * 默认模型以及截图分析 Preset Prompt 进行本地化高速缓存，且实现即时热更。
 */
object AiConfigManager {
    private const val PREF_NAME = "sparkai_settings"
    private const val KEY_API_KEY = "ai_api_key"
    private const val KEY_BASE_URL = "ai_base_url"
    private const val KEY_DEFAULT_MODEL = "ai_default_model"
    private const val KEY_PRESET_PROMPT = "ai_preset_prompt"

    private const val DEFAULT_API_KEY = "tp-cmpd87vmxq8e12ur2888ghlud88ju8kjhidfo3wugq5ogoj4"
    private const val DEFAULT_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1"
    private const val DEFAULT_MODEL = "mimo-v2.5-pro"
    private const val DEFAULT_PROMPT = "请深度分析该屏幕截图中展示的界面、内容或错误，并给出详细的解释和建议。"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun saveApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun getBaseUrl(context: Context): String {
        return getPrefs(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun saveBaseUrl(context: Context, url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        getPrefs(context).edit().putString(KEY_BASE_URL, cleanUrl).apply()
    }

    fun getDefaultModel(context: Context): String {
        return getPrefs(context).getString(KEY_DEFAULT_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun saveDefaultModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_DEFAULT_MODEL, model.trim()).apply()
    }

    fun getPresetPrompt(context: Context): String {
        return getPrefs(context).getString(KEY_PRESET_PROMPT, DEFAULT_PROMPT) ?: DEFAULT_PROMPT
    }

    fun savePresetPrompt(context: Context, prompt: String) {
        getPrefs(context).edit().putString(KEY_PRESET_PROMPT, prompt.trim()).apply()
    }
}
