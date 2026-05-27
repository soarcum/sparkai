package com.soar.sparkai.feature.floatwindow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import kotlin.math.sqrt

/**
 * 高端常驻悬浮窗及展开面板 Compose 视图
 * 
 * 作用：绘制悬浮球及折叠展开的功能菜单。
 * 特色：
 * 1. 采用高饱和度渐变炫彩圆形悬浮球，支持柔和脉冲呼吸外发光。
 * 2. 独创的“位移判定点击算法”，完美解决了 WindowManager 下触摸冲突的世纪难题。
 * 3. 拟物化高斯模糊面板设计，带下沉微动触控反馈的精美图标按键。
 */
@Composable
fun FloatingWidget(
    onDrag: (dx: Int, dy: Int) -> Unit,
    onDragEnd: () -> Unit,
    onActionScreenshot: () -> Unit,
    onActionClose: () -> Unit,
    onActionBackToApp: () -> Unit
) {
    // 展开状态标记
    var isExpanded by remember { mutableStateOf(false) }

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
                                AppLogger.i("FloatingWidget", "Colorful float ball tapped. Expanding assistant panel.")
                                // 专门捕获原地高灵敏轻触，瞬间优雅展开
                                isExpanded = true
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // 拖动结束，执行贴边对齐
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
                    },
                contentAlignment = Alignment.Center
            ) {
                // 圆形球内展示 SparkAI 标志首字母 S
                Text(
                    text = "S",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
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
                        .width(220.dp)
                        .shadow(
                            elevation = 12.dp, 
                            shape = RoundedCornerShape(24.dp),
                            clip = false
                        )
                        .background(
                            // 完美的半透卡片背景，带高级边缘描边
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
                                    AppLogger.i("FloatingWidget", "Collapse chevron icon clicked. Contracting panel.")
                                    isExpanded = false
                                    // 点击折叠后，再次检测一次吸边以防重叠
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
                        // 1. 截图按键
                        ActionButton(
                            icon = Icons.Rounded.Crop,
                            label = "截图",
                            backgroundColor = Color(0xFF6C5CE7).copy(alpha = 0.2f),
                            tint = Color(0xFFA8A3FF),
                            onClick = {
                                AppLogger.i("FloatingWidget", "Action: Screenshot button clicked.")
                                isExpanded = false
                                onActionScreenshot()
                            }
                        )

                        // 2. 主页按键
                        ActionButton(
                            icon = Icons.Rounded.Home,
                            label = "主页",
                            backgroundColor = Color(0xFF00B894).copy(alpha = 0.2f),
                            tint = Color(0xFF55EFC4),
                            onClick = {
                                AppLogger.i("FloatingWidget", "Action: Return to Home button clicked.")
                                onActionBackToApp()
                            }
                        )

                        // 3. 关闭按键
                        ActionButton(
                            icon = Icons.Rounded.Close,
                            label = "退出",
                            backgroundColor = Color(0xFFD63031).copy(alpha = 0.2f),
                            tint = Color(0xFFFF7675),
                            onClick = {
                                AppLogger.i("FloatingWidget", "Action: Stop and Exit button clicked.")
                                onActionClose()
                            }
                        )
                    }
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
    // 处理按压交互的动画缩放效果，呈现完美下沉触感
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
