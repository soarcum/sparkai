import { ref, reactive, watch, nextTick } from 'vue'
import { AAMSModule, AAMSLog, AIScriptSDK } from './types'
import { createSDKInstance } from './sdk'
import { loadAndRunModule, unloadModule } from './executor'

// ==========================================
// 1. 全局大模型 API 核心配置管理 (持久化至 localStorage)
// ==========================================
export const aiConfig = reactive({
  apiKey: localStorage.getItem('aams_ai_api_key') || '',
  baseUrl: localStorage.getItem('aams_ai_base_url') || 'https://token-plan-cn.xiaomimimo.com/v1',
  model: localStorage.getItem('aams_ai_model') || 'mimo-v2.5-pro'
})

// 监听大模型配置，一有变动自动保存
watch(aiConfig, (newVal) => {
  localStorage.setItem('aams_ai_api_key', newVal.apiKey)
  localStorage.setItem('aams_ai_base_url', newVal.baseUrl)
  localStorage.setItem('aams_ai_model', newVal.model)
}, { deep: true })


// ==========================================
// 2. 模块列表的响应式状态与持久化操作
// ==========================================
export const modulesList = ref<AAMSModule[]>([])

// 保存模块列表到本地存储中
export function saveModules() {
  localStorage.setItem('aams_saved_modules', JSON.stringify(modulesList.value))
}

// 追加日志函数
export function addModuleLog(
  moduleId: string, 
  level: 'info' | 'success' | 'warn' | 'error', 
  message: string
) {
  const mod = modulesList.value.find(m => m.id === moduleId)
  if (mod) {
    if (!mod.logs) mod.logs = []
    mod.logs.push({
      time: new Date().toLocaleTimeString(),
      level,
      message
    })
    // 限制日志条数
    if (mod.logs.length > 80) {
      mod.logs.shift()
    }
    saveModules()
    
    // 抛出全局日志更新事件，方便 UI 组件在日志滚动时做自动触底
    window.dispatchEvent(new CustomEvent('aams-log-updated', { detail: { moduleId } }))
  }
}

// 清空特定模块日志
export function clearModuleLogs(moduleId: string) {
  const mod = modulesList.value.find(m => m.id === moduleId)
  if (mod) {
    mod.logs = []
    saveModules()
  }
}

// ==========================================
// 3. 核心运行与停用逻辑
// ==========================================

// 存储各个活跃模块对应的 SDK 实例，方便后续闭包调用
const sdkInstances = new Map<string, AIScriptSDK>();

/**
 * 运行指定的模块
 */
export function runModule(moduleId: string): boolean {
  const mod = modulesList.value.find(m => m.id === moduleId)
  if (!mod) return false

  addModuleLog(moduleId, 'info', '⚡ 正在准备加载运行环境...')

  // 1. 创建特有的 SDK 实例
  const sdk = createSDKInstance(
    moduleId,
    (level, msg) => addModuleLog(moduleId, level, msg),
    () => ({
      apiKey: aiConfig.apiKey,
      baseUrl: aiConfig.baseUrl,
      model: aiConfig.model
    })
  )
  sdkInstances.set(moduleId, sdk)

  // 2. 编译并加载执行
  const success = loadAndRunModule(
    moduleId,
    mod.code,
    sdk,
    (level, msg) => addModuleLog(moduleId, level, msg)
  )

  if (!success) {
    // 运行失败，重置状态
    mod.enabled = false
    saveModules()
  }

  return success
}

/**
 * 停用指定的模块
 */
export function stopModule(moduleId: string) {
  const mod = modulesList.value.find(m => m.id === moduleId)
  if (!mod) return

  addModuleLog(moduleId, 'info', '⏹ 正在停用该模块，卸载相关资源...')
  
  const sdk = sdkInstances.get(moduleId)
  if (sdk) {
    unloadModule(moduleId, sdk, (level, msg) => addModuleLog(moduleId, level, msg))
    sdkInstances.delete(moduleId)
  } else {
    // 兜底一键清理 DOM
    try {
      const elementsToCleanup = document.querySelectorAll(`[data-aams-id="${moduleId}"]`)
      elementsToCleanup.forEach(el => el.remove())
    } catch {}
    addModuleLog(moduleId, 'success', '❌ 模块已强制停用 (资源已回收)')
  }
}

/**
 * 切换启用/禁用状态
 */
export function toggleModule(moduleId: string) {
  const mod = modulesList.value.find(m => m.id === moduleId)
  if (!mod) return

  mod.enabled = !mod.enabled
  saveModules()

  if (mod.enabled) {
    runModule(mod.id)
  } else {
    stopModule(mod.id)
  }
}

/**
 * 添加一个新的 AI 生成模块
 */
export function addModule(name: string, description: string, prompt: string, code: string): AAMSModule {
  const newMod: AAMSModule = {
    id: 'm_' + Math.random().toString(36).substring(2, 11),
    name,
    description,
    prompt,
    code,
    enabled: false,
    logs: []
  }
  
  modulesList.value.push(newMod)
  saveModules()
  
  addModuleLog(newMod.id, 'success', `模块 [${name}] 创建成功！您现在可以点击开启开关使其挂载生效。`)
  return newMod
}

/**
 * 保存修改的代码并重新运行
 */
export function updateModuleCode(moduleId: string, newCode: string) {
  const mod = modulesList.value.find(m => m.id === moduleId)
  if (!mod) return

  mod.code = newCode
  saveModules()
  
  addModuleLog(moduleId, 'info', '📝 检测到模块代码已发生更改，正在进行重新编译加载...')

  // 如果原本处于启用状态，则热重启
  if (mod.enabled) {
    stopModule(moduleId)
    runModule(moduleId)
  }
}

/**
 * 删除模块
 */
export function deleteModule(moduleId: string) {
  const idx = modulesList.value.findIndex(m => m.id === moduleId)
  if (idx !== -1) {
    const mod = modulesList.value[idx]
    if (mod.enabled) {
      stopModule(moduleId)
    }
    modulesList.value.splice(idx, 1)
    saveModules()
  }
}

/**
 * 初始化加载全部模块，并自动激活以前启用的模块 (可用于应用冷启动)
 */
export function initModules() {
  const data = localStorage.getItem('aams_saved_modules')
  if (data) {
    try {
      modulesList.value = JSON.parse(data)
      // 在 DOM 渲染就绪后，自动挂载激活所有处于 enabled === true 的模块
      nextTick(() => {
        modulesList.value.forEach(mod => {
          if (mod.enabled) {
            // 重置一下日志并启动
            mod.logs = []
            runModule(mod.id)
          }
        })
      })
    } catch {
      modulesList.value = []
    }
  }
}
