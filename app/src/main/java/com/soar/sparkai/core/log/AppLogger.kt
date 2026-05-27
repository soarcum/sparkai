package com.soar.sparkai.core.log

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 作用：为应用提供统一的日志 API 路由，并将日志分发到 Logcat、内存流（用于 UI 实时渲染）与磁盘追加器。
 */
object AppLogger {

    data class LogEntry(
        val timestamp: String,
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // 内存实时日志流缓存，采用 MutableStateFlow，最大限制 200 条，免读盘直接驱动 UI 高帧率渲染
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (!isInitialized) {
                LogFileAppender.init(context)
                isInitialized = true
                i("AppLogger", "SparkAI detailed logging system initialized successfully.")
            }
        }
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log("VERBOSE", tag, message, throwable)
        android.util.Log.v(tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log("DEBUG", tag, message, throwable)
        android.util.Log.d(tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log("INFO", tag, message, throwable)
        android.util.Log.i(tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
        android.util.Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
        android.util.Log.e(tag, message, throwable)
    }

    /**
     * 清空内存与磁盘日志
     */
    fun clearAllLogs() {
        _logsFlow.value = emptyList()
        if (isInitialized) {
            LogFileAppender.getInstance().clearLogs()
        }
    }

    /**
     * 分发与持久化
     */
    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!isInitialized) return
        
        // 1. 追加到磁盘文件
        LogFileAppender.getInstance().appendLog(level, tag, message, throwable)

        // 2. 追加到内存状态流（线程安全）
        val entry = LogEntry(timeFormat.format(Date()), level, tag, message, throwable)
        synchronized(this) {
            val currentList = _logsFlow.value.toMutableList()
            if (currentList.size >= 200) {
                currentList.removeAt(0) // 淘汰老日志
            }
            currentList.add(entry)
            _logsFlow.value = currentList
        }
    }
}
