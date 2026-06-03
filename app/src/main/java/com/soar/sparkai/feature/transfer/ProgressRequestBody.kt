package com.soar.sparkai.feature.transfer

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

/**
 * 支持上传进度回调的流式 RequestBody，防止大文件 OOM 并实现精准进度统计
 */
class ProgressRequestBody(
    private val inputStream: InputStream,
    private val contentLength: Long,
    private val contentTypeStr: String = "application/octet-stream",
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? {
        return contentTypeStr.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return contentLength
    }

    override fun isOneShot(): Boolean {
        return true
    }

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(8192)
        var bytesWritten = 0L
        inputStream.use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                bytesWritten += read
                onProgress(bytesWritten, contentLength)
            }
        }
    }
}
