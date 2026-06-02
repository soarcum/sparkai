package com.soar.sparkai.feature.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soar.sparkai.feature.ai.util.AiConfigManager

/**
 * 高颜值的 AI 大模型参数配置浮板对话框
 *
 * 作用：展现精细的表单输入项以修改和应用 API Key、Base URL 等配置，
 * 采用磁贴卡片风格选择模型以提供完美的视觉体验。
 */
@Composable
fun AiConfigDialog(
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current

    var apiKey by remember { mutableStateOf(AiConfigManager.getApiKey(context)) }
    var baseUrl by remember { mutableStateOf(AiConfigManager.getBaseUrl(context)) }
    var defaultModel by remember { mutableStateOf(AiConfigManager.getDefaultModel(context)) }
    var presetPrompt by remember { mutableStateOf(AiConfigManager.getPresetPrompt(context)) }

    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    AiConfigManager.saveApiKey(context, apiKey)
                    AiConfigManager.saveBaseUrl(context, baseUrl)
                    AiConfigManager.saveDefaultModel(context, defaultModel)
                    AiConfigManager.savePresetPrompt(context, presetPrompt)
                    onSaveSuccess()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94057)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存配置", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        },
        title = {
            Text(
                "🤖 小米大模型参数配置",
                fontSize = 18.sp,
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
                // 1. API Key 输入项
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "API 密钥 (api-key)", 
                        fontSize = 11.sp, 
                        color = Color.White.copy(alpha = 0.5f), 
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "查看密钥",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. Base URL 输入项
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "接口基础路径 (Base URL)", 
                        fontSize = 11.sp, 
                        color = Color.White.copy(alpha = 0.5f), 
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
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

                // 3. 磁贴卡片风格模型切换
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "默认选择模型", 
                        fontSize = 11.sp, 
                        color = Color.White.copy(alpha = 0.5f), 
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { defaultModel = "mimo-v2.5-pro" },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (defaultModel == "mimo-v2.5-pro") Color(0xFFE94057).copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.15f)
                            ),
                            border = if (defaultModel == "mimo-v2.5-pro") BorderStroke(1.2.dp, Color(0xFFE94057)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp), 
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("mimo-v2.5-pro", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Pro级纯文本", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { defaultModel = "mimo-v2.5" },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (defaultModel == "mimo-v2.5") Color(0xFFE94057).copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.15f)
                            ),
                            border = if (defaultModel == "mimo-v2.5") BorderStroke(1.2.dp, Color(0xFFE94057)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp), 
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("mimo-v2.5", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("多模态识图", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                            }
                        }
                    }
                }

                // 4. 截图 Preset Prompt
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "截图分析预设 Prompt", 
                        fontSize = 11.sp, 
                        color = Color.White.copy(alpha = 0.5f), 
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = presetPrompt,
                        onValueChange = { presetPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
                        maxLines = 3,
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
