package com.soar.sparkai.feature.ai.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * AAMS 模型配置数据实体类
 *
 * 包含模型名称、标识符以及针对特定模型的定制参数（如温度、存在/频率惩罚、停止词、深度思考等）。
 */
data class AamsModelConfig(
    val id: String,
    val name: String,
    val model: String,
    val temperature: Double = 1.0,
    val stop: List<String>? = null,
    val frequencyPenalty: Double = 0.0,
    val presencePenalty: Double = 0.0,
    val thinkingType: String = "disabled", // "disabled" 或 "enabled"
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val isSystem: Boolean = false
) {
    /**
     * 转换为 JSON 字符串用于持久化
     */
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("model", model)
        json.put("temperature", temperature)
        
        if (stop != null) {
            val arr = JSONArray()
            stop.forEach { arr.put(it) }
            json.put("stop", arr)
        } else {
            json.put("stop", JSONObject.NULL)
        }
        
        json.put("frequencyPenalty", frequencyPenalty)
        json.put("presencePenalty", presencePenalty)
        json.put("thinkingType", thinkingType)
        
        if (apiKey != null) json.put("apiKey", apiKey) else json.put("apiKey", JSONObject.NULL)
        if (baseUrl != null) json.put("baseUrl", baseUrl) else json.put("baseUrl", JSONObject.NULL)
        
        json.put("isSystem", isSystem)
        return json.toString()
    }

    companion object {
        /**
         * 从 JSON 还原 AamsModelConfig
         */
        fun fromJson(jsonStr: String): AamsModelConfig {
            val json = JSONObject(jsonStr)
            
            val stopList = if (json.isNull("stop")) {
                null
            } else {
                val arr = json.optJSONArray("stop")
                if (arr != null) {
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }
                    list
                } else {
                    null
                }
            }

            return AamsModelConfig(
                id = json.getString("id"),
                name = json.getString("name"),
                model = json.getString("model"),
                temperature = json.optDouble("temperature", 1.0),
                stop = stopList,
                frequencyPenalty = json.optDouble("frequencyPenalty", 0.0),
                presencePenalty = json.optDouble("presencePenalty", 0.0),
                thinkingType = json.optString("thinkingType", "disabled"),
                apiKey = if (json.isNull("apiKey")) null else json.optString("apiKey").ifEmpty { null },
                baseUrl = if (json.isNull("baseUrl")) null else json.optString("baseUrl").ifEmpty { null },
                isSystem = json.optBoolean("isSystem", false)
            )
        }
    }
}
