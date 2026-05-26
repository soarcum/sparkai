package com.soar.sparkai.feature.home.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.soar.sparkai.core.update.UpdateDialog
import com.soar.sparkai.core.update.UpdateManager
import com.soar.sparkai.feature.floatwindow.service.FloatingService

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 悬浮服务实际运行状态
    var isServiceActive by remember { mutableStateOf(FloatingService.isServiceRunning) }
    // 悬浮窗权限说明弹窗控制
    var showPermissionDialog by remember { mutableStateOf(false) }

    // 监听应用前后台切换，从系统设置返回时能够立即感知并刷新状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceActive = FloatingService.isServiceRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 当 HomeScreen 初次进入组合树生命周期时，自动触发静默更新检查
    LaunchedEffect(Unit) {
        UpdateManager.checkUpdate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A)) // 炫酷暗夜底色
    ) {
        // 主页面的主体内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SparkAI",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Your agentic power assistant",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ================== 高颜值屏幕助手配置卡片 ==================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2F) // 深空灰色调
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 带有渐变效果的高端闪电图标
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF8A2387), Color(0xFFE94057))
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = "助手",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "常驻屏幕助手",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // 动态渲染状态指示器（带色彩渐变的 Badge）
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isServiceActive) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = "状态",
                                    tint = if (isServiceActive) Color(0xFF00B894) else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isServiceActive) "运行中" else "未开启",
                                    fontSize = 12.sp,
                                    color = if (isServiceActive) Color(0xFF00B894) else Color.White.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 极简现代的 Switch 控制器
                        Switch(
                            checked = isServiceActive,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // 检查系统悬浮窗权限
                                    if (Settings.canDrawOverlays(context)) {
                                        startFloatingService(context)
                                        isServiceActive = true
                                    } else {
                                        // 无权限则展示高颜值引导弹窗
                                        showPermissionDialog = true
                                    }
                                } else {
                                    stopFloatingService(context)
                                    isServiceActive = false
                                }
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "开启后屏幕边缘将浮现 SparkAI 圆形控制球，支持在任意其他应用上方进行“系统截屏”、“快速呼回主界面”等操作，让智能协同如影随形。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 挂载磨砂玻璃质感的自更新交互弹窗
        UpdateDialog()

        // ================== 悬浮窗权限授权引导弹窗 ==================
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "权限",
                        tint = Color(0xFFE94057),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "需要悬浮窗权限",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "为了能够在其他应用上层显示常驻悬浮按钮及快捷工具，我们需要“显示在其他应用上层”（悬浮窗）权限。请在接下来的设置页中找到 SparkAI 并开启授权。",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            // 跳转至系统悬浮窗权限配置界面
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE94057)
                        )
                    ) {
                        Text("去授权", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPermissionDialog = false }
                    ) {
                        Text("取消", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF1E1E2F),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

/**
 * 启动悬浮窗前台服务
 */
private fun startFloatingService(context: android.content.Context) {
    val intent = Intent(context, FloatingService::class.java).apply {
        action = FloatingService.ACTION_START
    }
    context.startService(intent)
}

/**
 * 停止悬浮窗前台服务
 */
private fun stopFloatingService(context: android.content.Context) {
    val intent = Intent(context, FloatingService::class.java).apply {
        action = FloatingService.ACTION_STOP
    }
    context.startService(intent)
}


