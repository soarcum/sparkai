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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AamsMatchTesterManager {

    private var testerOverlayView: ComposeView? = null

    fun removeTesterOverlay(context: Context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        testerOverlayView?.let { view ->
            try {
                if (view.parent != null) {
                    wm.removeView(view)
                }
            } catch (e: Exception) {
                AppLogger.e("TesterManager", "移除对齐测试层失败: ${e.message}")
            }
            testerOverlayView = null
        }
    }

    fun showAlignmentTesterOverlay(context: Context, service: LifecycleOwner) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        removeTesterOverlay(context)

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, // 初始不使用 FLAG_NOT_FOCUSABLE 以便获取软键盘输入
            PixelFormat.TRANSLUCENT
        )

        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(service)
            if (service is ViewModelStoreOwner) setViewTreeViewModelStoreOwner(service)
            if (service is SavedStateRegistryOwner) setViewTreeSavedStateRegistryOwner(service)
        }

        fun performMatchAction(
            queryText: String,
            highlights: androidx.compose.runtime.snapshots.SnapshotStateList<HighlightBound>
        ) {
            val q = queryText.trim()
            if (q.isEmpty()) {
                Toast.makeText(context, "请输入要匹配的文字", Toast.LENGTH_SHORT).show()
                return
            }

            android.util.Log.d("TesterManager", "[对齐测试] 开始执行匹配，查询文本: '$q'")

            CoroutineScope(Dispatchers.Main).launch {
                // 1. 自动移开 Overlay 焦点 & 物理退避（暂时设为 1x1 物理尺寸），使得无障碍服务能完美提取底层所有文字，彻底消除遮挡
                val originalWidth = overlayParams.width
                val originalHeight = overlayParams.height
                overlayParams.width = 1
                overlayParams.height = 1
                overlayParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                try {
                    wm.updateViewLayout(composeView, overlayParams)
                } catch (e: Exception) {
                    android.util.Log.e("TesterManager", "[对齐测试] 移开焦点与尺寸退避发生异常: ${e.message}")
                }

                // 2. 延迟 250ms，留出时间让系统更新底层的 Activity 焦点并刷新 UI 无障碍树
                kotlinx.coroutines.delay(250)

                // 3. 用无障碍服务实时抓取当前屏幕下层的所有 TextView 物理节点与坐标
                val accessibilityService = com.soar.sparkai.feature.accessibility.service.SparkAccessibilityService.instance
                android.util.Log.d("TesterManager", "[对齐测试] 无障碍服务实例状态: ${if (accessibilityService != null) "已连接" else "为空(未开启)"}")
                
                val textNodesList = mutableListOf<com.soar.sparkai.feature.accessibility.util.ScreenTextNode>()
                if (accessibilityService != null) {
                    val windowsList = accessibilityService.windows
                    android.util.Log.d("TesterManager", "[对齐测试] 获取到的交互窗口数量: ${windowsList?.size ?: 0}")
                    if (!windowsList.isNullOrEmpty()) {
                        for (window in windowsList) {
                            val root = window.root
                            android.util.Log.d("TesterManager", "[对齐测试] 窗口 ID: ${window.id}, Root节点是否为空: ${root == null}")
                            if (root != null) {
                                textNodesList.addAll(
                                    com.soar.sparkai.feature.accessibility.util.AccessibilityTextExtractor.extractVisibleTexts(root)
                                )
                            }
                        }
                    }
                    if (textNodesList.isEmpty()) {
                        val activeRoot = accessibilityService.rootInActiveWindow
                        android.util.Log.d("TesterManager", "[对齐测试] windows 列表为空或获取失败，尝试 rootInActiveWindow，是否为空: ${activeRoot == null}")
                        if (activeRoot != null) {
                            textNodesList.addAll(
                                com.soar.sparkai.feature.accessibility.util.AccessibilityTextExtractor.extractVisibleTexts(activeRoot)
                            )
                        }
                    }
                }
                val freshNodes = textNodesList
                android.util.Log.d("TesterManager", "[对齐测试] 无障碍抓取结束，共捕获到 ${freshNodes.size} 个可见文本元素")

                // 4. 恢复 Overlay 为可聚焦状态以及原有的 MATCH_PARENT 铺满大小
                overlayParams.width = originalWidth
                overlayParams.height = originalHeight
                overlayParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                try {
                    wm.updateViewLayout(composeView, overlayParams)
                } catch (e: Exception) {
                    android.util.Log.e("TesterManager", "[对齐测试] 恢复焦点与尺寸发生异常: ${e.message}")
                }

                // 5. 对抓取到的高精度物理坐标执行本地实时匹配
                highlights.clear()
                var matchCount = 0
                freshNodes.forEach { node ->
                    if (node.text.contains(q, ignoreCase = true)) {
                        val rect = node.bounds
                        val xmin = (rect.left.toFloat() / screenWidth) * 100f
                        val ymin = (rect.top.toFloat() / screenHeight) * 100f
                        val xmax = (rect.right.toFloat() / screenWidth) * 100f
                        val ymax = (rect.bottom.toFloat() / screenHeight) * 100f
                        highlights.add(
                            HighlightBound(
                                originalValue = node.text,
                                calculatedValue = "",
                                ymin = ymin,
                                xmin = xmin,
                                ymax = ymax,
                                xmax = xmax
                            )
                        )
                        matchCount++
                    }
                }

                android.util.Log.d("TesterManager", "[对齐测试] 本次文本过滤匹配结束，成功匹配到 ${matchCount} 个方框")

                if (matchCount > 0) {
                    Toast.makeText(context, "自动对齐框选成功！找到 $matchCount 处匹配文本", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "未在屏幕中寻寻找该文字（共解析到 ${freshNodes.size} 个可见文本元素）", Toast.LENGTH_LONG).show()
                }
            }
        }

        composeView.setContent {
            AppTheme {
                val queryTextState = remember { mutableStateOf("") }
                val highlights = remember { mutableStateListOf<HighlightBound>() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    // 1. Canvas 霓虹高亮红色圈画
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        highlights.forEach { bound ->
                            val pxMin = (bound.xmin / 100f) * size.width
                            val pyMin = (bound.ymin / 100f) * size.height
                            val pxMax = (bound.xmax / 100f) * size.width
                            val pyMax = (bound.ymax / 100f) * size.height

                            if (pxMax > pxMin && pyMax > pyMin) {
                                drawRoundRect(
                                    color = Color(0xFFFF0055).copy(alpha = 0.3f),
                                    topLeft = Offset(pxMin - 3.dp.toPx(), pyMin - 3.dp.toPx()),
                                    size = Size(pxMax - pxMin + 6.dp.toPx(), pyMax - pyMin + 6.dp.toPx()),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                                drawRoundRect(
                                    color = Color(0xFFFF0055),
                                    topLeft = Offset(pxMin, pyMin),
                                    size = Size(pxMax - pxMin, pyMax - pyMin),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }

                    // 2. 对齐测试控制卡片面板
                    Column(
                        modifier = Modifier
                            .width(310.dp)
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .shadow(16.dp, RoundedCornerShape(24.dp))
                            .background(Color(0xFF131324), RoundedCornerShape(24.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎯 无障碍对齐校准工具",
                            color = Color(0xFFF1C40F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "输入文字，点击“对齐测试”即可实时框选目标",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        androidx.compose.material3.OutlinedTextField(
                            value = queryTextState.value,
                            onValueChange = { queryTextState.value = it },
                            placeholder = { Text("输入要匹配框选的文字...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { performMatchAction(queryTextState.value, highlights) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("对齐测试", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { removeTesterOverlay(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031)),
                                modifier = Modifier.weight(1.0f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("退出", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        testerOverlayView = composeView
        wm.addView(composeView, overlayParams)
    }
}
