<template>
  <div class="h-screen w-screen aurora-bg flex flex-col text-brand-text overflow-hidden select-none">
    <!-- 顶部自定义标题栏 (支持窗口拖动) -->
    <header class="h-11 border-b border-brand-border glass-panel flex items-center justify-between px-4 drag-region shrink-0 z-50">
      <div class="flex items-center space-x-2">
        <!-- 科技感 LOGO 图标 -->
        <svg class="w-5 h-5 text-brand-secondary neon-text-cyan animate-pulse" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 21l8.982-11.725h-5.228l.836-5.092L4.5 15.904h5.313z" />
        </svg>
        <span class="font-bold tracking-wider text-sm bg-gradient-to-r from-white to-brand-textMuted bg-clip-text text-transparent">SparkAI</span>
        <span class="text-[10px] px-1.5 py-0.5 rounded bg-brand-primary/20 border border-brand-primary/30 text-brand-primary font-semibold tracking-widest scale-90">DESKTOP</span>
      </div>

      <!-- 标题栏右侧控制按钮 (禁用拖动) -->
      <div class="flex items-center space-x-1 no-drag-region h-full">
        <button @click="minimize" class="w-8 h-8 rounded-md flex items-center justify-center text-brand-textMuted hover:text-white hover:bg-white/10 transition-colors">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M18 12H6" /></svg>
        </button>
        <button @click="maximize" class="w-8 h-8 rounded-md flex items-center justify-center text-brand-textMuted hover:text-white hover:bg-white/10 transition-colors">
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><rect x="4" y="4" width="16" height="16" rx="2" /></svg>
        </button>
        <button @click="close" class="w-8 h-8 rounded-md flex items-center justify-center text-brand-textMuted hover:text-white hover:bg-red-500/20 hover:text-red-400 transition-colors">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
        </button>
      </div>
    </header>

    <!-- 下部主体区 -->
    <div class="flex-1 flex overflow-hidden">
      <!-- 左边栏 (导航菜单) -->
      <aside class="w-60 border-r border-brand-border glass-panel p-4 flex flex-col justify-between shrink-0">
        <div class="space-y-6">
          <div class="px-2 text-xs font-semibold tracking-wider text-brand-textMuted uppercase">核心控制面板</div>
          <nav class="space-y-1.5">
            <a v-for="(item, idx) in menuItems" :key="idx" href="#" @click="currentTab = idx"
               :class="[currentTab === idx ? 'bg-gradient-to-r from-brand-primary/20 to-brand-secondary/5 border-l-2 border-brand-secondary text-brand-text' : 'border-l-2 border-transparent text-brand-textMuted hover:text-brand-text hover:bg-white/5']"
               class="flex items-center space-x-3 px-3 py-2.5 rounded-r-md text-sm transition-all duration-200">
              <component :is="item.icon" class="w-4 h-4" />
              <span>{{ item.name }}</span>
            </a>
          </nav>
        </div>

        <!-- 边栏底部运行状态 -->
        <div class="p-3.5 rounded-lg border border-brand-border bg-white/[0.02] flex items-center justify-between">
          <div class="flex items-center space-x-2.5">
            <span class="w-2.5 h-2.5 rounded-full bg-brand-secondary dot-pulse shadow-glow-cyan shrink-0"></span>
            <span class="text-xs font-medium tracking-wide">核心引擎运行中</span>
          </div>
          <span class="text-[10px] text-brand-textMuted">v1.0.0</span>
        </div>
      </aside>

      <!-- 右侧主内容区 -->
      <main class="flex-1 p-6 overflow-y-auto flex flex-col space-y-6">
        <!-- Tab 0: 引擎概览 -->
        <template v-if="currentTab === 0">
          <!-- 头部渐变欢迎卡片 -->
          <section class="p-6 rounded-2xl border border-brand-border bg-gradient-to-br from-brand-primary/10 via-brand-secondary/5 to-transparent relative overflow-hidden shrink-0">
            <div class="relative z-10 space-y-2">
              <h2 class="text-xl font-bold tracking-tight bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">欢迎使用 SparkAI 后台引擎</h2>
              <p class="text-xs text-brand-textMuted max-w-xl leading-relaxed">当前为 Windows 桌面端空白运行环境。此模块将作为系统后台任务、数据集成与本地代理的核心处理节点。</p>
            </div>
            <div class="absolute -right-10 -bottom-10 w-40 h-40 bg-brand-secondary/10 rounded-full blur-3xl"></div>
          </section>

          <!-- 三栏监控卡片 -->
          <section class="grid grid-cols-3 gap-6 shrink-0">
            <div v-for="(stat, idx) in systemStats" :key="idx" class="p-5 rounded-2xl glass-panel-interactive flex flex-col space-y-3.5">
              <div class="flex items-center justify-between">
                <span class="text-xs font-semibold text-brand-textMuted tracking-wider">{{ stat.title }}</span>
                <component :is="stat.icon" :class="stat.iconColor" class="w-4 h-4" />
              </div>
              <div class="flex items-baseline space-x-1.5">
                <span class="text-2xl font-bold font-mono tracking-tight">{{ stat.value }}</span>
                <span class="text-xs text-brand-textMuted">{{ stat.unit }}</span>
              </div>
              <div class="w-full bg-white/5 h-1 rounded-full overflow-hidden">
                <div :style="{ width: stat.percentage + '%' }" :class="stat.barColor" class="h-full transition-all duration-500"></div>
              </div>
            </div>
          </section>

          <!-- 局域网极速互传桥接卡片 -->
          <section class="grid grid-cols-3 gap-6 shrink-0">
            <div class="col-span-2 p-5 rounded-2xl glass-panel flex flex-col justify-between border border-brand-border min-h-[170px] relative overflow-hidden">
              <div class="relative z-10">
                <div class="flex items-center space-x-2.5">
                  <span :class="[serverRunning ? 'bg-emerald-400 dot-pulse shadow-glow-cyan' : 'bg-red-400']" class="w-2.5 h-2.5 rounded-full shrink-0"></span>
                  <span class="text-xs font-semibold tracking-wider text-brand-textMuted uppercase">极速互传局域网服务</span>
                </div>
                <h3 class="text-lg font-bold text-white mt-3.5">
                  {{ serverRunning ? '局域网互传桥接引擎已就绪' : '正在启动局域网引擎...' }}
                </h3>
                <p class="text-xs text-brand-textMuted mt-1 leading-relaxed">
                  手机端可扫描右侧二维码一键桥接，或进入「局域网高速互传中心」连接以下任一本地 IP：
                </p>
                <div class="flex flex-wrap gap-2 mt-3.5">
                  <span v-for="ip in ips" :key="ip" @click="copyText(`http://${ip}:${port}`)" 
                        class="text-[10px] font-mono px-2.5 py-1.5 rounded bg-white/5 border border-white/10 text-brand-secondary hover:bg-brand-secondary/15 cursor-pointer transition-colors">
                    {{ ip }}:{{ port }} (点击复制)
                  </span>
                  <span v-if="ips.length === 0" class="text-xs text-brand-textMuted font-mono">未发现有效网络网卡</span>
                </div>
              </div>
              <div class="absolute -right-10 -bottom-10 w-32 h-32 bg-brand-primary/5 rounded-full blur-3xl"></div>
            </div>

            <!-- 极客发光扫码卡片 -->
            <div class="p-5 rounded-2xl glass-panel flex flex-col items-center justify-center border border-brand-border min-h-[170px] relative overflow-hidden">
              <div class="relative z-10 flex flex-col items-center">
                <canvas ref="overviewQrCanvas" class="w-24 h-24 bg-white/5 rounded-lg p-1 shadow-glow-cyan"></canvas>
                <!-- 智能网卡选择器 -->
                <select v-if="ips.length > 1" v-model="selectedIp" @change="loadOverviewQRCode"
                        class="text-[9px] bg-black/60 border border-white/10 rounded px-1.5 py-0.5 mt-2 text-brand-secondary outline-none cursor-pointer hover:bg-black/80 transition-colors">
                  <option v-for="ip in ips" :key="ip" :value="ip">{{ ip }}</option>
                </select>
                <span class="text-[10px] text-brand-textMuted mt-2">手机扫码一键连接</span>
              </div>
              <div class="absolute -left-10 -bottom-10 w-24 h-24 bg-brand-secondary/5 rounded-full blur-3xl"></div>
            </div>
          </section>

          <!-- 虚拟活动日志监控 (实时滚动) -->
          <section class="flex-1 min-h-[220px] rounded-2xl glass-panel p-5 flex flex-col overflow-hidden border border-brand-border">
            <div class="flex items-center justify-between pb-3.5 border-b border-brand-border shrink-0">
              <div class="flex items-center space-x-2">
                <span class="text-xs font-bold tracking-wider uppercase text-brand-secondary">后台引擎实时日志</span>
                <span class="text-[10px] px-1.5 py-0.5 rounded bg-brand-secondary/10 border border-brand-secondary/20 text-brand-secondary font-mono">STREAMING</span>
              </div>
              <button @click="clearLogs" class="text-xs text-brand-textMuted hover:text-white transition-colors">清除日志</button>
            </div>
            <div ref="logContainer" class="flex-1 overflow-y-auto font-mono text-[11px] space-y-1.5 p-3.5 bg-black/40 rounded-lg border border-brand-border mt-3.5">
              <div v-for="(log, idx) in logs" :key="idx" class="flex space-x-3.5 leading-relaxed">
                <span class="text-brand-secondary/70 select-none">[{{ log.time }}]</span>
                <span :class="log.levelClass" class="font-bold select-none shrink-0">{{ log.level }}</span>
                <span class="text-gray-300 break-all">{{ log.message }}</span>
              </div>
            </div>
          </section>
        </template>

        <!-- Tab 1: 局域网互传 -->
        <template v-else-if="currentTab === 1">
          <FileTransfer :serverRunning="serverRunning" :ips="ips" :port="port" />
        </template>

        <!-- Tab 2: 参数配置 -->
        <template v-else-if="currentTab === 2">
          <div class="p-6 rounded-2xl glass-panel border border-brand-border space-y-4">
            <h3 class="text-base font-bold text-white">⚙ 通信与代理参数配置</h3>
            <p class="text-xs text-brand-textMuted">默认局域网握手端口为 19090。如遇端口冲突，系统将在后台自动递增匹配可用通信通道，无需手动修改任何参数。</p>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, shallowRef } from 'vue'
import FileTransfer from '@/components/FileTransfer.vue'
import QRCode from 'qrcode'

// SVG 图标组件，使用 shallowRef 提升渲染性能
const HomeIcon = shallowRef({
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></svg>`
})
const SettingsIcon = shallowRef({
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`
})
const CpuIcon = shallowRef({
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/><path d="M9 1v3M15 1v3M9 20v3M15 20v3M20 9h3M20 15h3M1 9h3M1 15h3"/></svg>`
})
const DatabaseIcon = shallowRef({
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/><path d="M3 12c0 1.66 4 3 9 3s9-1.34 9-3"/></svg>`
})
const NetworkIcon = shallowRef({
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.55a11 11 0 0 1 14.08 0"/><path d="M1.42 9a16 16 0 0 1 21.16 0"/><path d="M8.53 16.11a6 6 0 0 1 6.95 0"/><circle cx="12" cy="20" r="1"/></svg>`
})
const ShareIcon = shallowRef({
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/></svg>`
})

const serverRunning = ref(false)
const ips = ref<string[]>([])
const selectedIp = ref('')
const port = ref(19090)
const overviewQrCanvas = ref<HTMLCanvasElement | null>(null)

const loadOverviewQRCode = () => {
  nextTick(() => {
    if (overviewQrCanvas.value && selectedIp.value) {
      const connectUrl = `http://${selectedIp.value}:${port.value}`
      QRCode.toCanvas(overviewQrCanvas.value, connectUrl, {
        width: 96,
        margin: 1,
        color: { dark: '#ffffff', light: '#1e1e2f00' }
      })
    }
  })
}

const copyText = (text: string) => {
  navigator.clipboard.writeText(text)
  addLog('SUCCESS', `已复制连接地址到剪贴板: ${text}`)
}

const currentTab = ref(0)
const logContainer = ref<HTMLElement | null>(null)

// 菜单定义
const menuItems = [
  { name: '引擎概览', icon: HomeIcon },
  { name: '局域网互传', icon: ShareIcon },
  { name: '参数配置', icon: SettingsIcon }
]

// 监控数据定义
const systemStats = ref([
  { title: 'CPU 负载', value: '12.4', unit: '%', percentage: 12.4, icon: CpuIcon, iconColor: 'text-brand-secondary shadow-glow-cyan', barColor: 'bg-brand-secondary', base: 12 },
  { title: '内存占用', value: '42.8', unit: '%', percentage: 42.8, icon: DatabaseIcon, iconColor: 'text-brand-primary shadow-glow-purple', barColor: 'bg-brand-primary', base: 42 },
  { title: '通信延迟', value: '18', unit: 'ms', percentage: 24, icon: NetworkIcon, iconColor: 'text-brand-accent', barColor: 'bg-brand-accent', base: 18 }
])

// 虚拟日志
interface LogEntry {
  time: string
  level: string
  levelClass: string
  message: string
}
const logs = ref<LogEntry[]>([
  { time: new Date().toLocaleTimeString(), level: 'INFO', levelClass: 'text-brand-secondary', message: 'SparkAI 桌面引擎客户端已成功初始化' },
  { time: new Date().toLocaleTimeString(), level: 'INFO', levelClass: 'text-brand-secondary', message: '正在加载本地通信网桥模块...' },
  { time: new Date().toLocaleTimeString(), level: 'SUCCESS', levelClass: 'text-emerald-400', message: '核心服务通道监听中，端口: 3000' }
])

// 窗口控制逻辑
const minimize = () => window.electronAPI?.minimize()
const maximize = () => window.electronAPI?.maximize()
const close = () => window.electronAPI?.close()

// 日志操作
const clearLogs = () => { logs.value = [] }
const addLog = (level: 'INFO' | 'SUCCESS' | 'WARN', message: string) => {
  const levelClass = level === 'SUCCESS' ? 'text-emerald-400' : level === 'WARN' ? 'text-amber-400' : 'text-brand-secondary'
  logs.value.push({
    time: new Date().toLocaleTimeString(),
    level,
    levelClass,
    message
  })
  if (logs.value.length > 50) logs.value.shift()
  nextTick(() => {
    if (logContainer.value) logContainer.value.scrollTop = logContainer.value.scrollHeight
  })
}

// 模拟动态数据刷新
let updateTimer: number | null = null
const simulateData = () => {
  systemStats.value.forEach(stat => {
    const change = (Math.random() - 0.5) * 4
    let newValue = parseFloat(stat.value) + change
    if (stat.title === '通信延迟') {
      newValue = Math.max(8, Math.min(60, Math.round(newValue)))
      stat.percentage = (newValue / 60) * 100
    } else {
      newValue = Math.max(2, Math.min(95, parseFloat(newValue.toFixed(1))))
      stat.percentage = newValue
    }
    stat.value = newValue.toString()
  })

  // 偶尔追加一条日志
  if (Math.random() > 0.75) {
    const messages = [
      { l: 'INFO', m: '同步本地数据批次: #' + Math.floor(Math.random() * 1000) },
      { l: 'SUCCESS', m: '与 Android 前端主通道握手成功，心跳正常' },
      { l: 'INFO', m: '清理过期缓存碎片完成，释放内存 2.4MB' },
      { l: 'WARN', m: '发现未知连接请求，已自动通过防火墙拦截规则' }
    ]
    const pick = messages[Math.floor(Math.random() * messages.length)]
    addLog(pick.l as any, pick.m)
  }
}

onMounted(async () => {
  updateTimer = window.setInterval(simulateData, 3000)
  addLog('INFO', '数据模拟刷新与心跳服务已上线')

  if (window.electronAPI) {
    addLog('INFO', '正在拉起局域网极速互传服务器...')
    const res = await window.electronAPI.startFileServer()
    if (res?.success) {
      serverRunning.value = true
      ips.value = res.ips
      selectedIp.value = res.ips[0] || ''
      port.value = res.port
      addLog('SUCCESS', `局域网极速互传服务器启动成功！端口: ${res.port}`)
      loadOverviewQRCode()
    } else {
      addLog('WARN', `局域网极速互传服务器启动失败: ${res?.error || '未知原因'}`)
    }

    window.electronAPI.onServerLog((log: any) => {
      if (log && log.level && log.message) {
        addLog(log.level, log.message)
      }
    })
  }
})

onUnmounted(() => {
  if (updateTimer) clearInterval(updateTimer)
})
</script>
