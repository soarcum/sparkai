package com.soar.sparkai.feature.floatwindow.util

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.soar.sparkai.core.theme.AppTheme
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.feature.floatwindow.service.HighlightBound

object AamsFullscreenOverlayManager {

    private var fullscreenOverlayView: ComposeView? = null

    fun removeFullscreenOverlay(context: Context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        fullscreenOverlayView?.let { view ->
            try {
                if (view.parent != null) {
                    wm.removeView(view)
                }
            } catch (e: Exception) {
                AppLogger.e("OverlayManager", "移除全屏覆盖层失败: ${e.message}")
            }
            fullscreenOverlayView = null
        }
    }

    fun showLoadingOverlay(context: Context, service: LifecycleOwner, moduleName: String) {
        removeFullscreenOverlay(context)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(service)
            if (service is ViewModelStoreOwner) setViewTreeViewModelStoreOwner(service)
            if (service is SavedStateRegistryOwner) setViewTreeSavedStateRegistryOwner(service)

            setContent {
                AppTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { removeFullscreenOverlay(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .width(260.dp)
                                .background(Color(0xFF1E1E2F), RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFF1C40F),
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "🔮 正在执行 AAMS 模块...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = moduleName,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        fullscreenOverlayView = view
        wm.addView(view, overlayParams)
    }

    fun showFullscreenOverlay(
        context: Context,
        service: LifecycleOwner,
        moduleName: String,
        totalSum: Double,
        numbersList: List<String>,
        explanation: String,
        highlights: List<HighlightBound>
    ) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        removeFullscreenOverlay(context)

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(service)
            if (service is ViewModelStoreOwner) setViewTreeViewModelStoreOwner(service)
            if (service is SavedStateRegistryOwner) setViewTreeSavedStateRegistryOwner(service)

            setContent {
                AppTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { removeFullscreenOverlay(context) }
                    ) {
                        // 1. 全屏 Canvas：原位 AR 渲染
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            highlights.forEach { bound ->
                                val pxMin = (bound.xmin / 100f) * size.width
                                val pyMin = (bound.ymin / 100f) * size.height
                                val pxMax = (bound.xmax / 100f) * size.width
                                val pyMax = (bound.ymax / 100f) * size.height

                                if (pxMax > pxMin && pyMax > pyMin) {
                                    val origDouble = bound.originalValue.toDoubleOrNull()
                                    val calDouble = bound.calculatedValue.toDoubleOrNull()
                                    val isNumeric = origDouble != null && calDouble != null

                                    if (isNumeric) {
                                        if (origDouble != calDouble) {
                                            // 原价格删除线
                                            drawLine(
                                                color = Color(0xFFFF7675).copy(alpha = 0.35f),
                                                start = Offset(pxMin - 2.dp.toPx(), pyMin + 2.dp.toPx()),
                                                end = Offset(pxMax + 2.dp.toPx(), pyMax - 2.dp.toPx()),
                                                strokeWidth = 4.dp.toPx()
                                            )
                                            drawLine(
                                                color = Color(0xFFFF7675),
                                                start = Offset(pxMin, pyMin + 2.dp.toPx()),
                                                end = Offset(pxMax, pyMax - 2.dp.toPx()),
                                                strokeWidth = 2.dp.toPx()
                                            )

                                            // 折后价格胶囊背景
                                            drawRoundRect(
                                                color = Color(0xF21F1F2E),
                                                topLeft = Offset(pxMin - 2.dp.toPx(), pyMax + 2.dp.toPx()),
                                                size = Size((pxMax - pxMin + 4.dp.toPx()).coerceAtLeast(40.dp.toPx()), 15.dp.toPx()),
                                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                            )

                                            // 绘制折后价格文字
                                            val paint = android.graphics.Paint().apply {
                                                color = android.graphics.Color.parseColor("#00B894")
                                                textSize = 9.sp.toPx()
                                                isAntiAlias = true
                                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                            }
                                            val textToDraw = String.format("%.2f", calDouble)
                                            drawContext.canvas.nativeCanvas.drawText(
                                                textToDraw,
                                                pxMin,
                                                pyMax + 13.dp.toPx(),
                                                paint
                                            )
                                        } else {
                                            // 普通高亮方框
                                            drawRoundRect(
                                                color = Color(0xFFE94057).copy(alpha = 0.35f),
                                                topLeft = Offset(pxMin - 4.dp.toPx(), pyMin - 4.dp.toPx()),
                                                size = Size(pxMax - pxMin + 8.dp.toPx(), pyMax - pyMin + 8.dp.toPx()),
                                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                                style = Stroke(width = 4.dp.toPx())
                                            )
                                            drawRoundRect(
                                                color = Color(0xFF00B894),
                                                topLeft = Offset(pxMin, pyMin),
                                                size = Size(pxMax - pxMin, pyMax - pyMin),
                                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    } else {
                                        // 文本翻译覆盖
                                        if (bound.originalValue != bound.calculatedValue && bound.calculatedValue.isNotEmpty()) {
                                            drawRoundRect(
                                                color = Color(0xFA1F1F2E),
                                                topLeft = Offset(pxMin - 2.dp.toPx(), pyMin - 2.dp.toPx()),
                                                size = Size(pxMax - pxMin + 4.dp.toPx(), pyMax - pyMin + 4.dp.toPx()),
                                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                            )

                                            val textPaint = android.graphics.Paint().apply {
                                                color = android.graphics.Color.WHITE
                                                isAntiAlias = true
                                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                                textAlign = android.graphics.Paint.Align.CENTER
                                            }

                                            val boxHeight = pyMax - pyMin
                                            val calculatedTextSize = (boxHeight * 0.72f).coerceIn(10.sp.toPx(), 18.sp.toPx())
                                            textPaint.textSize = calculatedTextSize

                                            val centerX = (pxMin + pxMax) / 2
                                            val centerY = (pyMin + pyMax) / 2
                                            val yBaseline = centerY - (textPaint.descent() + textPaint.ascent()) / 2

                                            drawContext.canvas.nativeCanvas.drawText(
                                                bound.calculatedValue,
                                                centerX,
                                                yBaseline,
                                                textPaint
                                            )
                                        } else {
                                            // 普通微调高亮框
                                            drawRoundRect(
                                                color = Color(0xFF00B894).copy(alpha = 0.5f),
                                                topLeft = Offset(pxMin, pyMin),
                                                size = Size(pxMax - pxMin, pyMax - pyMin),
                                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                                style = Stroke(width = 1.5.dp.toPx())
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. 中央控制面板
                        Column(
                            modifier = Modifier
                                .width(310.dp)
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 48.dp)
                                .shadow(16.dp, RoundedCornerShape(28.dp), clip = false)
                                .background(Color(0xFF1F1F2E), RoundedCornerShape(28.dp))
                                .padding(24.dp)
                                .clickable(enabled = false) {},
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = moduleName,
                                color = Color(0xFFF1C40F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            val isNumericModule = numbersList.isNotEmpty() && numbersList.any { it.toDoubleOrNull() != null }

                            if (isNumericModule) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = String.format("%.2f", totalSum),
                                    color = Color.White,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Total Aggregated Value (合计结果)",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "🔢 被提取数据列表:",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = numbersList.joinToString(" + "),
                                    color = Color(0xFF00B894),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Start),
                                    maxLines = 3
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "🤖 $explanation",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(22.dp))

                            Button(
                                onClick = { removeFullscreenOverlay(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("我知道了", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        fullscreenOverlayView = view
        wm.addView(view, overlayParams)
    }
}
