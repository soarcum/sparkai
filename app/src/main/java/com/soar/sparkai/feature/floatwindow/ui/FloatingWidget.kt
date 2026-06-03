package com.soar.sparkai.feature.floatwindow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import com.soar.sparkai.core.log.AppLogger
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.soar.sparkai.feature.ai.model.AamsModule
import com.soar.sparkai.feature.ai.util.AamsModuleManager

/**
 * 高端常驻悬浮窗及展开面板 Compose 视图
 * 
 * 作用：绘制悬浮球及折叠展开的功能菜单。
 * 特色：
 * 1. 采用高饱和度渐变炫彩圆形悬浮球，支持柔和脉冲呼吸外发光。
 * 2. 拟物化高斯模糊面板设计，带下沉微动触控反馈的精美图标按键。
 * 3. 动态加载已启用的 AAMS 模块子面板，让 AI 自定义能力热插拔呈现。
 */
@Composable
fun FloatingWidget(
    isLoading: Boolean,
    onDrag: (dx: Int, dy: Int) -> Unit,
    onDragEnd: () -> Unit,
    onActionScreenshot: () -> Unit,
    onActionExecuteModule: (moduleId: String) -> Unit,
    onActionClose: () -> Unit,
    onActionBackToApp: () -> Unit
) {
    // 展开状态标记
    var isExpanded by remember { mutableStateOf(false) }
    // 子模块选择列表展示标记
    var showModuleList by remember { mutableStateOf(false) }

    // 每次面板重新展开时，重置子模块的展开状态
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            showModuleList = false
        }
    }

    val context = LocalContext.current
    val enabledModules = remember(isExpanded) {
        AamsModuleManager.getAllModules(context).filter { it.enabled }
    }

    // 呼吸动画控制：使未展开的悬浮球自带微光呼吸感
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EasyInOutSineEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 交互源，消除默认水波纹以呈现高级微动按压
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp), // 留出呼吸和投影的绘制空间
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            // ================== 1. 静态收折态：炫彩悬浮球 ==================
            Box(
                modifier = Modifier
                    .scale(pulseScale) // 注入灵动的微光呼吸动效
                    .size(56.dp)
                    .shadow(
                        elevation = 8.dp, 
                        shape = CircleShape, 
                        ambientColor = Color(0xFFE94057).copy(alpha = 0.5f),
                        spotColor = Color(0xFF8A2387).copy(alpha = 0.5f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8A2387), // 优雅深紫
                                Color(0xFFE94057), // 炫彩玫红
                                Color(0xFFF27121)  // 活力暖橙
                             )
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                AppLogger.i("FloatingWidget", "炫彩悬浮球被轻点，正在优雅展开快捷助理面板。")
                                isExpanded = true
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                onDragEnd()
                            },
                            onDragCancel = {
                                onDragEnd()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                            }
                        )
                    }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                } else {
                    Text(
                        text = "S",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        } else {
            // ================== 2. 展开态：高级拟物毛玻璃控制卡片 ==================
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .shadow(
                            elevation = 12.dp, 
                            shape = RoundedCornerShape(24.dp),
                            clip = false
                        )
                        .background(
                            color = Color(0xF21F1F2E), 
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部小标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SparkAI 便捷助理",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // 快速折叠按键
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowLeft,
                            contentDescription = "折叠",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    AppLogger.i("FloatingWidget", "折叠快捷图标被点击，正在将面板收折回悬浮球。")
                                    isExpanded = false
                                    onDragEnd()
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 功能按键横排展示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 1. AI 自定义模块智能菜单触发键
                        ActionButton(
                            icon = Icons.Rounded.AutoAwesome,
                            label = "AI模块",
                            backgroundColor = Color(0xFFF1C40F).copy(alpha = 0.15f),
                            tint = Color(0xFFF39C12),
                            onClick = {
                                if (enabledModules.isEmpty()) {
                                    android.widget.Toast.makeText(context, "请先在主页“AI自定义模块”装载并启用模块", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    AppLogger.i("FloatingWidget", "展开 AAMS 子模块配置列表。")
                                    showModuleList = !showModuleList
                                }
                            }
                        )

                        // 2. 截图按键
                        ActionButton(
                            icon = Icons.Rounded.Crop,
                            label = "截图",
                            backgroundColor = Color(0xFF6C5CE7).copy(alpha = 0.2f),
                            tint = Color(0xFFA8A3FF),
                            onClick = {
                                AppLogger.i("FloatingWidget", "功能触发：用户点击了“截图”按键。")
                                isExpanded = false
                                onActionScreenshot()
                            }
                        )

                        // 3. 主页按键
                        ActionButton(
                            icon = Icons.Rounded.Home,
                            label = "主页",
                            backgroundColor = Color(0xFF00B894).copy(alpha = 0.2f),
                            tint = Color(0xFF55EFC4),
                            onClick = {
                                AppLogger.i("FloatingWidget", "功能触发：用户点击了“主页”按键，返回主界面。")
                                onActionBackToApp()
                            }
                        )

                        // 4. 关闭按键
                        ActionButton(
                            icon = Icons.Rounded.Close,
                            label = "退出",
                            backgroundColor = Color(0xFFD63031).copy(alpha = 0.2f),
                            tint = Color(0xFFFF7675),
                            onClick = {
                                AppLogger.i("FloatingWidget", "功能触发：用户点击了“退出”按键，正在注销悬浮服务。")
                                onActionClose()
                            }
                        )
                    }

                    // ================== 子模块列表平滑滑出展示 ==================
                    AnimatedVisibility(
                        visible = showModuleList && enabledModules.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(Color(0xFF141421), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "🧩 请选择要运行的 AI 模块:",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            enabledModules.forEach { module ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            AppLogger.i("FloatingWidget", "触发执行 AAMS 模块: ${module.name} (ID: ${module.id})")
                                            isExpanded = false
                                            showModuleList = false
                                            onActionExecuteModule(module.id)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (module.isSystem) Color(0xFFF1C40F) else Color(0xFF00B894),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = module.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "当前版本 v${com.soar.sparkai.BuildConfig.VERSION_NAME}",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 悬浮面板功能按钮组件
 * 
 * 带有微动画的按压态及极简美学文字排版。
 */
@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "buttonPress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(backgroundColor, shape = CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// 经典的缓入缓出插值曲线，提供高品质动效
val EasyInOutSineEasing = Easing { fraction ->
    -(cos(Math.PI * fraction) - 1f).toFloat() / 2f
}
private fun cos(x: Double): Double = kotlin.math.cos(x)
