package com.soar.sparkai.feature.ai.model

import org.json.JSONObject

/**
 * AAMS (AI-Assisted Module System) 自定义模块数据实体类
 *
 * 作用：定义自定义 AI 屏幕分析脚本的元信息，
 * 支持模块的基本描述、prompt 配置、状态控制以及原生 JSON 序列化传输。
 */
data class AamsModule(
    val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val enabled: Boolean = true,
    val isSystem: Boolean = false,
    val modelConfigId: String? = null
) {
    /**
     * 将模块对象序列化为 JSON 字符串，便于本地持久化持久保存
     */
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("description", description)
        json.put("prompt", prompt)
        json.put("enabled", enabled)
        json.put("isSystem", isSystem)
        if (modelConfigId != null) {
            json.put("modelConfigId", modelConfigId)
        } else {
            json.put("modelConfigId", JSONObject.NULL)
        }
        return json.toString()
    }

    companion object {
        /**
         * 从 JSON 字符串中安全还原出 AamsModule 对象
         */
        fun fromJson(jsonStr: String): AamsModule {
            val json = JSONObject(jsonStr)
            return AamsModule(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.getString("description"),
                prompt = json.getString("prompt"),
                enabled = json.optBoolean("enabled", true),
                isSystem = json.optBoolean("isSystem", false),
                modelConfigId = if (json.isNull("modelConfigId")) null else json.optString("modelConfigId").ifEmpty { null }
            )
        }
    }
}
