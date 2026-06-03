package com.soar.sparkai.feature.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.util.UUID

/**
 * 大模型高级配置参数编辑器浮窗 Dialog
 *
 * 作用：支持对模型名称、接口地址、密钥、温度、深度思考、停止词等数十个高级维度属性的精确自定义。
 */
@Composable
fun AamsModelConfigEditDialog(
    config: AamsModelConfig?,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = config != null

    var name by remember { mutableStateOf(config?.name ?: "") }
    var model by remember { mutableStateOf(config?.model ?: "mimo-v2.5-pro") }
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(config?.baseUrl ?: "") }
    var temperatureStr by remember { mutableStateOf(config?.temperature?.toString() ?: "1.0") }
    var frequencyPenaltyStr by remember { mutableStateOf(config?.frequencyPenalty?.toString() ?: "0.0") }
    var presencePenaltyStr by remember { mutableStateOf(config?.presencePenalty?.toString() ?: "0.0") }
    var stopStr by remember { mutableStateOf(config?.stop?.joinToString(",") ?: "") }
    var thinkingType by remember { mutableStateOf(config?.thinkingType ?: "disabled") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty() || model.trim().isEmpty()) {
                        android.widget.Toast.makeText(context, "配置名称与模型标识不能为空", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val tempVal = temperatureStr.toDoubleOrNull() ?: 1.0
                    val freqVal = frequencyPenaltyStr.toDoubleOrNull() ?: 0.0
                    val presVal = presencePenaltyStr.toDoubleOrNull() ?: 0.0
                    
                    val stopList = if (stopStr.trim().isEmpty()) {
                        null
                    } else {
                        stopStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }

                    val newConfig = AamsModelConfig(
                        id = config?.id ?: ("config_" + UUID.randomUUID().toString().substring(0, 8)),
                        name = name.trim(),
                        model = model.trim(),
                        temperature = tempVal,
                        stop = stopList,
                        frequencyPenalty = freqVal,
                        presencePenalty = presVal,
                        thinkingType = thinkingType,
                        apiKey = apiKey.trim().ifEmpty { null },
                        baseUrl = baseUrl.trim().ifEmpty { null },
                        isSystem = config?.isSystem ?: false
                    )

                    AamsModelConfigManager.saveConfig(context, newConfig)
                    onSaveSuccess()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94057)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存参数", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        },
        title = {
            Text(
                text = if (isEditing) "🔧 编辑模型配置" else "➕ 新建模型配置",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 配置名称
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "配置名称" + if (config?.isSystem == true) " (内置系统，只读)" else "",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (config?.isSystem != true) name = it },
                        enabled = config?.isSystem != true,
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

                // 2. 模型 ID (支持快捷选择)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "模型标识 (model)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 快速磁贴切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { model = "mimo-v2.5-pro" },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (model == "mimo-v2.5-pro") Color(0xFFE94057).copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.15f)
                            ),
                            border = if (model == "mimo-v2.5-pro") BorderStroke(1.dp, Color(0xFFE94057)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                                Text("mimo-v2.5-pro", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { model = "mimo-v2.5" },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (model == "mimo-v2.5") Color(0xFFE94057).copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.15f)
                            ),
                            border = if (model == "mimo-v2.5") BorderStroke(1.dp, Color(0xFFE94057)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                                Text("mimo-v2.5", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. 深度思考
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "深度思考 (thinking)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { thinkingType = "disabled" }
                        ) {
                            RadioButton(
                                selected = thinkingType == "disabled",
                                onClick = { thinkingType = "disabled" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE94057))
                            )
                            Text("禁用", color = Color.White, fontSize = 12.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { thinkingType = "enabled" }
                        ) {
                            RadioButton(
                                selected = thinkingType == "enabled",
                                onClick = { thinkingType = "enabled" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE94057))
                            )
                            Text("启用", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // 4. 温度
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "采样温度 (temperature, 0.0 ~ 2.0)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = temperatureStr,
                        onValueChange = { temperatureStr = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 5. 存在惩罚 & 频率惩罚
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "存在惩罚 (presence)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = presencePenaltyStr,
                            onValueChange = { presencePenaltyStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE94057),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "频率惩罚 (frequency)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = frequencyPenaltyStr,
                            onValueChange = { frequencyPenaltyStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
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

                // 6. 停止词
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "停止词 (stop, 英文逗号分隔)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = stopStr,
                        onValueChange = { stopStr = it },
                        placeholder = { Text("例如: [DONE],null", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 7. 自定义 API 密钥
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "独立 API 密钥 (可选，留空则继承全局)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = { Text("继承全局 api-key 配置", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 8. 自定义 Base URL
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "独立 Base URL (可选，留空则继承全局)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        placeholder = { Text("继承全局 base-url 配置", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
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
