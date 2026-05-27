package com.soar.sparkai.core.log

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 作用：实现多线程异步日志追加写入本地文件，具备空间自动滚存控制，防止磁盘爆满。
 */
class LogFileAppender private constructor(context: Context) {

    companion object {
        private const val LOG_FILE_NAME = "app_running.log"
        private const val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2MB 空间限制
        
        @Volatile
        private var instance: LogFileAppender? = null

        fun init(context: Context): LogFileAppender {
            return instance ?: synchronized(this) {
                instance ?: LogFileAppender(context.applicationContext).also { instance = it }
            }
        }

        fun getInstance(): LogFileAppender {
            return instance ?: throw IllegalStateException("LogFileAppender is not initialized. Call init() first.")
        }
    }

    private val logFile: File = File(context.cacheDir, LOG_FILE_NAME)
    private val writeExecutor = Executors.newSingleThreadExecutor()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 异步将格式化好的日志追加写入本地文件中
     */
    fun appendLog(level: String, tag: String, message: String, throwable: Throwable? = null) {
        writeExecutor.execute {
            try {
                checkAndRotateFile()
                val logLine = formatLog(level, tag, message, throwable)
                FileOutputStream(logFile, true).use { fos ->
                    PrintWriter(fos).use { writer ->
                        writer.println(logLine)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LogFileAppender", "Failed to write log to file: ${e.message}")
            }
        }
    }

    /**
     * 读取并获取本地全部的日志文件内容
     */
    fun readAllLogs(): String {
        return try {
            if (logFile.exists()) logFile.readText() else "暂无本地持久化日志记录。"
        } catch (e: Exception) {
            "读取本地日志文件失败: ${e.message}"
        }
    }

    /**
     * 清空本地日志文件
     */
    fun clearLogs() {
        writeExecutor.execute {
            try {
                if (logFile.exists()) {
                    logFile.delete()
                }
                logFile.createNewFile()
            } catch (e: Exception) {
                android.util.Log.e("LogFileAppender", "Failed to clear logs file: ${e.message}")
            }
        }
    }

    /**
     * 核心防爆盘策略：检测日志文件大小，超过 2MB 时清空重开，避免无休止消耗存储
     */
    private fun checkAndRotateFile() {
        if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
            logFile.delete()
            logFile.createNewFile()
            FileOutputStream(logFile, true).use { fos ->
                PrintWriter(fos).use { writer ->
                    writer.println("${timeFormat.format(Date())} [INFO] [LogFileAppender] 日志文件大小超过 2MB 上限限制。系统已自动清理，并开启全新日志记录。")
                }
            }
        } else if (!logFile.exists()) {
            logFile.createNewFile()
        }
    }

    /**
     * 格式化日志行为
     */
    private fun formatLog(level: String, tag: String, message: String, throwable: Throwable?): String {
        val timeStr = timeFormat.format(Date())
        val baseLog = "$timeStr [$level] [$tag] $message"
        if (throwable == null) return baseLog

        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        throwable.printStackTrace(pw)
        return "$baseLog\n${sw}"
    }
}
