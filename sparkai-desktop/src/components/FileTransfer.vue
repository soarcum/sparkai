<template>
  <div class="flex-1 flex flex-col space-y-6 overflow-y-auto pr-1.5 scrollbar-thin scroll-smooth">
    <!-- 头部连接卡片 -->
    <div id="transfer-connection" class="grid grid-cols-3 gap-6 shrink-0">
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

    <!-- 麦克风投音接收卡片 -->
    <div id="transfer-mic" class="p-5 rounded-2xl glass-panel border border-brand-border flex flex-col space-y-4 relative overflow-hidden transition-all duration-300 shrink-0"
         :class="[isAudioStreaming ? 'border-brand-primary/40 bg-brand-primary/[0.02]' : '']">
      
      <!-- 酷炫背景渐变光环，只有播放时有流光 -->
      <div v-if="isAudioStreaming" class="absolute -right-16 -top-16 w-44 h-44 rounded-full bg-brand-primary/10 blur-3xl animate-pulse"></div>

      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-2.5">
          <span :class="[isAudioStreaming ? 'bg-brand-primary dot-pulse shadow-glow-purple' : 'bg-white/20']" 
                class="w-2.5 h-2.5 rounded-full shrink-0"></span>
          <span class="text-xs font-semibold tracking-wider text-brand-textMuted uppercase">
            {{ isAudioStreaming ? '手机无线麦克风实时音频流已接入' : '无线麦克风极速投音接收器' }}
          </span>
        </div>
        <div v-if="isAudioStreaming" class="flex items-center space-x-4">
          <span class="text-[11px] font-mono text-brand-textMuted">已接收: {{(streamByteCount / 1024).toFixed(1)}} KB</span>
          <span class="text-[11px] font-mono px-2 py-0.5 rounded bg-brand-primary/20 text-brand-primary font-bold">⏱ {{ audioStreamTime }}</span>
        </div>
      </div>

      <div class="flex flex-col md:flex-row md:items-center justify-between gap-6 pt-1">
        <!-- 麦克风状态与播放控制 -->
        <div class="flex items-center space-x-4">
          <div class="w-12 h-12 rounded-xl flex items-center justify-center transition-all shrink-0"
               :class="[isAudioStreaming ? 'bg-gradient-to-tr from-brand-primary to-brand-primary/60 text-white shadow-glow-purple animate-pulse' : 'bg-white/5 text-brand-textMuted']">
            <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
            </svg>
          </div>
          <div>
            <h4 class="text-sm font-bold text-white">
              {{ isAudioStreaming ? '电脑扬声器正实时转播声音...' : '等待手机端启动麦克风投送' }}
            </h4>
            <p class="text-xs text-brand-textMuted mt-1 leading-relaxed">
              {{ isAudioStreaming ? '对准手机端（无线麦克风）说话，声音正极低延迟输送至电脑。可随意控制静音或放大监听。' : '在手机 App 配对连接电脑后，开启「无线麦克风极速投音」即可让电脑听到手机接收的声音。' }}
            </p>
          </div>
        </div>

        <!-- 音频流可视化与增益控制 -->
        <div v-if="isAudioStreaming" class="flex items-center space-x-6 shrink-0 self-end md:self-center">
          <!-- 酷炫的 8 根声波柱跳动 -->
          <div class="flex items-end space-x-1.5 h-10 px-4 py-1.5 bg-black/40 rounded-xl border border-white/5">
            <div v-for="i in 8" :key="i"
                 :style="{ height: `${Math.max(15, (rmsVolume * (0.3 + Math.sin(i * 0.8) * 0.5)))}%` }"
                 class="w-1 bg-brand-primary rounded-full transition-all duration-75"></div>
          </div>

          <!-- 音量控制面板 -->
          <div class="flex items-center space-x-3 bg-white/5 p-2 rounded-xl border border-white/10">
            <button @click="toggleMute" 
                    class="p-1.5 rounded-lg text-[10px] font-bold transition-all active:scale-95 hover:bg-white/10 shrink-0"
                    :class="[isMuted ? 'text-red-400 bg-red-400/10 border border-red-500/20' : 'text-brand-secondary bg-brand-secondary/10 border border-brand-secondary/20']">
              <span>{{ isMuted ? '🔇 已静音' : '🔊 监听中' }}</span>
            </button>
            <div class="flex items-center space-x-1.5 px-1 shrink-0">
              <span class="text-[10px] text-brand-textMuted">增益</span>
              <input type="range" min="0.5" max="3.0" step="0.1" v-model.number="volumeGain" 
                     class="w-16 h-1 rounded bg-white/10 appearance-none accent-brand-secondary cursor-pointer" />
              <span class="text-[10px] font-mono text-brand-secondary font-bold w-6">{{ volumeGain }}x</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 拖拽发送区 -->
    <div id="transfer-send" @dragover.prevent="dragOver = true" @dragleave="dragOver = false" @drop.prevent="handleDrop"
         :class="[dragOver ? 'border-brand-secondary bg-brand-secondary/5' : 'border-brand-border hover:bg-white/[0.01]']"
         class="h-32 rounded-2xl border border-dashed flex flex-col items-center justify-center cursor-pointer transition-all duration-200 shrink-0"
         @click="selectAndSendFile">
      <svg class="w-8 h-8 text-brand-secondary mb-2 animate-bounce" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
      </svg>
      <span class="text-xs font-semibold text-brand-text">点击选择 或 拖拽电脑文件至此处发送给手机</span>
      <span class="text-[10px] text-brand-textMuted mt-1">支持任意格式大文件，局域网点对点高速直传</span>
    </div>

    <!-- 剪贴板与文本极速桥接卡片 -->
    <div id="transfer-clipboard" class="p-5 rounded-2xl glass-panel border border-brand-border flex flex-col space-y-4 shrink-0">
      <div class="flex items-center space-x-2.5">
        <span class="w-2.5 h-2.5 rounded-full bg-brand-primary dot-pulse shadow-glow-purple shrink-0"></span>
        <span class="text-xs font-semibold tracking-wider text-brand-textMuted uppercase">剪贴板与文本极速桥接 (支持图片 Ctrl+V 自动识别)</span>
      </div>
      
      <div class="flex space-x-3 items-end">
        <textarea v-model="inputText" placeholder="直接在此输入或粘贴大段文字、分享网页链接... 按 Ctrl+V 也可智能识别并粘贴发送剪贴板图片！"
                  class="flex-1 min-h-[72px] bg-black/30 border border-brand-border rounded-xl p-3 text-xs text-gray-200 placeholder-gray-500 focus:outline-none focus:border-brand-primary focus:shadow-glow-purple transition-all duration-200 resize-none"></textarea>
        
        <div class="flex flex-col space-y-2 shrink-0">
          <button @click="sendInputText" :disabled="!inputText.trim()"
                  class="px-4 py-2.5 rounded-xl bg-gradient-to-r from-brand-primary to-brand-primary/80 text-white font-semibold text-xs transition-transform active:scale-95 disabled:opacity-50 disabled:pointer-events-none hover:shadow-glow-purple">
            发送输入文本
          </button>
          <button @click="sendSystemClipboard"
                  class="px-4 py-2.5 rounded-xl border border-brand-secondary bg-brand-secondary/10 hover:bg-brand-secondary/20 text-brand-secondary font-semibold text-xs transition-transform active:scale-95 hover:shadow-glow-cyan">
            发送系统剪贴板
          </button>
        </div>
      </div>
    </div>

    <!-- 传输进度与列表 -->
    <div id="transfer-records" class="flex-1 rounded-2xl glass-panel p-5 flex flex-col border border-brand-border min-h-[350px]">
      <div class="flex items-center justify-between pb-3.5 border-b border-brand-border">
        <span class="text-xs font-bold tracking-wider uppercase text-brand-secondary">极速互传与共享记录</span>
        <button @click="clearHistory" class="text-xs text-brand-textMuted hover:text-white transition-colors">清除记录</button>
      </div>

      <div class="flex-1 overflow-y-auto mt-4 space-y-3">
        <div v-for="item in transferList" :key="item.name + item.time" class="p-3.5 rounded-lg border border-brand-border bg-white/[0.01] flex flex-col space-y-2">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2 flex-1 min-w-0 mr-4">
              <span :class="[item.type === 'receive' ? 'text-emerald-400 bg-emerald-400/10' : 'text-brand-secondary bg-brand-secondary/10']" 
                    class="text-[10px] px-2 py-0.5 rounded font-bold uppercase shrink-0">
                {{ item.type === 'receive' ? '接收' : '发送' }}
              </span>
              <span class="text-xs font-semibold text-gray-200 truncate flex-1 min-w-0">{{ item.name }}</span>
            </div>
            <div class="flex items-center space-x-2">
              <!-- 一键复制文本 -->
              <button v-if="item.textRaw || item.name.includes('[发送文本]') || item.name.includes('[收到文本]') || item.name.includes('[发送剪贴板]') || item.name.includes('[收到链接]') || item.name.includes('[分享链接]')"
                      @click.stop="copyText(item.textRaw || item.name.split('] ')[1] || item.name)"
                      class="text-[10px] text-brand-secondary hover:text-white px-2 py-0.5 rounded border border-brand-secondary/30 bg-brand-secondary/5 hover:bg-brand-secondary/20 transition-colors">
                复制文本
              </button>
              <!-- 一键打开链接 -->
              <button v-if="item.isUrl || item.name.includes('[收到链接]') || item.name.includes('[分享链接]')"
                      @click.stop="openUrl(item.textRaw || item.name.split('] ')[1] || item.name)"
                      class="text-[10px] text-brand-accent hover:text-white px-2 py-0.5 rounded border border-brand-accent/30 bg-brand-accent/5 hover:bg-brand-accent/20 transition-colors">
                ⚡ 打开链接
              </button>
              <!-- 一键定位文件/文本所在文件夹 -->
              <button v-if="item.path"
                      @click.stop="openFile(item.path)"
                      class="text-[10px] text-emerald-400 hover:text-white px-2 py-0.5 rounded border border-emerald-400/30 bg-emerald-400/5 hover:bg-emerald-400/20 transition-colors">
                📂 定位
              </button>
              <span class="text-xs font-bold font-mono" :class="[item.status === '完成' ? 'text-emerald-400' : 'text-brand-secondary']">
                {{ item.status }}
              </span>
            </div>
          </div>

          <div v-if="item.status !== '完成' && item.progress !== undefined" class="w-full bg-white/5 h-1.5 rounded-full overflow-hidden flex items-center">
            <div :style="{ width: item.progress + '%' }" :class="[item.type === 'receive' ? 'bg-emerald-400' : 'bg-brand-secondary']" class="h-full transition-all duration-100"></div>
          </div>

          <div class="flex items-center justify-between text-[10px] text-brand-textMuted">
            <span>{{ (item.size / 1024 / 1024) > 0.01 ? (item.size / 1024 / 1024).toFixed(2) + ' MB' : item.size + ' B' }} | {{ item.time }}</span>
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
import { ref, onMounted, onUnmounted, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps<{
  serverRunning: boolean
  ips: string[]
  port: number
}>()

const dragOver = ref(false)

// --- 实时无线麦克风音频播放与调度系统 ---
const audioCtx = ref<AudioContext | null>(null)
const gainNode = ref<GainNode | null>(null)
const isMuted = ref(false)
const volumeGain = ref(1.5) // 默认 1.5 倍增益放大
const isAudioStreaming = ref(false)
const audioStreamTime = ref("00:00")
const streamByteCount = ref(0)
const rmsVolume = ref(0) // 实时音量均方根 [0, 100]

let nextPlayTime = 0
let streamStartTime = 0
let timerInterval: any = null

// 初始化 Web Audio 环境
const initAudioContext = () => {
  if (!audioCtx.value) {
    const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext
    audioCtx.value = new AudioContextClass({ sampleRate: 16000 })
    gainNode.value = audioCtx.value.createGain()
    gainNode.value.gain.value = volumeGain.value
    gainNode.value.connect(audioCtx.value.destination)
  }
  if (audioCtx.value.state === 'suspended') {
    audioCtx.value.resume()
  }
}

// 调节音量增益
watch(volumeGain, (val) => {
  if (gainNode.value) {
    gainNode.value.gain.value = isMuted.value ? 0 : val
  }
})

// 静音开关
const toggleMute = () => {
  isMuted.value = !isMuted.value
  if (gainNode.value) {
    gainNode.value.gain.value = isMuted.value ? 0 : volumeGain.value
  }
}

// 收到原始 PCM 数据包
const handleAudioChunk = (uint8Array: Uint8Array) => {
  initAudioContext()
  
  if (!audioCtx.value || !gainNode.value) return
  
  if (!isAudioStreaming.value) {
    isAudioStreaming.value = true
    streamStartTime = Date.now()
    streamByteCount.value = 0
    rmsVolume.value = 0
    nextPlayTime = audioCtx.value.currentTime
    
    // 启动时长计时器
    if (timerInterval) clearInterval(timerInterval)
    timerInterval = setInterval(() => {
      const diff = Math.floor((Date.now() - streamStartTime) / 1000)
      const mm = String(Math.floor(diff / 60)).padStart(2, '0')
      const ss = String(diff % 60).padStart(2, '0')
      audioStreamTime.value = `${mm}:${ss}`
    }, 1000)
  }
  
  streamByteCount.value += uint8Array.length
  
  // 转换 Uint8Array -> Int16Array -> Float32Array
  const buffer = uint8Array.buffer
  const byteOffset = uint8Array.byteOffset
  const byteLength = uint8Array.byteLength
  
  // 16bit PCM 每个采样占 2 字节
  const samples = byteLength / 2
  const int16Data = new Int16Array(buffer, byteOffset, samples)
  const float32Data = new Float32Array(samples)
  
  let sumSquare = 0
  for (let i = 0; i < samples; i++) {
    const val = int16Data[i] / 32768.0 // 归一化到 [-1, 1]
    float32Data[i] = val
    sumSquare += val * val
  }
  
  // 计算均方根音量 (RMS) 用于波形跳动
  const rms = Math.sqrt(sumSquare / (samples || 1))
  rmsVolume.value = Math.min(100, Math.floor(rms * 250)) // 限制在 0-100 内
  
  // 如果静音，依然计算音量但不做排队播放以省 CPU
  if (isMuted.value) return
  
  // 创建 AudioBuffer 并播放
  const audioBuffer = audioCtx.value.createBuffer(1, samples, 16000)
  audioBuffer.getChannelData(0).set(float32Data)
  
  const source = audioCtx.value.createBufferSource()
  source.buffer = audioBuffer
  source.connect(gainNode.value)
  
  // 排队调度算法
  const now = audioCtx.value.currentTime
  let startTime = nextPlayTime
  if (startTime < now) {
    startTime = now // 如果网络延迟累积，重置为当前时间
  }
  
  source.start(startTime)
  nextPlayTime = startTime + audioBuffer.duration
}

// 停止流式传输
const handleAudioEnd = () => {
  isAudioStreaming.value = false
  rmsVolume.value = 0
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}
const qrCanvas = ref<HTMLCanvasElement | null>(null)
const inputText = ref('')

interface TransferItem {
  name: string
  size: number
  type: 'send' | 'receive'
  status: string
  progress?: number
  time: string
  textRaw?: string
  isUrl?: boolean
  path?: string
}

// 从本地缓存加载互传记录并清洗中间状态
const loadCachedRecords = (): TransferItem[] => {
  try {
    const cached = localStorage.getItem('sparkai_transfer_records')
    if (cached) {
      const list = JSON.parse(cached) as TransferItem[]
      return list.map(item => {
        // 清洗处于非终结状态（非“完成”且非“失败”）的过渡态数据，防止显示错误
        if (item.status !== '完成' && item.status !== '失败') {
          return { ...item, status: '已中断', progress: 0 }
        }
        return item
      })
    }
    return []
  } catch (e) {
    return []
  }
}

const transferList = ref<TransferItem[]>(loadCachedRecords())

const copyText = (text: string) => {
  navigator.clipboard.writeText(text)
}

const openUrl = (url: string) => {
  const targetUrl = url.startsWith('http') ? url : `http://${url}`
  window.open(targetUrl, '_blank')
}

// 电脑端输入文本并发送
const sendInputText = async () => {
  const text = inputText.value.trim()
  if (!text) return
  const isUrl = text.startsWith('http://') || text.startsWith('https://')
  if (window.electronAPI) {
    const savedPath = await window.electronAPI.sendTextOffer(text, isUrl)
    transferList.value.unshift({
      name: isUrl ? `[分享链接] ${text}` : `[发送文本] ${text.length > 30 ? text.substring(0, 30) + '...' : text}`,
      size: text.length,
      type: 'send',
      status: '完成',
      time: new Date().toLocaleTimeString(),
      textRaw: text,
      isUrl: isUrl,
      path: savedPath
    })
    inputText.value = ''
  }
}

// 自动识别系统剪贴板类型并发送
const sendSystemClipboard = async () => {
  try {
    const text = await navigator.clipboard.readText()
    if (text && text.trim().length > 0) {
      const isUrl = text.startsWith('http://') || text.startsWith('https://')
      if (window.electronAPI) {
        const savedPath = await window.electronAPI.sendTextOffer(text, isUrl)
        transferList.value.unshift({
          name: isUrl ? `[分享链接] ${text}` : `[发送剪贴板] ${text.length > 30 ? text.substring(0, 30) + '...' : text}`,
          size: text.length,
          type: 'send',
          status: '完成',
          time: new Date().toLocaleTimeString(),
          textRaw: text,
          isUrl: isUrl,
          path: savedPath
        })
        return
      }
    }
  } catch (e) {
    // 浏览器没有剪贴板权限或为图片
  }

  // 尝试读取剪贴板图片并作为要约发送
  if (window.electronAPI) {
    const res = await window.electronAPI.readClipboardImageAndOffer()
    if (res.success) {
      transferList.value.unshift({
        name: res.filename,
        size: res.size,
        type: 'send',
        status: '等待手机确认...',
        progress: 0,
        time: new Date().toLocaleTimeString()
      })
    } else {
      alert('剪贴板中未发现有效文本或图片内容！')
    }
  }
}

// 监听键盘 Ctrl+V 粘贴截图/图片一键极速发送
const handleGlobalPaste = async (e: ClipboardEvent) => {
  const items = e.clipboardData?.items
  if (!items) return
  
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.type.indexOf('image') !== -1) {
      // 捕获到了图片数据
      e.preventDefault()
      if (window.electronAPI) {
        const res = await window.electronAPI.readClipboardImageAndOffer()
        if (res.success) {
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
      break
    }
  }
}

const openSaveDir = () => window.electronAPI?.openSaveDir()

const openFile = (path: string) => {
  if (window.electronAPI && window.electronAPI.openFile) {
    window.electronAPI.openFile(path)
  }
}

const selectAndSendFile = async () => {
  const res = await window.electronAPI?.selectAndSendFile()
  if (res?.success) {
    transferList.value.unshift({
      name: res.filename,
      size: res.size,
      type: 'send',
      status: '等待手机确认...',
      progress: 0,
      time: new Date().toLocaleTimeString(),
      path: res.filePath
    })
  }
}

const handleDrop = async (e: DragEvent) => {
  dragOver.value = false
  const file = e.dataTransfer?.files[0]
  if (file && window.electronAPI) {
    const res = await window.electronAPI?.selectAndSendFile()
  }
}

const clearHistory = () => { transferList.value = [] }

const loadQRCode = () => {
  if (qrCanvas.value && props.ips && props.ips.length > 0) {
    const connectUrl = `http://${props.ips[0]}:${props.port}`
    QRCode.toCanvas(qrCanvas.value, connectUrl, {
      width: 96,
      margin: 1,
      color: { dark: '#ffffff', light: '#1e1e2f00' }
    })
  }
}

watch(() => props.ips, () => {
  loadQRCode()
}, { deep: true, immediate: true })

const handleScrollToSection = (e: any) => {
  if (e.detail && e.detail.id) {
    const el = document.getElementById(e.detail.id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }
}

onMounted(() => {
  loadQRCode()
  window.addEventListener('paste', handleGlobalPaste)
  window.addEventListener('scroll-to-transfer-section', handleScrollToSection)
  
  if (window.electronAPI) {
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
        time: file.time,
        path: file.path
      })
    })

    window.electronAPI.onTextReceived((data: any) => {
      transferList.value.unshift({
        name: data.isUrl ? `[收到链接] ${data.text}` : `[收到文本] ${data.text.length > 30 ? data.text.substring(0, 30) + '...' : data.text}`,
        size: data.text.length,
        type: 'receive',
        status: '完成',
        time: data.time || new Date().toLocaleTimeString(),
        textRaw: data.text,
        isUrl: data.isUrl,
        path: data.savedPath
      })
    })

    window.electronAPI.onAudioStreamData((chunk: Uint8Array) => {
      handleAudioChunk(chunk)
    })

    window.electronAPI.onAudioStreamEnd(() => {
      handleAudioEnd()
    })
  }
})

// 深度监听传输列表的变化并存入本地缓存
watch(transferList, (newList) => {
  try {
    localStorage.setItem('sparkai_transfer_records', JSON.stringify(newList))
  } catch (e) {
    console.error('[Cache Records Error]', e)
  }
}, { deep: true })

onUnmounted(() => {
  window.removeEventListener('paste', handleGlobalPaste)
  window.removeEventListener('scroll-to-transfer-section', handleScrollToSection)
  handleAudioEnd()
})
</script>
