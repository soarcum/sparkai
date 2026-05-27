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
                i("AppLogger", "SparkAI 详细运行日志及诊断系统已成功初始化。")
                
                // 异步回填本地历史日志文件，确保打开日志控制台时记录不丢失
                kotlin.concurrent.thread(start = true) {
                    restoreLogsFromFile()
                }
            }
        }
    }

    /**
     * 核心算法：读取本地磁盘日志文件并进行高宽容度正则反序列化，填回内存 Flow 缓存中，并完美归纳跨行崩溃堆栈
     */
    private fun restoreLogsFromFile() {
        try {
            val rawLogs = LogFileAppender.getInstance().readAllLogs()
            if (rawLogs.isBlank() || rawLogs.startsWith("No logs") || rawLogs.startsWith("Failed to") || rawLogs.startsWith("暂无")) return

            val lines = rawLogs.lines()
            val parsedEntries = mutableListOf<LogEntry>()
            // 匹配格式: 2026-05-27 10:49:56.123 [INFO] [MainActivity] 消息内容
            val regex = """^(\d{4}-\d{2}-\d{2} )?(\d{2}:\d{2}:\d{2}\.\d{3}) \[([A-Z]+)\] \[([^\]]+)\] (.*)$""".toRegex()

            for (line in lines) {
                if (line.isBlank()) continue
                val matchResult = regex.matchEntire(line)
                if (matchResult != null) {
                    val timestamp = matchResult.groupValues[2]
                    val level = matchResult.groupValues[3]
                    val tag = matchResult.groupValues[4]
                    val message = matchResult.groupValues[5]
                    parsedEntries.add(LogEntry(timestamp, level, tag, message))
                } else {
                    // 对于换行的异常栈信息，自动将其合并归纳到上一条主日志的消息体中
                    if (parsedEntries.isNotEmpty()) {
                        val last = parsedEntries.last()
                        parsedEntries[parsedEntries.size - 1] = last.copy(message = last.message + "\n" + line)
                    }
                }
            }

            // 仅在内存保留最新的 200 条
            val subList = if (parsedEntries.size > 200) {
                parsedEntries.subList(parsedEntries.size - 200, parsedEntries.size)
            } else {
                parsedEntries
            }

            _logsFlow.value = subList
            i("AppLogger", "已从本地日志文件成功回显并恢复了 ${subList.size} 条历史交互记录。")
        } catch (e: Exception) {
            android.util.Log.e("AppLogger", "Failed to restore logs from local disk: ${e.message}")
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
