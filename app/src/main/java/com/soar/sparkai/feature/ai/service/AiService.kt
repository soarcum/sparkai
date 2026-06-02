package com.soar.sparkai.feature.ai.service

import android.graphics.Bitmap
import com.soar.sparkai.feature.ai.model.AiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 小米 MiMo 大模型流式网络交互服务
 *
 * 作用：基于 OkHttp 网络客户端发起 POST 请求，使用原生 JSONObject 构建
 * 兼容 Vision 多模态和普通文本的多轮对话 Payload，并通过 BuffereReader
 * 逐行破译 SSE (Server-Sent Events) 数据流，以 Kotlin Flow 优雅传回 UI。
 */
object AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 将 Bitmap 进行 Base64 PNG 无损压缩并编码
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    /**
     * 发送流式 SSE 对话请求
     */
    fun sendChatRequestStream(
        apiKey: String,
        baseUrl: String,
        model: String,
        promptText: String,
        bitmap: Bitmap?,
        history: List<AiMessage>
    ): Flow<String> = flow {
        // 构建规范的 completions 网关 URL
        val url = "${baseUrl.trim().removeSuffix("/")}/chat/completions"
        
        // 智能多模态自动升级
        val modelToUse = if (bitmap != null) "mimo-v2.5" else model

        val reqObj = JSONObject()
        reqObj.put("model", modelToUse)
        reqObj.put("stream", true)

        val messagesArr = JSONArray()

        // 1. 装载 System 角色描述
        val systemObj = JSONObject()
        systemObj.put("role", "system")
        systemObj.put("content", "You are MiMo, an AI assistant developed by Xiaomi. Today is date: Tuesday, December 16, 2025. Your knowledge cutoff date is December 2024. Please reply in Chinese.")
        messagesArr.put(systemObj)

        // 2. 装载多轮历史对话上下文 (不超过最近 12 轮，防止 Token 溢出)
        val recentHistory = history.takeLast(12)
        for (msg in recentHistory) {
            val histObj = JSONObject()
            histObj.put("role", msg.role)
            
            // 如果历史消息带 Bitmap，映射为多模态格式，否则直接作为 String
            if (msg.imageBitmap != null) {
                val histContentArr = JSONArray()
                
                val textPart = JSONObject()
                textPart.put("type", "text")
                textPart.put("text", msg.content)
                histContentArr.put(textPart)
                
                val imagePart = JSONObject()
                imagePart.put("type", "image_url")
                val imgUrlObj = JSONObject()
                imgUrlObj.put("url", "data:image/png;base64,${bitmapToBase64(msg.imageBitmap)}")
                imagePart.put("image_url", imgUrlObj)
                histContentArr.put(imagePart)
                
                histObj.put("content", histContentArr)
            } else {
                histObj.put("content", msg.content)
            }
            messagesArr.put(histObj)
        }

        // 3. 装载当前轮的最新消息
        val userObj = JSONObject()
        userObj.put("role", "user")

        if (bitmap != null) {
            val currentContentArr = JSONArray()
            
            val textPart = JSONObject()
            textPart.put("type", "text")
            textPart.put("text", promptText)
            currentContentArr.put(textPart)
            
            val imagePart = JSONObject()
            imagePart.put("type", "image_url")
            val imgUrlObj = JSONObject()
            imgUrlObj.put("url", "data:image/png;base64,${bitmapToBase64(bitmap)}")
            imagePart.put("image_url", imgUrlObj)
            currentContentArr.put(imagePart)
            
            userObj.put("content", currentContentArr)
        } else {
            userObj.put("content", promptText)
        }
        messagesArr.put(userObj)

        reqObj.put("messages", messagesArr)

        // 4. 发起 HTTP 字节流直连
        val mediaType = "application/json".toMediaType()
        val requestBody = reqObj.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: response.message
            throw Exception("API 联调失败 (HTTP ${response.code}): $errBody")
        }

        val body = response.body ?: throw Exception("API 返回响应体为空")
        val reader = BufferedReader(InputStreamReader(body.byteStream(), "UTF-8"))

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue
                
                // 解析 Server-Sent Events
                if (trimmed.startsWith("data:")) {
                    val dataStr = trimmed.substring(5).trim()
                    if (dataStr == "[DONE]") {
                        break
                    }
                    
                    try {
                        val parsed = JSONObject(dataStr)
                        val choices = parsed.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val deltaObj = choices.getJSONObject(0).optJSONObject("delta")
                            val content = deltaObj?.optString("content") ?: ""
                            if (content.isNotEmpty()) {
                                emit(content)
                            }
                        }
                    } catch (e: Exception) {
                        // 忽略半包或数据行断裂解析异常，等待下个数据块读取
                    }
                }
            }
        } finally {
            try {
                reader.close()
            } catch (e: Exception) {
                // 忽略关闭流异常
            }
            body.close()
        }
    }.flowOn(Dispatchers.IO) // 强制运行在 IO 挂起线程，保障 Android App UI 的流畅顺滑
}
