package com.soar.sparkai.feature.ai.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * AI 对话消息气泡实体类
 *
 * 作用：支持表示用户与助理的角色、消息文本内容、图片 Uri、多模态 Bitmap、
 * 是否发生异常以及是否正处于 SSE 流式打字接收中。
 */
data class AiMessage(
    val role: String, // "user" | "assistant"
    val content: String,
    val imageUri: Uri? = null,
    val imageBitmap: Bitmap? = null,
    val isError: Boolean = false,
    val isStreaming: Boolean = false
)
