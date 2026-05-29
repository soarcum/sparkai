package com.soar.sparkai.feature.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.soar.sparkai.core.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 手机无线麦克风音频流捕获与推送管理器
 */
object AudioStreamer {

    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS) // 实时音频流，允许无限长时间写入
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var isStreaming = false
    private var audioRecord: AudioRecord? = null

    /**
     * 开始捕获麦克风输入并流式推送到电脑端
     */
    @SuppressLint("MissingPermission")
    suspend fun startStreaming(
        ip: String,
        port: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (isStreaming) return@withContext
        isStreaming = true

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufSize == AudioRecord.ERROR || minBufSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("系统不支持所需的音频录制参数")
            isStreaming = false
            return@withContext
        }

        // 使用 2048 字节作为读取单位，保证低延迟吞吐
        val bufferSize = Math.max(minBufSize, 2048)
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("初始化录音硬件设备失败，请确认已授予麦克风权限")
                releaseAudio()
                return@withContext
            }

            audioRecord?.startRecording()
            onSuccess()
            AppLogger.i("AudioStreamer", "录音已启动，开始向电脑端建立流式连接...")
        } catch (e: Exception) {
            onError("启动录音发生异常: ${e.message}")
            releaseAudio()
            return@withContext
        }

        val requestBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                val tempBuffer = ByteArray(1024) // 每次读取 1KB，极其平滑低延迟
                try {
                    while (isStreaming && audioRecord != null) {
                        val readBytes = audioRecord?.read(tempBuffer, 0, tempBuffer.size) ?: 0
                        if (readBytes > 0) {
                            sink.write(tempBuffer, 0, readBytes)
                            sink.flush() // 立即推送，确保极致低延迟
                        } else if (readBytes < 0) {
                            AppLogger.e("AudioStreamer", "读取音频数据返回错误错误码: $readBytes")
                            break
                        }
                    }
                } catch (e: IOException) {
                    AppLogger.e("AudioStreamer", "音频网络写入发生异常，可能是网络断开: ${e.message}")
                }
            }
        }

        val url = "http://$ip:$port/audio/stream"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onError("电脑端返回连接错误: ${response.code}")
                } else {
                    AppLogger.i("AudioStreamer", "音频流传输成功结束")
                }
            }
        } catch (e: Exception) {
            if (isStreaming) {
                onError("音频传输连接中断: ${e.message}")
            }
        } finally {
            releaseAudio()
        }
    }

    /**
     * 停止实时音频流推送
     */
    fun stopStreaming() {
        if (!isStreaming) return
        isStreaming = false
        AppLogger.i("AudioStreamer", "主动请求停止音频投射")
        releaseAudio()
    }

    private fun releaseAudio() {
        isStreaming = false
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (e: Exception) {}
        try {
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null
    }
}
