import http from 'http'
import fs from 'fs'
import path from 'path'
import os from 'os'
import { BrowserWindow } from 'electron'

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
  if (server) {
    sseClients.forEach(res => res.end())
    sseClients.clear()
    server.close()
    server = null
    notifyRenderer('server-log', { level: 'WARN', message: `局域网通信服务器已关闭` })
  }
}
