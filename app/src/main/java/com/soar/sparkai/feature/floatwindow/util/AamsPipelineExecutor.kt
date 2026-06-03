package com.soar.sparkai.feature.floatwindow.util

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.feature.accessibility.util.ScreenTextNode
import com.soar.sparkai.feature.ai.model.AamsModule
import com.soar.sparkai.feature.ai.service.AiService
import com.soar.sparkai.feature.ai.util.AamsModelConfigManager
import com.soar.sparkai.feature.ai.util.AiConfigManager
import com.soar.sparkai.feature.floatwindow.service.HighlightBound
import com.soar.sparkai.feature.floatwindow.util.ScreenshotHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AamsPipelineExecutor {

    fun executeTextModePipeline(
        context: Context,
        service: LifecycleOwner,
        module: AamsModule,
        textNodes: List<ScreenTextNode>,
        setWidgetLoading: (Boolean) -> Unit,
        showFloatingWindow: () -> Unit
    ) {
        setWidgetLoading(true)
        Toast.makeText(context, "🔮 精准模式：正在使用无障碍提取屏幕文字...", Toast.LENGTH_SHORT).show()

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. 获取模块关联的模型配置
                val modelConfig = AamsModelConfigManager.getConfigOrDefault(context, module.modelConfigId)

                // 2. 匹配 API 密钥和 Base URL
                val configApiKey = modelConfig.apiKey
                val configBaseUrl = modelConfig.baseUrl
                val apiKey = if (!configApiKey.isNullOrEmpty()) configApiKey else AiConfigManager.getApiKey(context)
                val baseUrl = if (!configBaseUrl.isNullOrEmpty()) configBaseUrl else AiConfigManager.getBaseUrl(context)

                // 3. 构建高精度文本模式的 Prompt
                val textsString = textNodes.mapIndexed { index, node -> "${index + 1}. ${node.text}" }.joinToString("\n")
                val textModePrompt = "${module.prompt}\n\n" +
                    "【重要优化输入模式】：系统已提前提取屏幕全部文本节点，请直接对此文本列表进行翻译、计算等处理。不要自行捏造不存在的项。返回的 JSON 中，numbers 数组里的每个对象的 original_value 必须严格与以下列表中的项内容完全一致。你不用返回 ymin, xmin, ymax, xmax 坐标属性。\n\n" +
                    "待处理的屏幕文本列表：\n$textsString"

                var accumulatedReply = ""
                withContext(Dispatchers.IO) {
                    AiService.sendChatRequestStream(
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        model = modelConfig.model,
                        promptText = textModePrompt,
                        bitmap = null, // 纯文本模式
                        history = emptyList(),
                        temperature = modelConfig.temperature,
                        stop = modelConfig.stop,
                        frequencyPenalty = modelConfig.frequencyPenalty,
                        presencePenalty = modelConfig.presencePenalty,
                        thinkingType = modelConfig.thinkingType
                    ).collect { chunk ->
                        accumulatedReply += chunk
                    }
                }

                AppLogger.i("AamsExecutor", "[AAMS精准模式] 响应: $accumulatedReply")

                var cleanJson = accumulatedReply.trim()
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.substringAfter("```json").substringAfter("```").substringBeforeLast("```").trim()
                }

                // 解析 JSON 数据与相对坐标集
                val json = JSONObject(cleanJson)
                val totalSum = json.optDouble("total_sum", 0.0)
                val explanation = json.optString("explanation", "分析就绪")

                val numbersList = mutableListOf<String>()
                val highlights = mutableListOf<HighlightBound>()

                val numbersArr = json.optJSONArray("numbers")
                if (numbersArr != null) {
                    for (i in 0 until numbersArr.length()) {
                        val item = numbersArr.optJSONObject(i)
                        if (item != null) {
                            val calVal = if (item.has("calculated_value")) {
                                item.optString("calculated_value", "")
                            } else if (item.has("value")) {
                                item.optString("value", "")
                            } else {
                                ""
                            }
                            val origVal = if (item.has("original_value")) {
                                item.optString("original_value", "")
                            } else if (item.has("value")) {
                                item.optString("value", "")
                            } else {
                                ""
                            }

                            if (calVal.isNotEmpty()) {
                                numbersList.add(calVal)
                            }

                            // 核心匹配算法
                            val matchedNode = findBestMatchedNode(origVal, textNodes)
                            if (matchedNode != null) {
                                val rect = matchedNode.bounds
                                val xmin = (rect.left.toFloat() / screenWidth) * 100f
                                val ymin = (rect.top.toFloat() / screenHeight) * 100f
                                val xmax = (rect.right.toFloat() / screenWidth) * 100f
                                val ymax = (rect.bottom.toFloat() / screenHeight) * 100f

                                highlights.add(
                                    HighlightBound(
                                        originalValue = origVal,
                                        calculatedValue = calVal,
                                        ymin = ymin,
                                        xmin = xmin,
                                        ymax = ymax,
                                        xmax = xmax
                                    )
                                )
                            } else if (item.has("ymin") && item.has("xmin") && item.has("ymax") && item.has("xmax")) {
                                highlights.add(
                                    HighlightBound(
                                        originalValue = origVal,
                                        calculatedValue = calVal,
                                        ymin = item.optDouble("ymin").toFloat(),
                                        xmin = item.optDouble("xmin").toFloat(),
                                        ymax = item.optDouble("ymax").toFloat(),
                                        xmax = item.optDouble("xmax").toFloat()
                                    )
                                )
                            }
                        }
                    }
                }

                // 呈现全屏遮罩
                AamsFullscreenOverlayManager.showFullscreenOverlay(
                    context = context,
                    service = service,
                    moduleName = module.name,
                    totalSum = totalSum,
                    numbersList = numbersList,
                    explanation = explanation,
                    highlights = highlights
                )

            } catch (e: Exception) {
                AppLogger.e("AamsExecutor", "[AAMS精准模式] 请求处理失败", e)
                Toast.makeText(context, "高精度分析失败: ${e.message}", Toast.LENGTH_LONG).show()
                AamsFullscreenOverlayManager.removeFullscreenOverlay(context)
            } finally {
                setWidgetLoading(false)
                showFloatingWindow()
            }
        }
    }

    private fun findBestMatchedNode(origVal: String, nodes: List<ScreenTextNode>): ScreenTextNode? {
        val cleanOrig = origVal.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (cleanOrig.isEmpty()) return null

        val exactMatch = nodes.find { it.text.trim().equals(cleanOrig, ignoreCase = true) }
        if (exactMatch != null) return exactMatch

        return nodes.find {
            it.text.trim().contains(cleanOrig, ignoreCase = true) ||
                    cleanOrig.contains(it.text.trim(), ignoreCase = true)
        }
    }

    fun executeVisionModePipeline(
        context: Context,
        service: LifecycleOwner,
        reader: android.media.ImageReader,
        module: AamsModule,
        isFirstTime: Boolean,
        setWidgetLoading: (Boolean) -> Unit,
        showFloatingWindow: () -> Unit
    ) {
        setWidgetLoading(true)
        Toast.makeText(context, "🔮 视觉模式：正在截屏读取屏幕...", Toast.LENGTH_SHORT).show()

        ScreenshotHelper.captureScreenForAI(context, reader, isFirstTime) { bitmap ->
            if (bitmap != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val modelConfig = AamsModelConfigManager.getConfigOrDefault(context, module.modelConfigId)
                        val configApiKey = modelConfig.apiKey
                        val configBaseUrl = modelConfig.baseUrl
                        val apiKey = if (!configApiKey.isNullOrEmpty()) configApiKey else AiConfigManager.getApiKey(context)
                        val baseUrl = if (!configBaseUrl.isNullOrEmpty()) configBaseUrl else AiConfigManager.getBaseUrl(context)

                        var accumulatedReply = ""
                        withContext(Dispatchers.IO) {
                            AiService.sendChatRequestStream(
                                apiKey = apiKey,
                                baseUrl = baseUrl,
                                model = modelConfig.model,
                                promptText = module.prompt,
                                bitmap = bitmap,
                                history = emptyList(),
                                temperature = modelConfig.temperature,
                                stop = modelConfig.stop,
                                frequencyPenalty = modelConfig.frequencyPenalty,
                                presencePenalty = modelConfig.presencePenalty,
                                thinkingType = modelConfig.thinkingType
                            ).collect { chunk ->
                                accumulatedReply += chunk
                            }
                        }

                        AppLogger.i("AamsExecutor", "[AAMS视觉模式] 响应: $accumulatedReply")

                        var cleanJson = accumulatedReply.trim()
                        if (cleanJson.startsWith("```")) {
                            cleanJson = cleanJson.substringAfter("```json").substringAfter("```").substringBeforeLast("```").trim()
                        }

                        val json = JSONObject(cleanJson)
                        val totalSum = json.optDouble("total_sum", 0.0)
                        val explanation = json.optString("explanation", "分析就绪")

                        val numbersList = mutableListOf<String>()
                        val highlights = mutableListOf<HighlightBound>()

                        val numbersArr = json.optJSONArray("numbers")
                        if (numbersArr != null) {
                            for (i in 0 until numbersArr.length()) {
                                val item = numbersArr.optJSONObject(i)
                                if (item != null) {
                                    val calVal = if (item.has("calculated_value")) {
                                        item.optString("calculated_value", "")
                                    } else if (item.has("value")) {
                                        item.optString("value", "")
                                    } else {
                                        ""
                                    }
                                    val origVal = if (item.has("original_value")) {
                                        item.optString("original_value", "")
                                    } else if (item.has("value")) {
                                        item.optString("value", "")
                                    } else {
                                        ""
                                    }

                                    if (calVal.isNotEmpty()) {
                                        numbersList.add(calVal)
                                    }

                                    if (item.has("ymin") && item.has("xmin") && item.has("ymax") && item.has("xmax")) {
                                        highlights.add(
                                            HighlightBound(
                                                originalValue = origVal,
                                                calculatedValue = calVal,
                                                ymin = item.optDouble("ymin").toFloat(),
                                                xmin = item.optDouble("xmin").toFloat(),
                                                ymax = item.optDouble("ymax").toFloat(),
                                                xmax = item.optDouble("xmax").toFloat()
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        AamsFullscreenOverlayManager.showFullscreenOverlay(
                            context = context,
                            service = service,
                            moduleName = module.name,
                            totalSum = totalSum,
                            numbersList = numbersList,
                            explanation = explanation,
                            highlights = highlights
                        )

                    } catch (e: Exception) {
                        AppLogger.e("AamsExecutor", "[AAMS视觉模式] 请求处理失败", e)
                        val errMsg = e.message ?: ""
                        val hint = if (errMsg.contains("image input") || errMsg.contains("404")) {
                            "您的 AI 接口不支持多模态视觉输入，请确认已开启无障碍服务以使用高精度文本模式"
                        } else {
                            "视觉分析失败: ${e.message}"
                        }
                        Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
                        AamsFullscreenOverlayManager.removeFullscreenOverlay(context)
                    } finally {
                        setWidgetLoading(false)
                        bitmap.recycle()
                        showFloatingWindow()
                    }
                }
            } else {
                Toast.makeText(context, "截图失败，无法分析屏幕", Toast.LENGTH_SHORT).show()
                setWidgetLoading(false)
                showFloatingWindow()
            }
        }
    }
}
