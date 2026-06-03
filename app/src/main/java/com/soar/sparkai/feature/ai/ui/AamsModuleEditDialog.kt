package com.soar.sparkai.feature.ai.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soar.sparkai.feature.ai.model.AamsModule
import com.soar.sparkai.feature.ai.util.AamsModelConfigManager
import com.soar.sparkai.feature.ai.util.AamsModuleManager

/**
 * AI 模块参数配置编辑器对话框
 *
 * 作用：支持对指定模块的 Prompt 提示词、名称、描述以及所绑定的模型配置进行微调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AamsModuleEditDialog(
    module: AamsModule,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(module.name) }
    var description by remember { mutableStateOf(module.description) }
    var prompt by remember { mutableStateOf(module.prompt) }
    var modelConfigId by remember { mutableStateOf(module.modelConfigId ?: "sys_mimo_2.5") }

    // 获取当前所有可用的模型配置
    val modelConfigs = remember { AamsModelConfigManager.getAllConfigs(context) }
    val currentConfig = modelConfigs.find { it.id == modelConfigId } ?: modelConfigs.firstOrNull()
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty() || prompt.trim().isEmpty()) {
                        android.widget.Toast.makeText(context, "名称与提示词不能为空", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val updatedModule = module.copy(
                        name = name.trim(),
                        description = description.trim(),
                        prompt = prompt.trim(),
                        modelConfigId = modelConfigId
                    )
                    AamsModuleManager.saveModule(context, updatedModule)
                    onSaveSuccess()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94057)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存设置", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        },
        title = {
            Text(
                text = "🔧 配置模块参数",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 模块名称
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "模块名称" + if (module.isSystem) " (系统内置，只读)" else "",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (!module.isSystem) name = it },
                        enabled = !module.isSystem,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                            disabledTextColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. 模块描述
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "模块描述" + if (module.isSystem) " (系统内置，只读)" else "",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (!module.isSystem) description = it },
                        enabled = !module.isSystem,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            disabledContainerColor = Color.White.copy(alpha = 0.05f),
                            disabledTextColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 3. 选择模型配置 (下拉菜单)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "绑定大模型配置",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentConfig?.name ?: "选择大模型配置",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "展开",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            modelConfigs.forEach { config ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(config.name, fontSize = 13.sp) 
                                    },
                                    onClick = {
                                        modelConfigId = config.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. 模块提示词 Prompt
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "提示词 (Prompt)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        containerColor = Color(0xFF1E1E2F),
        shape = RoundedCornerShape(24.dp)
    )
}
