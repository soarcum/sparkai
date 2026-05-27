package com.soar.sparkai.core.log

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 作用：为应用提供炫酷极客风格、色彩搭配极其和谐的日志控制台页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDisplayScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val rawLogs by AppLogger.logsFlow.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("ALL") }

    // 过滤筛选日志，同时支持模糊搜索与级别分类
    val filteredLogs = remember(rawLogs, searchQuery, selectedLevel) {
        rawLogs.filter { log ->
            val matchLevel = selectedLevel == "ALL" || log.level == selectedLevel
            val matchQuery = searchQuery.isEmpty() || 
                    log.tag.contains(searchQuery, true) || 
                    log.message.contains(searchQuery, true)
            matchLevel && matchQuery
        }.reversed() // 倒序排列，最新日志在最上方呈现
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SparkAI 运行控制台", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E2F))
            )
        },
        bottomBar = {
            LogControlBar(
                onClear = {
                    AppLogger.clearAllLogs()
                    Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show()
                },
                onCopy = {
                    copyToClipboard(context, rawLogs)
                }
            )
        },
        containerColor = Color(0xFF0F0F1A) // 极客深黑背景
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 搜索输入框与级别过滤器
            SearchBarAndFilters(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                level = selectedLevel,
                onLevelChange = { selectedLevel = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 日志流列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("暂无匹配日志", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(filteredLogs) { log ->
                        LogCardItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarAndFilters(query: String, onQueryChange: (String) -> Unit, level: String, onLevelChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("搜索 Tag 或关键字...", color = Color.White.copy(0.4f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, "搜索", tint = Color.White.copy(0.6f)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E1E2F),
                unfocusedContainerColor = Color(0xFF1E1E2F),
                focusedBorderColor = Color(0xFFE94057),
                unfocusedBorderColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "DEBUG", "INFO", "WARN", "ERROR").forEach { lvl ->
                val selected = lvl == level
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) Color(0xFFE94057) else Color(0xFF1E1E2F))
                        .clickable { onLevelChange(lvl) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lvl, color = if (selected) Color.White else Color.White.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LogCardItem(log: AppLogger.LogEntry) {
    val levelColor = when (log.level) {
        "ERROR" -> Color(0xFFD63031)
        "WARN" -> Color(0xFFF27121)
        "INFO" -> Color(0xFF00B894)
        "DEBUG" -> Color(0xFF0984E3)
        else -> Color.Gray
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(log.timestamp, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(0.5f))
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    Modifier.background(levelColor.copy(0.15f), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(log.level, color = levelColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(log.tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.8f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(log.message, fontSize = 12.sp, color = Color.White.copy(0.9f), lineHeight = 16.sp)
        }
    }
}

@Composable
fun LogControlBar(onClear: () -> Unit, onCopy: () -> Unit) {
    Surface(
        color = Color(0xFF1E1E2F),
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031).copy(0.15f), contentColor = Color(0xFFFF7675)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.ClearAll, "清空")
                Spacer(Modifier.width(6.dp))
                Text("一键清空", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894).copy(0.15f), contentColor = Color(0xFF55EFC4)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.ContentCopy, "复制")
                Spacer(Modifier.width(6.dp))
                Text("复制全部", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun copyToClipboard(context: Context, logs: List<AppLogger.LogEntry>) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val logString = logs.joinToString("\n") { "${it.timestamp} [${it.level}] [${it.tag}] ${it.message}" }
    val clip = android.content.ClipData.newPlainText("SparkAI Log", logString)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "日志已全部复制到剪贴板", Toast.LENGTH_SHORT).show()
}
