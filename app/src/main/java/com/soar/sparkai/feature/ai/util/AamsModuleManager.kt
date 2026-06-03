package com.soar.sparkai.feature.ai.util

import android.content.Context
import com.soar.sparkai.feature.ai.model.AamsModule
import org.json.JSONArray
import org.json.JSONObject
import com.soar.sparkai.core.log.AppLogger

/**
 * AAMS (AI-Assisted Module System) 自定义模块管理器
 *
 * 作用：管理手机端所有 AI 自定义及系统内置模块。
 * 基于 SharedPreferences 提供热装载持久化存储，支持模块加载、启用切换、自定义导入及删除操作。
 */
object AamsModuleManager {

    private const val PREFS_NAME = "aams_module_prefs"
    private const val KEY_MODULES = "aams_modules_list_json"

    // 内置系统默认模块
    private val systemModules = listOf(
        AamsModule(
            id = "sys_sum_numbers",
            name = "🔮 实时屏幕数字求和",
            description = "智能捕获屏幕图像，提取所有价格或数字，计算它们的原价总和，并在屏幕上圈出数字高亮标注。",
            prompt = "请在提供的屏幕图像中，智能识别出所有代表价格、数值或交易额的数字，计算它们的总和。然后，只返回一个合法的 JSON 字符串，其中包含：\n1. total_sum：计算出的总和。\n2. explanation：简短中文说明。\n3. numbers：JSON 数组，其中的每个对象代表屏幕上识别到的一个数字，必须包含以下字段：\n- original_value：识别到的原始数值（如 100.00）\n- calculated_value：相同数值（即 100.00）\n- ymin, xmin, ymax, xmax：该价格/数字在屏幕上的 0-100 相对百分比整数坐标（必须精确识别）。\n\n注意：请勿返回任何 Markdown 代码块包裹（如 ```json），直接返回纯文本 JSON 格式的内容。",
            enabled = true,
            isSystem = true,
            modelConfigId = "sys_mimo_2.5"
        ),
        AamsModule(
            id = "sys_discount_calculator",
            name = "🏷️ 商品八折计算器",
            description = "自动提取屏幕上的所有价格，对所有价格乘以 0.8 进行打折计算，汇总折后价总和并圈出标注。",
            prompt = "请在提供的屏幕图像中，智能识别出所有商品的价格，并对所有识别的价格乘以 0.8 进行打折计算。计算打折后的总金额。然后，只返回一个合法的 JSON 字符串，其中包含：\n1. total_sum：计算出的折后价格总和。\n2. explanation：简短中文优惠说明。\n3. numbers：JSON 数组，其中的每个对象代表屏幕上识别到的一个数字，必须包含以下字段：\n- original_value：识别到的原始价格数值（如 128.00）\n- calculated_value：乘以0.8打八折后的折扣价格数值（如 102.40）\n- ymin, xmin, ymax, xmax：该价格/数字在屏幕上的 0-100 相对百分比整数坐标（必须精确识别，以便我们在屏幕原位划掉原价并直接展示打折新价格）。\n\n注意：请勿返回任何 Markdown 代码块包裹（如 ```json），直接返回纯文本 JSON 格式的内容。",
            enabled = true,
            isSystem = true,
            modelConfigId = "sys_mimo_2.5"
        ),
        AamsModule(
            id = "sys_ar_translation",
            name = "🌍 屏幕原位 AI 翻译",
            description = "自动提取屏幕中所有的英文字母与单词，翻译为中文，并直接原汁原味覆盖在屏幕原本的英文正上方！",
            prompt = "请在提供的屏幕图像中，智能识别出所有代表英文单词、短语或长句的英文字母。只返回一个合法的 JSON 字符串，其中包含：\n1. total_sum：这里设为 0.0 即可。\n2. explanation：简短中文说明。\n3. numbers：JSON 数组，其中的每个对象代表屏幕上识别到的一个英文文本块，必须包含以下字段：\n- original_value：识别到的原始英文内容（如 'Open Settings'）\n- calculated_value：翻译后的对应中文内容（如 '打开设置'）\n- ymin, xmin, ymax, xmax：该英文文本块在屏幕上的 0-100 相对百分比整数坐标（必须极其精确识别，以便我们在屏幕原位完美遮罩并覆盖展示翻译后的中文）。\n\n注意：请勿返回任何 Markdown 代码块包裹（如 ```json），直接返回纯文本 JSON 格式的内容。",
            enabled = true,
            isSystem = true,
            modelConfigId = "sys_mimo_2.5"
        ),
        AamsModule(
            id = "sys_alignment_tester",
            name = "🎯 无障碍原位对齐测试器",
            description = "测试无障碍物理定位的精准度！您可在弹窗中输入要查找的文字，系统将通过无障碍节点树在屏幕原位精确框选高亮。",
            prompt = "alignment_tester_no_ai_required",
            enabled = true,
            isSystem = true,
            modelConfigId = "sys_mimo_2.5"
        )
    )

    /**
     * 获取所有已加载的模块列表（合并内置系统模块与用户自定义模块）
     */
    fun getAllModules(context: Context): List<AamsModule> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_MODULES, null)
        
        if (jsonStr.isNullOrEmpty()) {
            // 初次初始化，写入默认系统模块
            saveModulesToPrefs(context, systemModules)
            return systemModules
        }

        return try {
            val list = mutableListOf<AamsModule>()
            val arr = JSONArray(jsonStr)
            
            // 1. 读取持久化的模块
            for (i in 0 until arr.length()) {
                list.add(AamsModule.fromJson(arr.getString(i)))
            }
            
            // 2. 双重确认：如果系统模块在持久化中丢失，自动补齐
            for (sys in systemModules) {
                if (list.none { it.id == sys.id }) {
                    list.add(sys)
                }
            }
            list
        } catch (e: Exception) {
            AppLogger.e("AamsModuleManager", "解析模块持久化数据失败: ${e.message}", e)
            systemModules
        }
    }

    /**
     * 保存单个模块（新建/更新自定义模块，或者更新系统模块的 enabled 状态）
     */
    fun saveModule(context: Context, module: AamsModule) {
        val currentList = getAllModules(context).toMutableList()
        val index = currentList.indexOfFirst { it.id == module.id }
        
        if (index >= 0) {
            currentList[index] = module
        } else {
            currentList.add(module)
        }
        
        saveModulesToPrefs(context, currentList)
        AppLogger.i("AamsModuleManager", "已成功保存并同步模块持久化状态: ${module.name} (ID: ${module.id})")
    }

    /**
     * 删除指定自定义模块（不允许删除系统内置模块）
     */
    fun deleteModule(context: Context, moduleId: String): Boolean {
        val currentList = getAllModules(context).toMutableList()
        val module = currentList.find { it.id == moduleId }
        
        if (module != null && !module.isSystem) {
            currentList.remove(module)
            saveModulesToPrefs(context, currentList)
            AppLogger.i("AamsModuleManager", "已成功移除自定义模块: ${module.name} (ID: $moduleId)")
            return true
        }
        
        AppLogger.w("AamsModuleManager", "拒绝删除模块: ID 不存在或为系统内置模块 (ID: $moduleId)")
        return false
    }

    /**
     * 快速一键切换模块启用/禁用状态
     */
    fun toggleModuleEnabled(context: Context, moduleId: String, enabled: Boolean) {
        val currentList = getAllModules(context)
        val module = currentList.find { it.id == moduleId }
        
        if (module != null) {
            val updated = module.copy(enabled = enabled)
            saveModule(context, updated)
        }
    }

    /**
     * 私有方法：将模块列表序列化保存至 SharedPreferences
     */
    private fun saveModulesToPrefs(context: Context, list: List<AamsModule>) {
        try {
            val arr = JSONArray()
            for (m in list) {
                arr.put(m.toJson())
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MODULES, arr.toString())
                .apply()
        } catch (e: Exception) {
            AppLogger.e("AamsModuleManager", "序列化保存模块数据发生异常: ${e.message}", e)
        }
    }
}
