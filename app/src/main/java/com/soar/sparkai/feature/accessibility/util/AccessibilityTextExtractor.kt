package com.soar.sparkai.feature.accessibility.util

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.soar.sparkai.core.log.AppLogger

/**
 * 屏幕物理文本节点信息实体
 */
data class ScreenTextNode(
    val text: String,
    val bounds: Rect,
    val isEditable: Boolean
)

/**
 * Android 无障碍服务文字及精确物理坐标提取工具
 */
object AccessibilityTextExtractor {

    /**
     * 递归遍历当前活跃窗口的节点树，提取所有可见的文本元素及其物理坐标 Rect
     */
    fun extractVisibleTexts(rootNode: AccessibilityNodeInfo?): List<ScreenTextNode> {
        val result = mutableListOf<ScreenTextNode>()
        if (rootNode == null) return result

        try {
            traverseNode(rootNode, result)
        } catch (e: Exception) {
            AppLogger.e("AccessibilityTextExtractor", "遍历节点树提取文本时发生异常: ${e.message}", e)
        }
        return result
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<ScreenTextNode>) {
        if (node == null) return

        // 1. 提取当前节点的信息
        // 核心优化：如果是底层其它应用（包名不等于本应用包名），即使被悬浮窗遮挡导致 isVisibleToUser 为 false，也强行保留提取
        val packageNameStr = node.packageName?.toString()
        val isSelfApp = packageNameStr == "com.soar.sparkai"
        val shouldExtract = node.isVisibleToUser || (packageNameStr != null && !isSelfApp)

        if (shouldExtract) {
            // 优先提取 text，若为空则尝试提取 contentDescription（对图标、自定义View极重要）
            val rawText = node.text ?: node.contentDescription
            val textVal = rawText?.toString()?.trim()
            if (!textVal.isNullOrEmpty()) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                // 确保有合法的物理宽高且内容不是过长的长文
                if (rect.width() > 0 && rect.height() > 0 && textVal.length <= 500) {
                    list.add(
                        ScreenTextNode(
                            text = textVal,
                            bounds = rect,
                            isEditable = node.isEditable
                        )
                    )
                    val logMsg = "成功提取节点 [Pkg: $packageNameStr, Text: $textVal, Bounds: $rect, VisibleToUser: ${node.isVisibleToUser}, Editable: ${node.isEditable}]"
                    AppLogger.d("AccessibilityTextExtractor", logMsg)
                    android.util.Log.d("AccessibilityTextExtractor", logMsg)
                }
            }
        }

        // 2. 递归遍历子节点
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, list)
            }
        }
    }
}
