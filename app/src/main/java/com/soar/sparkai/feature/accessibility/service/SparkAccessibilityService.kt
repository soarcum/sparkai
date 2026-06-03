package com.soar.sparkai.feature.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.soar.sparkai.core.log.AppLogger

/**
 * SparkAI 自动化授权辅助无障碍服务
 *
 * 作用：后台静默监听系统窗口事件。当检测到与 SparkAI 相关的系统投屏/录屏权限确认框时，
 * 按照正确的步骤自动完成授权：
 *   步骤 1: 点击"单个应用"下拉框展开选项
 *   步骤 2: 选择"整个屏幕"
 *   步骤 3: 点击"下一步"或"立即开始"确认按钮
 *
 * 核心判断策略：通过页面中同时可见的文本组合推断当前所处阶段，
 * 无需依赖厂商特定的容器类名（兼容 MIUI / HyperOS / 原生 Android）。
 */
class SparkAccessibilityService : AccessibilityService() {

    companion object {
        var instance: SparkAccessibilityService? = null
            private set
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            try {
                handleProjectionDialog(rootNode)
            } catch (e: Exception) {
                AppLogger.e("SparkAccessibility", "处理系统窗口节点异常: ${e.message}", e)
            }
        }
    }

    /**
     * 处理系统投屏授权对话框的多步骤自动化流程
     */
    private fun handleProjectionDialog(rootNode: AccessibilityNodeInfo) {
        val entireScreenNodes = findNodesByTexts(rootNode, "整个屏幕", "Entire screen")
        val singleAppNodes = findNodesByTexts(rootNode, "单个应用", "A single app")

        // ========== 阶段 B：下拉列表已展开（两个选项同时可见）==========
        // 当"单个应用"与"整个屏幕"同时出现在同一个窗口节点树中，
        // 说明下拉列表一定已经打开。此时直接点击"整个屏幕"。
        if (entireScreenNodes.isNotEmpty() && singleAppNodes.isNotEmpty()) {
            for (node in entireScreenNodes) {
                if (performClick(node)) {
                    AppLogger.i("SparkAccessibility", "[步骤2] 下拉列表已展开，已自动选择「整个屏幕」")
                    return
                }
            }
            // 如果常规点击失败，尝试对每个节点执行 ACTION_SELECT
            for (node in entireScreenNodes) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_SELECT)) {
                    AppLogger.i("SparkAccessibility", "[步骤2] 通过 ACTION_SELECT 选中「整个屏幕」")
                    return
                }
            }
            AppLogger.w("SparkAccessibility", "[步骤2] 找到了「整个屏幕」节点但点击失败，等待下一次事件重试")
            return
        }

        // ========== 以下阶段需要确认属于 SparkAI 投屏授权对话框 ==========
        val isProjectionDialog = isSparkAIProjectionDialog(rootNode)
        if (!isProjectionDialog) return

        // ========== 阶段 C：已选好"整个屏幕"，点击确认按钮 ==========
        if (entireScreenNodes.isNotEmpty() && singleAppNodes.isEmpty()) {
            val confirmTexts = listOf("下一步", "开始", "立即开始", "允许", "确定", "Start now", "Start", "Allow", "OK")
            for (text in confirmTexts) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(text)
                for (node in nodes) {
                    if (node.text?.toString()?.trim() == text && performClick(node)) {
                        AppLogger.i("SparkAccessibility", "[步骤3] 已自动点击确认按钮 [$text]，授权完成")
                        return
                    }
                }
            }
        }

        // ========== 阶段 A：默认显示"单个应用"，点击展开下拉框 ==========
        if (singleAppNodes.isNotEmpty() && entireScreenNodes.isEmpty()) {
            for (node in singleAppNodes) {
                if (performClick(node)) {
                    AppLogger.i("SparkAccessibility", "[步骤1] 已点击「单个应用」下拉框，展开选项列表")
                    return
                }
            }
        }
    }

    /**
     * 判断当前窗口是否为 SparkAI 触发的系统投屏授权弹窗
     */
    private fun isSparkAIProjectionDialog(rootNode: AccessibilityNodeInfo): Boolean {
        val hasSparkAI = rootNode.findAccessibilityNodeInfosByText("SparkAI").isNotEmpty()
        val hasProjectionHint = rootNode.findAccessibilityNodeInfosByText("录制或投放").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByText("录制或投射").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByText("截取屏幕上显示的所有内容").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByText("要开始使用").isNotEmpty()
        return hasSparkAI || hasProjectionHint
    }

    /**
     * 聚合搜索多个候选文本，返回所有匹配节点
     */
    private fun findNodesByTexts(rootNode: AccessibilityNodeInfo, vararg texts: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        for (text in texts) {
            result.addAll(rootNode.findAccessibilityNodeInfosByText(text))
        }
        return result
    }

    /**
     * 递归向上检索可点击的容器或组件并执行点击动作
     */
    private fun performClick(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                val success = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
            current = current.parent
        }
        return false
    }

    override fun onInterrupt() {
        AppLogger.w("SparkAccessibility", "SparkAI 自动化授权无障碍服务被系统意外中断")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppLogger.i("SparkAccessibility", "SparkAI 自动化授权无障碍服务已成功建立连接并正常激活")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        AppLogger.i("SparkAccessibility", "SparkAI 自动化授权无障碍服务已销毁")
    }
}
