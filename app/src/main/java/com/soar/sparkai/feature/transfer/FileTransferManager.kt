package com.soar.sparkai.feature.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.soar.sparkai.core.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 局域网传输核心逻辑管理器
 */
object FileTransferManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // SSE 长连接不需要读取超时
        .build()

    private var sseCall: okhttp3.Call? = null

    /**
     * 建立与电脑端的长连接监听 (SSE)
     */
    suspend fun startSSEConnection(
        ip: String,
        port: Int,
        onOfferReceived: (id: String, name: String, size: Long) -> Unit,
        onTextOfferReceived: (id: String, text: String, isUrl: Boolean) -> Unit,
        onStatusChanged: (connected: Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = "http://$ip:$port/events"
        val request = Request.Builder().url(url).build()
        sseCall = client.newCall(request)

        try {
            sseCall?.execute()?.use { response ->
                if (!response.isSuccessful) {
                    onStatusChanged(false)
                    return@use
                }
                onStatusChanged(true)
                val reader = response.body?.charStream()?.buffered() ?: return@use
                var line: String?
                var currentEvent = ""

                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.startsWith("event:")) {
                        currentEvent = trimmed.substring(6).trim()
                    } else if (trimmed.startsWith("data:")) {
                        val data = trimmed.substring(5).trim()
                        if (currentEvent == "file-offer") {
                            val json = JSONObject(data)
                            onOfferReceived(
                                json.getString("id"),
                                json.getString("filename"),
                                json.getLong("size")
                            )
                        } else if (currentEvent == "text-offer") {
                            val json = JSONObject(data)
                            onTextOfferReceived(
                                json.getString("id"),
                                json.getString("text"),
                                json.getBoolean("isUrl")
                            )
                        }
                        currentEvent = ""
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("FileTransferManager", "SSE 长连接发生异常被动断开: ${e.message}")
        } finally {
            onStatusChanged(false)
        }
    }

    /**
     * 上传手机文件到电脑端
     */
    suspend fun uploadFile(
        context: Context,
        ip: String,
        port: Int,
        fileUri: Uri,
        onProgress: (progress: Float) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var filename = "unknown_file"
        var size = 0L

        resolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                filename = cursor.getString(nameIndex)
                size = cursor.getLong(sizeIndex)
            }
        }

        try {
            val inputStream = resolver.openInputStream(fileUri) ?: throw Exception("无法读取文件流")
            val progressBody = ProgressRequestBody(inputStream, size) { written, total ->
                onProgress((written.toFloat() / total.toFloat()) * 100f)
            }

            val uploadUrl = "http://$ip:$port/upload?filename=${Uri.encode(filename)}&size=$size"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(progressBody)
                .build()

            OkHttpClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("服务器返回错误: ${response.code}")
                }
            }
        } catch (e: Exception) {
            onError(e.message ?: "未知上传错误")
        }
    }

    /**
     * 上传手机文本/链接到电脑端
     */
    suspend fun uploadText(
        ip: String,
        port: Int,
        text: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("text", text)
            }
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = json.toString().toRequestBody(mediaType)
            val uploadUrl = "http://$ip:$port/share/text"
            
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("服务器返回错误: ${response.code}")
                }
            }
        } catch (e: Exception) {
            onError(e.message ?: "未知文本发送错误")
        }
    }

    /**
     * 从电脑端下载文件并保存至 Downloads/SparkAI
     */
    suspend fun downloadFile(
        context: Context,
        ip: String,
        port: Int,
        fileId: String,
        filename: String,
        totalSize: Long,
        onProgress: (progress: Float) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val downloadUrl = "http://$ip:$port/download?id=$fileId"
        val request = Request.Builder().url(downloadUrl).build()

        try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("下载请求失败: ${response.code}")
                val bodyStream = response.body?.byteStream() ?: throw Exception("空的数据响应体")
                
                val outputStream = createDownloadOutputStream(context, filename)
                outputStream.use { output ->
                    val buffer = ByteArray(8192)
                    var readBytes = 0L
                    var bytes: Int
                    while (bodyStream.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        readBytes += bytes
                        onProgress((readBytes.toFloat() / totalSize.toFloat()) * 100f)
                    }
                }
                onSuccess()
            }
        } catch (e: Exception) {
            onError(e.message ?: "未知下载错误")
        }
    }

    // 高兼容性创建本地 Download 目录写入流
    private fun createDownloadOutputStream(context: Context, filename: String): OutputStream {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SparkAI")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("创建 MediaStore 记录失败")
            context.contentResolver.openOutputStream(uri) ?: throw Exception("打开媒体输出流失败")
        } else {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val sparkAiDir = File(downloadDir, "SparkAI")
            if (!sparkAiDir.exists()) sparkAiDir.mkdirs()
            val destFile = File(sparkAiDir, filename)
            FileOutputStream(destFile)
        }
    }

    fun stopSSEConnection() {
        sseCall?.cancel()
        sseCall = null
    }
}
