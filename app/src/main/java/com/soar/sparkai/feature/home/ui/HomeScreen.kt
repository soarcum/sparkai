package com.soar.sparkai.feature.home.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.feature.floatwindow.service.FloatingService
import com.soar.sparkai.feature.transfer.ui.FileTransferScreen
import com.soar.sparkai.feature.ai.ui.AiChatScreen
import com.soar.sparkai.feature.ai.ui.AamsModuleScreen
import com.soar.sparkai.feature.accessibility.util.AccessibilityUtils
import android.widget.Toast

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentScreen by remember { mutableStateOf("home") }

    // 悬浮服务实际运行状态
    var isServiceActive by remember { mutableStateOf(FloatingService.isServiceRunning) }
    // 无障碍服务实际开启状态
    var isAccessibilityActive by remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) }
    // 悬浮窗权限说明弹窗控制
    var showPermissionDialog by remember { mutableStateOf(false) }

    if (currentScreen == "transfer") {
        FileTransferScreen(onBack = { currentScreen = "home" })
        return
    } else if (currentScreen == "ai") {
        AiChatScreen(onBack = { currentScreen = "home" })
        return
    } else if (currentScreen == "aams") {
        AamsModuleScreen(onBack = { currentScreen = "home" })
        return
    }

    // 监听应用前后台切换，从系统设置返回时能够立即感知并刷新状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceActive = FloatingService.isServiceRunning
                isAccessibilityActive = AccessibilityUtils.isAccessibilityServiceEnabled(context)
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
            Text(
                text = "当前版本 v${com.soar.sparkai.BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 6.dp)
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
                                AppLogger.i("HomeScreen", "用户将常驻助手开关切换为 -> $isChecked")
                                if (isChecked) {
                                    // 检查系统悬浮窗权限
                                    if (Settings.canDrawOverlays(context)) {
                                        AppLogger.i("HomeScreen", "系统悬浮窗权限校验通过，正在启动前台悬浮服务。")
                                        startFloatingService(context)
                                        isServiceActive = true
                                    } else {
                                        AppLogger.w("HomeScreen", "未检测到悬浮窗权限！弹出授权引导对话框。")
                                        // 无权限则展示高颜值引导弹窗
                                        showPermissionDialog = true
                                    }
                                } else {
                                    AppLogger.i("HomeScreen", "用户主动关闭开关，正在停止并注销前台悬浮服务。")
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

            Spacer(modifier = Modifier.height(20.dp))

            // ================== AI 自动化授权助手（无障碍）卡片 ==================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2F)
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
                        // 带有渐变效果的高端闪电图标（代表极速自动授权）
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF00B894), Color(0xFF00CEC9))
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = "无障碍",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "AI 自动化授权助手",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // 动态渲染状态指示器（带色彩渐变的 Badge）
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAccessibilityActive) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = "状态",
                                    tint = if (isAccessibilityActive) Color(0xFF00B894) else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAccessibilityActive) "自动代点已激活" else "未开启",
                                    fontSize = 12.sp,
                                    color = if (isAccessibilityActive) Color(0xFF00B894) else Color.White.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 极简现代的 Switch 控制器
                        Switch(
                            checked = isAccessibilityActive,
                            onCheckedChange = { isChecked ->
                                AppLogger.i("HomeScreen", "用户切换自动化授权开关 -> $isChecked")
                                try {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                    Toast.makeText(
                                        context, 
                                        if (isChecked) "请在已下载的应用/服务列表中开启“SparkAI 自动化授权助手”" else "请在已下载的应用/服务列表中关闭“SparkAI 自动化授权助手”", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    AppLogger.e("HomeScreen", "无法打开系统无障碍设置页面", e)
                                    Toast.makeText(context, "无法跳转至无障碍设置，请手动在系统设置中搜索开启", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00B894),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "基于 Android 系统无障碍辅助服务。开启后，每当进行屏幕截图或分析时，助手将瞬间自动同意系统的投屏安全警告弹窗，达成“一次授权后永久后台静默运行”的零打扰体验。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================== 📂 局域网高速互传中心卡片 ==================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2F)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "📂 局域网高速互传中心",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "无需任何 USB 数据线，即可与 SparkAI 桌面端建立局域网安全数据通道。一键向电脑端推送手机照片与文件，亦可实时接收来自电脑发来的要约与多媒体素材。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            AppLogger.i("HomeScreen", "用户进入局域网高速文件互传界面。")
                            currentScreen = "transfer"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C5CE7)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("进入文件传输控制台", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================== 🤖 AI 智能助理中心卡片 ==================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2F)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🤖 AI 智能助理中心",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "集成小米 MiMo 智能大模型直连。不仅支持极速流式对话，若您开启「常驻屏幕助手」，还可在任何第三方界面随时截图，进入此处即可一键提取图片信息并完成智能多模态视觉破译分析。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            AppLogger.i("HomeScreen", "用户进入 AI 智能对话界面。")
                            currentScreen = "ai"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE94057) // 高端玫红，与常驻助手开关相称
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("进入 AI 智能助理", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================== 🧩 AI 自定义模块中心卡片 ==================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2F)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🧩 AI 自定义模块中心",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SparkAI 独家推出指令模块热装载引擎。支持直接装载大模型为您私人订制的屏幕截图分析指令，例如商品八折求和、均价分析、圈画特定文字等，赋能悬浮助手无限可能。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            AppLogger.i("HomeScreen", "用户进入 AI 自定义模块控制面板。")
                            currentScreen = "aams"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1C40F)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("进入 AI 自定义模块", color = Color(0xFF0F0F1A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================== ⚙ 高端诊断与日志系统卡片 ==================
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
                    Text(
                        text = "⚙ 诊断与日志中心",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "系统会详细记录手势轨迹、前台保活与截图状态。如果在测试过程中遇到点击无反应等疑问，可随时进入可视化控制台实时分析与诊断。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            AppLogger.i("HomeScreen", "用户请求开启实时运行日志控制台。")
                            val intent = Intent(context, com.soar.sparkai.core.log.LogDisplayActivity::class.java)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B894)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("查看实时运行日志", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            AppLogger.i("HomeScreen", "用户手动触发在线自更新检测。")
                            UpdateManager.checkUpdate(manual = true, context = context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C5CE7)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("在线检查新版本", color = Color.White, fontWeight = FontWeight.Bold)
                    }
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
                            AppLogger.i("HomeScreen", "引导去授权：正在重定向用户至系统的悬浮窗（显示在其他应用上层）设置页。")
                            showPermissionDialog = false
                            // 跳转至系统悬浮窗权限配置界面
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                AppLogger.e("HomeScreen", "重定向特定包名的悬浮窗配置失败，尝试打开全局悬浮窗配置列表。", e)
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


