<template>
  <div class="flex-1 flex flex-col space-y-6 overflow-y-auto">
    <!-- 头部连接卡片 -->
    <div class="grid grid-cols-3 gap-6">
      <!-- 局域网服务状态 -->
      <div class="col-span-2 p-5 rounded-2xl glass-panel flex flex-col justify-between border border-brand-border min-h-[170px]">
        <div>
          <div class="flex items-center space-x-2.5">
            <span :class="[serverRunning ? 'bg-emerald-400 dot-pulse shadow-glow-cyan' : 'bg-red-400']" class="w-2.5 h-2.5 rounded-full shrink-0"></span>
            <span class="text-xs font-semibold tracking-wider text-brand-textMuted uppercase">互传引擎服务</span>
          </div>
          <h3 class="text-lg font-bold text-white mt-3.5">
            {{ serverRunning ? '局域网传输通道已成功建立' : '正在启动通信引擎...' }}
          </h3>
          <p class="text-xs text-brand-textMuted mt-1 leading-relaxed">
            手机端请进入「局域网高速互传中心」并连接以下任一局域网 IP 即可：
          </p>
          <div class="flex flex-wrap gap-2 mt-3">
            <span v-for="ip in ips" :key="ip" @click="copyText(`http://${ip}:${port}`)" 
                  class="text-[11px] font-mono px-2.5 py-1 rounded bg-white/5 border border-white/10 text-brand-secondary hover:bg-brand-secondary/15 cursor-pointer transition-colors">
              {{ ip }}:{{ port }} (点击复制)
            </span>
          </div>
        </div>
        <button @click="openSaveDir" class="text-xs text-brand-secondary hover:text-white flex items-center space-x-1.5 transition-colors self-start mt-2">
          <span>📂 打开电脑接收文件夹</span>
        </button>
      </div>

      <!-- 扫码连接 -->
      <div class="p-5 rounded-2xl glass-panel flex flex-col items-center justify-center border border-brand-border min-h-[170px]">
        <canvas ref="qrCanvas" class="w-24 h-24 bg-white/5 rounded-lg p-1"></canvas>
        <span class="text-[10px] text-brand-textMuted mt-2.5">手机扫码一键握手连接</span>
      </div>
    </div>

    <!-- 拖拽发送区 -->
    <div @dragover.prevent="dragOver = true" @dragleave="dragOver = false" @drop.prevent="handleDrop"
         :class="[dragOver ? 'border-brand-secondary bg-brand-secondary/5' : 'border-brand-border hover:bg-white/[0.01]']"
         class="h-32 rounded-2xl border border-dashed flex flex-col items-center justify-center cursor-pointer transition-all duration-200"
         @click="selectAndSendFile">
      <svg class="w-8 h-8 text-brand-secondary mb-2 animate-bounce" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
      </svg>
      <span class="text-xs font-semibold text-brand-text">点击选择 或 拖拽电脑文件至此处发送给手机</span>
      <span class="text-[10px] text-brand-textMuted mt-1">支持任意格式大文件，局域网点对点高速直传</span>
    </div>

    <!-- 传输进度与列表 -->
    <div class="flex-1 rounded-2xl glass-panel p-5 flex flex-col border border-brand-border min-h-[220px]">
      <div class="flex items-center justify-between pb-3.5 border-b border-brand-border">
        <span class="text-xs font-bold tracking-wider uppercase text-brand-secondary">高速互传记录</span>
        <button @click="clearHistory" class="text-xs text-brand-textMuted hover:text-white transition-colors">清除记录</button>
      </div>

      <div class="flex-1 overflow-y-auto mt-4 space-y-3">
        <div v-for="item in transferList" :key="item.name + item.time" class="p-3.5 rounded-lg border border-brand-border bg-white/[0.01] flex flex-col space-y-2">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <span :class="[item.type === 'receive' ? 'text-emerald-400 bg-emerald-400/10' : 'text-brand-secondary bg-brand-secondary/10']" 
                    class="text-[10px] px-2 py-0.5 rounded font-bold uppercase shrink-0">
                {{ item.type === 'receive' ? '接收' : '发送' }}
              </span>
              <span class="text-xs font-semibold text-gray-200 truncate max-w-[280px]">{{ item.name }}</span>
            </div>
            <span class="text-xs font-bold font-mono" :class="[item.status === '完成' ? 'text-emerald-400' : 'text-brand-secondary']">
              {{ item.status }}
            </span>
          </div>

          <div v-if="item.status !== '完成' && item.progress !== undefined" class="w-full bg-white/5 h-1.5 rounded-full overflow-hidden flex items-center">
            <div :style="{ width: item.progress + '%' }" :class="[item.type === 'receive' ? 'bg-emerald-400' : 'bg-brand-secondary']" class="h-full transition-all duration-100"></div>
          </div>

          <div class="flex items-center justify-between text-[10px] text-brand-textMuted">
            <span>{{ (item.size / 1024 / 1024).toFixed(2) }} MB | {{ item.time }}</span>
            <span>{{ item.progress ? Math.round(item.progress) + '%' : '' }}</span>
          </div>
        </div>

        <div v-if="transferList.length === 0" class="h-full flex flex-col items-center justify-center text-brand-textMuted py-8">
          <span class="text-xs">暂无局域网传输记录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const serverRunning = ref(false)
const ips = ref<string[]>([])
const port = ref(9090)
const dragOver = ref(false)
const qrCanvas = ref<HTMLCanvasElement | null>(null)

interface TransferItem {
  name: string
  size: number
  type: 'send' | 'receive'
  status: string
  progress: number
  time: string
}
const transferList = ref<TransferItem[]>([])

const copyText = (text: string) => {
  navigator.clipboard.writeText(text)
}

const openSaveDir = () => window.electronAPI?.openSaveDir()

const selectAndSendFile = async () => {
  const res = await window.electronAPI?.selectAndSendFile()
  if (res?.success) {
    transferList.value.unshift({
      name: res.filename,
      size: res.size,
      type: 'send',
      status: '等待手机确认...',
      progress: 0,
      time: new Date().toLocaleTimeString()
    })
  }
}

const handleDrop = async (e: DragEvent) => {
  dragOver.value = false
  const file = e.dataTransfer?.files[0]
  if (file && window.electronAPI) {
    // 拖拽逻辑支持
    const res = await window.electronAPI?.selectAndSendFile() // 也可以由 preload 暴露直接物理文件投递，这里采用标准dialog规避沙盒物理路径权限
  }
}

const clearHistory = () => { transferList.value = [] }

const loadQRCode = () => {
  const script = document.createElement('script')
  script.src = new URL('../assets/qrcode.min.js', import.meta.url).href
  script.onload = () => {
    if (qrCanvas.value && ips.value.length > 0 && (window as any).QRCode) {
      const connectUrl = `http://${ips.value[0]}:${port.value}`;
      (window as any).QRCode.toCanvas(qrCanvas.value, connectUrl, { width: 96, margin: 1, color: { dark: '#ffffff', light: '#1e1e2f00' } })
    }
  }
  document.head.appendChild(script)
}

onMounted(async () => {
  if (window.electronAPI) {
    const res = await window.electronAPI.startFileServer()
    if (res?.success) {
      serverRunning.value = true
      ips.value = res.ips
      port.value = res.port
      loadQRCode()
    }

    window.electronAPI.onServerLog((log: any) => {
      // 捕获日志，如有必要在界面输出
    })

    window.electronAPI.onTransferProgress((data: any) => {
      const item = transferList.value.find(t => t.name === data.filename)
      if (item) {
        item.progress = data.progress
        item.status = `${Math.round(data.progress)}%`
        if (data.progress === 100) item.status = '完成'
      }
    })

    window.electronAPI.onFileReceived((file: any) => {
      transferList.value.unshift({
        name: file.name,
        size: file.size,
        type: 'receive',
        status: '完成',
        progress: 100,
        time: file.time
      })
    })
  }
})
</script>
