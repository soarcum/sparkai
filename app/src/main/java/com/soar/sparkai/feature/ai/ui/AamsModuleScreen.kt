package com.soar.sparkai.feature.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.soar.sparkai.feature.ai.model.AamsModule
import com.soar.sparkai.feature.ai.util.AamsModuleManager
import com.soar.sparkai.core.log.AppLogger
import org.json.JSONObject
import java.util.UUID

/**
 * 🧩 AAMS AI 自定义模块管理中心 Screen
 * 
 * 特色：赛博朋克霓虹暗黑美学设计，支持模块列表查看、极速开关控制、一键删除以及热贴粘贴 JSON 载入新模块。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AamsModuleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    // 状态列表
    var modulesList by remember { mutableStateOf(emptyList<AamsModule>()) }
    // 弹窗标记
    var showImportDialog by remember { mutableStateOf(false) }
    // 输入文本
    var jsonInputText by remember { mutableStateOf("") }
    
    // 新增状态：控制模型配置管理 Dialog 展示
    var showModelConfigListDialog by remember { mutableStateOf(false) }
    // 新增状态：当前要配置参数的模块
    var selectedEditModule by remember { mutableStateOf<AamsModule?>(null) }

    // 初始化加载
    LaunchedEffect(Unit) {
        modulesList = AamsModuleManager.getAllModules(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A)) // 延续暗夜霓虹底色
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // ================== 1. 顶部高端导航栏 ==================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "🧩 AI 自定义模块中心",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = { showModelConfigListDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "模型配置管理",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "通过与手机大模型聊天，您可以让大模型为您开发各式各样的屏幕分析模块（生成 JSON 指令）。在此处贴粘载入，即可一键在悬浮球中启用！",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ================== 2. 模块滚动卡片列表 ==================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(modulesList, key = { it.id }) { module ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (module.enabled) Color(0xFFF1C40F).copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E2F)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = module.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 精美 Badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (module.isSystem) Color(0xFF8A2387).copy(alpha = 0.3f) else Color(0xFF00B894).copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (module.isSystem) "⚡ 内置系统" else "🧩 玩家自定义",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (module.isSystem) Color(0xFFFF7675) else Color(0xFF55EFC4)
                                        )
                                    }
                                }
                                
                                // 模块启用的 Switch 开关
                                Switch(
                                    checked = module.enabled,
                                    onCheckedChange = { checked ->
                                        AamsModuleManager.toggleModuleEnabled(context, module.id, checked)
                                        modulesList = AamsModuleManager.getAllModules(context)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFE94057),
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = module.description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = Color.White.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable { selectedEditModule = module }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "配置参数",
                                        tint = Color(0xFFF1C40F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "配置模块参数",
                                        color = Color(0xFFF1C40F),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (!module.isSystem) {
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                AamsModuleManager.deleteModule(context, module.id)
                                                modulesList = AamsModuleManager.getAllModules(context)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "删除",
                                            tint = Color(0xFFFF7675),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "删除此模块",
                                            color = Color(0xFFFF7675),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================== 3. 霓虹渐变“装载新模块”按钮 ==================
            Button(
                onClick = {
                    jsonInputText = ""
                    showImportDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF8A2387), Color(0xFFE94057))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "导入",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🧩 装载新模块 (粘贴 JSON 脚本)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ================== 4. 精致毛玻璃粘贴装载 Dialog 弹窗 ==================
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Text(
                        text = "🧩 装载 AI 自定义模块",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "请粘贴大模型为您生成的 AAMS 模块 JSON 格式配置代码：",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        TextField(
                            value = jsonInputText,
                            onValueChange = { jsonInputText = it },
                            placeholder = {
                                Text(
                                    text = "粘贴包含 name、description 和 prompt 的 JSON 配置...",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color(0xFF141421),
                                cursorColor = Color(0xFFE94057),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (jsonInputText.trim().isEmpty()) {
                                android.widget.Toast.makeText(context, "粘贴内容为空，无法装载", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            try {
                                var rawText = jsonInputText.trim()
                                // 清洗 markdown 包裹
                                if (rawText.startsWith("```")) {
                                    rawText = rawText.substringAfter("```json").substringAfter("```").substringBeforeLast("```").trim()
                                }

                                val json = JSONObject(rawText)
                                val name = json.optString("name", "").trim()
                                val description = json.optString("description", "AI 智能自动分析模块").trim()
                                val prompt = json.optString("prompt", "").trim()

                                if (name.isEmpty() || prompt.isEmpty()) {
                                    android.widget.Toast.makeText(context, "装载失败：JSON 必须包含 name 和 prompt 核心字段", android.widget.Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                val id = if (json.has("id")) json.getString("id") else "module_" + UUID.randomUUID().toString().substring(0, 8)
                                val newModule = AamsModule(
                                    id = id,
                                    name = name,
                                    description = description,
                                    prompt = prompt,
                                    enabled = true,
                                    isSystem = false
                                )

                                AamsModuleManager.saveModule(context, newModule)
                                modulesList = AamsModuleManager.getAllModules(context)
                                showImportDialog = false
                                android.widget.Toast.makeText(context, "🎉 模块“$name”装载成功！已持久化并上线悬浮球！", android.widget.Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                AppLogger.e("AamsModuleScreen", "导入模块 JSON 解析崩溃: ${e.message}", e)
                                android.widget.Toast.makeText(context, "装载失败：JSON 语法错误！请确认复制的是完整 JSON 文本。", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                    ) {
                        Text("确认装载", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("取消", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF1E1E2F),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // ================== 5. 管理模型配置的弹窗 ==================
        if (showModelConfigListDialog) {
            AamsModelConfigListDialog(
                onDismiss = { showModelConfigListDialog = false }
            )
        }

        // ================== 6. 编辑特定模块的配置弹窗 ==================
        if (selectedEditModule != null) {
            AamsModuleEditDialog(
                module = selectedEditModule!!,
                onDismiss = { selectedEditModule = null },
                onSaveSuccess = {
                    modulesList = AamsModuleManager.getAllModules(context)
                }
            )
        }
    }
}
