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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soar.sparkai.feature.ai.model.AamsModelConfig
import com.soar.sparkai.feature.ai.util.AamsModelConfigManager

/**
 * 大模型配置管理列表浮窗 Dialog
 *
 * 作用：展示所有可用的大模型高级参数配置，支持新增、编辑、删除等生命周期操作。
 */
@Composable
fun AamsModelConfigListDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var configsList by remember { mutableStateOf(emptyList<AamsModelConfig>()) }
    var selectedEditConfig by remember { mutableStateOf<AamsModelConfig?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun refreshList() {
        configsList = AamsModelConfigManager.getAllConfigs(context)
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("关闭", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ 模型配置管理",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // 新建模型按钮
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "新增配置",
                        tint = Color(0xFF00B894)
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                if (configsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无任何模型配置", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(configsList, key = { it.id }) { config ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF141421)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = config.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "模型: ${config.model} | Temp: ${config.temperature}",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp
                                        )
                                        if (config.isSystem) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = Color(0xFF8A2387).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "内置系统配置",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFFFF7675),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 编辑配置按钮
                                        IconButton(onClick = { selectedEditConfig = config }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "编辑",
                                                tint = Color(0xFFF1C40F),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // 删除配置按钮
                                        if (!config.isSystem) {
                                            IconButton(
                                                onClick = {
                                                    AamsModelConfigManager.deleteConfig(context, config.id)
                                                    refreshList()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "删除",
                                                    tint = Color(0xFFFF7675),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1E2F),
        shape = RoundedCornerShape(24.dp)
    )

    // 新增模型配置弹窗
    if (showCreateDialog) {
        AamsModelConfigEditDialog(
            config = null,
            onDismiss = { showCreateDialog = false },
            onSaveSuccess = {
                refreshList()
                showCreateDialog = false
            }
        )
    }

    // 编辑模型配置弹窗
    if (selectedEditConfig != null) {
        AamsModelConfigEditDialog(
            config = selectedEditConfig,
            onDismiss = { selectedEditConfig = null },
            onSaveSuccess = {
                refreshList()
                selectedEditConfig = null
            }
        )
    }
}
