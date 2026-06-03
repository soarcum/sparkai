package com.soar.sparkai.feature.ai.util

import android.content.Context
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.feature.ai.model.AamsModelConfig
import org.json.JSONArray

/**
 * AAMS 模型配置管理器
 *
 * 作用：持久化管理大模型参数配置列表，支持新增、修改和删除。
 */
object AamsModelConfigManager {

    private const val PREFS_NAME = "aams_model_config_prefs"
    private const val KEY_CONFIGS = "aams_model_configs_json"

    // 系统内置的默认模型配置
    private val systemConfigs = listOf(
        AamsModelConfig(
            id = "sys_mimo_2.5_pro",
            name = "🤖 小米 MiMo Pro (mimo-v2.5-pro)",
            model = "mimo-v2.5-pro",
            temperature = 1.0,
            stop = null,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0,
            thinkingType = "disabled",
            isSystem = true
        ),
        AamsModelConfig(
            id = "sys_mimo_2.5",
            name = "👁️ 小米 MiMo Vision (mimo-v2.5)",
            model = "mimo-v2.5",
            temperature = 1.0,
            stop = null,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0,
            thinkingType = "disabled",
            isSystem = true
        )
    )

    /**
     * 获取所有配置（合并系统内置配置与用户自定义配置）
     */
    fun getAllConfigs(context: Context): List<AamsModelConfig> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CONFIGS, null)

        if (jsonStr.isNullOrEmpty()) {
            saveConfigsToPrefs(context, systemConfigs)
            return systemConfigs
        }

        return try {
            val list = mutableListOf<AamsModelConfig>()
            val arr = JSONArray(jsonStr)

            for (i in 0 until arr.length()) {
                list.add(AamsModelConfig.fromJson(arr.getString(i)))
            }

            // 双重确认：如果系统内置配置在持久化中丢失，自动补齐
            for (sys in systemConfigs) {
                if (list.none { it.id == sys.id }) {
                    list.add(sys)
                }
            }
            list
        } catch (e: Exception) {
            AppLogger.e("AamsModelConfigManager", "解析模型配置失败: ${e.message}", e)
            systemConfigs
        }
    }

    /**
     * 根据 ID 获取模型配置，如果不存在则返回默认的 Pro 配置
     */
    fun getConfigOrDefault(context: Context, id: String?): AamsModelConfig {
        val configs = getAllConfigs(context)
        return configs.find { it.id == id } ?: systemConfigs.first()
    }

    /**
     * 保存单个配置（新建或更新）
     */
    fun saveConfig(context: Context, config: AamsModelConfig) {
        val currentList = getAllConfigs(context).toMutableList()
        val index = currentList.indexOfFirst { it.id == config.id }

        if (index >= 0) {
            currentList[index] = config
        } else {
            currentList.add(config)
        }

        saveConfigsToPrefs(context, currentList)
        AppLogger.i("AamsModelConfigManager", "模型配置已成功保存: ${config.name} (ID: ${config.id})")
    }

    /**
     * 删除指定配置（系统内置配置不允许删除）
     */
    fun deleteConfig(context: Context, id: String): Boolean {
        val currentList = getAllConfigs(context).toMutableList()
        val config = currentList.find { it.id == id }

        if (config != null && !config.isSystem) {
            currentList.remove(config)
            saveConfigsToPrefs(context, currentList)
            AppLogger.i("AamsModelConfigManager", "已成功删除模型配置: ${config.name} (ID: $id)")
            return true
        }

        AppLogger.w("AamsModelConfigManager", "无法删除配置: 不存在或为系统内置配置 (ID: $id)")
        return false
    }

    /**
     * 私有方法：序列化并保存到 SharedPreferences
     */
    private fun saveConfigsToPrefs(context: Context, list: List<AamsModelConfig>) {
        try {
            val arr = JSONArray()
            for (c in list) {
                arr.put(c.toJson())
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CONFIGS, arr.toString())
                .apply()
        } catch (e: Exception) {
            AppLogger.e("AamsModelConfigManager", "序列化保存模型配置异常: ${e.message}", e)
        }
    }
}
