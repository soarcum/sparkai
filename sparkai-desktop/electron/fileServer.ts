import http from 'http'
import fs from 'fs'
import path from 'path'
import os from 'os'
import { BrowserWindow } from 'electron'
import dgram from 'dgram'

// 手机上传的文件默认保存路径 (用户桌面的 SparkAI-Files)
export const SAVE_DIR = path.join(os.homedir(), 'Desktop', 'SparkAI-Files')
if (!fs.existsSync(SAVE_DIR)) {
  fs.mkdirSync(SAVE_DIR, { recursive: true })
}

let server: http.Server | null = null
let activePort = 9090
const sseClients = new Set<http.ServerResponse>()
// 待手机下载的文件池：ID -> { path, name, size }
const downloadPool = new Map<string, { path: string; name: string; size: number }>()

// 获取本机所有局域网 IPv4 地址
export function getIPAddresses(): string[] {
  const interfaces = os.networkInterfaces()
  const ips: string[] = []
  for (const name of Object.keys(interfaces)) {
    for (const net of interfaces[name] || []) {
      if (net.family === 'IPv4' && !net.internal) {
        ips.push(net.address)
      }
    }
  }
  return ips
}

// 广播 SSE 事件给所有已连接的手机
export function broadcastSSE(event: string, data: any) {
  const payload = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`
  sseClients.forEach(res => {
    try {
      res.write(payload)
    } catch (e) {
      sseClients.delete(res)
    }
  })
}

// 向渲染进程（桌面端 UI）发送日志与通知
function notifyRenderer(channel: string, data: any) {
  const windows = BrowserWindow.getAllWindows()
  if (windows.length > 0) {
    windows[0].webContents.send(channel, data)
  }
}

// 处理 SSE 长连接建立
function handleSSE(req: http.IncomingMessage, res: http.ServerResponse) {
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive',
    'Access-Control-Allow-Origin': '*'
  })
  res.write('event: connected\ndata: {"status":"ready"}\n\n')
  sseClients.add(res)
  notifyRenderer('server-log', { level: 'SUCCESS', message: `手机端通过 SSE 成功建立长连接监听` })
  
  req.on('close', () => {
    sseClients.delete(res)
    notifyRenderer('server-log', { level: 'INFO', message: `手机端长连接已断开` })
  })
}

// 处理手机端上传文件 (HTTP POST)
function handleUpload(req: http.IncomingMessage, res: http.ServerResponse, parsedUrl: URL) {
  const filename = decodeURIComponent(parsedUrl.searchParams.get('filename') || 'uploaded_file')
  const sizeStr = parsedUrl.searchParams.get('size') || '0'
  const totalSize = parseInt(sizeStr, 10)
  
  const targetPath = path.join(SAVE_DIR, filename)
  const writeStream = fs.createWriteStream(targetPath)
  let receivedBytes = 0
  let lastProgressTime = 0

  notifyRenderer('server-log', { level: 'INFO', message: `开始接收手机上传文件: ${filename} (${(totalSize / 1024 / 1024).toFixed(2)} MB)` })

  req.on('data', (chunk) => {
    receivedBytes += chunk.length
    const now = Date.now()
    if (now - lastProgressTime > 200 || receivedBytes === totalSize) {
      lastProgressTime = now
      notifyRenderer('transfer-progress', { filename, type: 'receive', progress: Math.min(100, (receivedBytes / totalSize) * 100), bytes: receivedBytes })
    }
  })

  req.pipe(writeStream)

  writeStream.on('finish', () => {
    res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
    res.end(JSON.stringify({ success: true, message: 'File saved successfully' }))
    notifyRenderer('server-log', { level: 'SUCCESS', message: `手机上传文件成功保存至: ${targetPath}` })
    notifyRenderer('file-received', { name: filename, size: totalSize, path: targetPath, time: new Date().toLocaleTimeString() })
  })

  writeStream.on('error', (err) => {
    res.writeHead(500, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
    res.end(JSON.stringify({ success: false, message: err.message }))
    notifyRenderer('server-log', { level: 'WARN', message: `文件保存失败: ${err.message}` })
  })
}

// 处理手机端下载文件 (HTTP GET)
function handleDownload(req: http.IncomingMessage, res: http.ServerResponse, parsedUrl: URL) {
  const id = parsedUrl.searchParams.get('id') || ''
  const item = downloadPool.get(id)
  
  if (!item || !fs.existsSync(item.path)) {
    res.writeHead(404, { 'Content-Type': 'text/plain', 'Access-Control-Allow-Origin': '*' })
    res.end('File not found')
    return
  }

  res.writeHead(200, {
    'Content-Type': 'application/octet-stream',
    'Content-Length': item.size,
    'Content-Disposition': `attachment; filename="${encodeURIComponent(item.name)}"`,
    'Access-Control-Allow-Origin': '*'
  })

  const readStream = fs.createReadStream(item.path)
  let sentBytes = 0
  let lastProgressTime = 0

  notifyRenderer('server-log', { level: 'INFO', message: `开始推送文件给手机: ${item.name}` })

  readStream.on('data', (chunk) => {
    sentBytes += chunk.length
    const now = Date.now()
    if (now - lastProgressTime > 200 || sentBytes === item.size) {
      lastProgressTime = now
      notifyRenderer('transfer-progress', { filename: item.name, type: 'send', progress: Math.min(100, (sentBytes / item.size) * 100), bytes: sentBytes })
    }
  })

  readStream.pipe(res)
}

// 处理手机端分享的文本/链接 (HTTP POST)
function handleShareText(req: http.IncomingMessage, res: http.ServerResponse) {
  let body = ''
  req.on('data', chunk => {
    body += chunk.toString()
  })
  req.on('end', () => {
    try {
      const json = JSON.parse(body)
      const text = json.text || ''
      const isUrl = text.startsWith('http://') || text.startsWith('https://')
      notifyRenderer('server-log', { level: 'SUCCESS', message: `收到手机分享的文本: ${text.length > 30 ? text.substring(0, 30) + '...' : text}` })
      notifyRenderer('text-received', { text, isUrl, time: new Date().toLocaleTimeString(), type: 'receive' })
      res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
      res.end(JSON.stringify({ success: true }))
    } catch (err: any) {
      res.writeHead(400, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
      res.end(JSON.stringify({ success: false, error: err.message }))
    }
  })
}

// 处理手机端实时音频流投射 (HTTP POST Chunked PCM)
function handleAudioStream(req: http.IncomingMessage, res: http.ServerResponse) {
  notifyRenderer('server-log', { level: 'INFO', message: `手机无线麦克风音频流已建立连接，开始透传...` })
  
  req.on('data', (chunk: Buffer) => {
    // 收到原始 PCM 字节，通过 IPC 发给渲染进程
    notifyRenderer('audio-stream-data', chunk)
  })
  
  req.on('end', () => {
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*'
    })
    res.end(JSON.stringify({ success: true, message: 'Audio stream finished' }))
    notifyRenderer('server-log', { level: 'SUCCESS', message: `手机无线麦克风音频流传输正常结束` })
    notifyRenderer('audio-stream-end', null)
  })
  
  req.on('error', (err) => {
    res.writeHead(500, {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*'
    })
    res.end(JSON.stringify({ success: false, error: err.message }))
    notifyRenderer('server-log', { level: 'WARN', message: `手机音频流传输发生异常: ${err.message}` })
    notifyRenderer('audio-stream-end', null)
  })
}

// 广播文本或链接要约给手机端
export function broadcastTextOffer(text: string, isUrl: boolean) {
  const id = Math.random().toString(36).substring(2, 10)
  broadcastSSE('text-offer', { id, text, isUrl })
  notifyRenderer('server-log', { level: 'INFO', message: `已向手机端广播文本要约: ${text.length > 30 ? text.substring(0, 30) + '...' : text}` })
}

// 注册待发送文件的要约
export function registerFileOffer(filePath: string, name: string, size: number): string {
  const id = Math.random().toString(36).substring(2, 10)
  downloadPool.set(id, { path: filePath, name, size })
  return id
}

// 启动局域网 HTTP + SSE 文件传输服务
export function startFileServer(): Promise<number> {
  return new Promise((resolve, reject) => {
    server = http.createServer((req, res) => {
      const parsedUrl = new URL(req.url || '', `http://localhost:${activePort}`)
      
      // 处理跨域预检
      if (req.method === 'OPTIONS') {
        res.writeHead(204, {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type'
        })
        res.end()
        return
      }

      if (parsedUrl.pathname === '/') {
        res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' })
        res.end(JSON.stringify({ service: 'SparkAI Transmission Server', status: 'running' }))
      } else if (parsedUrl.pathname === '/events') {
        handleSSE(req, res)
      } else if (parsedUrl.pathname === '/upload' && req.method === 'POST') {
        handleUpload(req, res, parsedUrl)
      } else if (parsedUrl.pathname === '/share/text' && req.method === 'POST') {
        handleShareText(req, res)
      } else if (parsedUrl.pathname === '/audio/stream' && req.method === 'POST') {
        handleAudioStream(req, res)
      } else if (parsedUrl.pathname === '/download') {
        handleDownload(req, res, parsedUrl)
      } else {
        res.writeHead(404)
        res.end('Not Found')
      }
    })

    const tryListen = (port: number) => {
      server?.listen(port, () => {
        activePort = port
        notifyRenderer('server-log', { level: 'SUCCESS', message: `局域网通信服务器在端口 ${port} 启动成功` })
        const ips = getIPAddresses()
        startUdpBroadcast(ips, port)
        resolve(port)
      })

      server?.on('error', (err: any) => {
        if (err.code === 'EADDRINUSE') {
          notifyRenderer('server-log', { level: 'WARN', message: `端口 ${port} 被占用，正在尝试端口 ${port + 1}...` })
          tryListen(port + 1)
        } else {
          reject(err)
        }
      })
    }

    tryListen(activePort)
  })
}

// 停止服务
export function stopFileServer() {
  stopUdpBroadcast()
  if (server) {
    sseClients.forEach(res => res.end())
    sseClients.clear()
    server.close()
    server = null
    notifyRenderer('server-log', { level: 'WARN', message: `局域网通信服务器已关闭` })
  }
}

let udpSocket: dgram.Socket | null = null
let udpTimer: NodeJS.Timeout | null = null

// 开启 UDP 广播服务，向局域网宣告此电脑节点
export function startUdpBroadcast(ips: string[], port: number) {
  stopUdpBroadcast()
  udpSocket = dgram.createSocket('udp4')
  
  udpSocket.bind(() => {
    try {
      udpSocket?.setBroadcast(true)
      
      // 定期 3 秒向局域网广播一次
      udpTimer = setInterval(() => {
        if (!udpSocket) return
        const message = JSON.stringify({
          type: 'sparkai-server',
          ips,
          port,
          hostname: os.hostname()
        })
        const payload = Buffer.from(message)
        // 广播端口 9092
        udpSocket.send(payload, 0, payload.length, 9092, '255.255.255.255', (err) => {
          if (err) {
            console.error('[UDP Broadcast Error]', err.message)
          }
        })
      }, 3000)
      
      notifyRenderer('server-log', { level: 'SUCCESS', message: `局域网 UDP 发现广播服务已拉起，端口: 9092` })
    } catch (e: any) {
      notifyRenderer('server-log', { level: 'WARN', message: `启动 UDP 广播发生异常: ${e.message}` })
    }
  })
}

// 停止 UDP 广播
export function stopUdpBroadcast() {
  if (udpTimer) {
    clearInterval(udpTimer)
    udpTimer = null
  }
  if (udpSocket) {
    try {
      udpSocket.close()
    } catch (e) {}
    udpSocket = null
    notifyRenderer('server-log', { level: 'INFO', message: `局域网 UDP 发现广播服务已安全关闭` })
  }
}
