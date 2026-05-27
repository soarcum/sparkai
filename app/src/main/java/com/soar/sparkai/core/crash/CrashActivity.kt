package com.soar.sparkai.core.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soar.sparkai.core.theme.AppTheme

class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_INFO = "extra_crash_info"
        const val EXTRA_THREAD_NAME = "extra_thread_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawCrashInfo = intent.getStringExtra(EXTRA_CRASH_INFO) ?: "暂无详细的崩溃信息记录"
        // 核心过滤算法：去粗取精，自动滤除底层冗长系统堆栈
        val crashInfo = filterStackTrace(rawCrashInfo)
        val threadName = intent.getStringExtra(EXTRA_THREAD_NAME) ?: "未知"

        val deviceInfo = """
            -- 诊断设备信息 --
            应用包名: $packageName
            安卓版本: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            硬件设备: ${Build.MANUFACTURER} ${Build.MODEL}
            异常线程: $threadName 线程
            ----------------
        """.trimIndent()

        val fullReport = "$deviceInfo\n\n$crashInfo"

        setContent {
            AppTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashScreen(
                        report = fullReport,
                        onCopy = { copyToClipboard(fullReport) },
                        onShare = { shareReport(fullReport) },
                        onRestart = { restartApp() }
                    )
                }
            }
        }
    }

    @Composable
    fun CrashScreen(
        report: String,
        onCopy: () -> Unit,
        onShare: () -> Unit,
        onRestart: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "应用程序不幸崩溃",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFEF5350)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "发生了一个预料之外的系统错误。请参阅下方的详细崩溃日志与诊断报告：",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(12.dp)
            ) {
                val scrollStateVertical = rememberScrollState()
                val scrollStateHorizontal = rememberScrollState()

                Text(
                    text = report,
                    color = Color(0xFFE5C07B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollStateVertical)
                        .horizontalScroll(scrollStateHorizontal)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "复制")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "复制日志", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "分享")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "分享报告", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "重启")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "重新启动", fontSize = 12.sp)
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("崩溃诊断日志", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "崩溃诊断日志已成功复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun shareReport(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "分享崩溃诊断日志"))
    }

    private fun restartApp() {
        val packageManager = packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        finish()
    }

    /**
     * 智能堆栈过滤器：自动剔除 Android VM/框架 底层反射及受精卵线程的冗长系统级日志，
     * 提炼并高亮呈现最宝贵的业务出错代码行与根本原因。
     */
    private fun filterStackTrace(rawInfo: String): String {
        val lines = rawInfo.lines()
        val filteredLines = mutableListOf<String>()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            // 保留异常名称、根本因由(Caused by)以及应用自身业务相关的每一行堆栈
            if (trimmed.startsWith("Caused by:") || 
                trimmed.contains("com.soar.sparkai") || 
                !trimmed.startsWith("at ")
            ) {
                filteredLines.add(line)
            } else {
                // 如果是连续的系统级冗长栈，我们只显示一个优雅的省略替代，提炼纯度
                if (filteredLines.isNotEmpty() && filteredLines.last().trim() != "... [已过滤掉系统级底层冗余堆栈]") {
                    filteredLines.add("    ... [已过滤掉系统级底层冗余堆栈]")
                }
            }
        }
        return filteredLines.joinToString("\n")
    }
}
