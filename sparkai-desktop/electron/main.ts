import { app, BrowserWindow, ipcMain, Menu, dialog, shell } from 'electron'
import { startFileServer, stopFileServer, getIPAddresses, registerFileOffer, broadcastSSE, SAVE_DIR } from './fileServer'
import path from 'path'
import fs from 'fs'
import os from 'os'

// 动态计算用户桌面路径，实现生产环境跨用户诊断
const logPath = path.join(os.homedir(), 'Desktop', 'SparkAI-debug.log')

try {
  fs.writeFileSync(logPath, `[${new Date().toISOString()}] ===== [物理诊断] 主进程 JS 引擎成功拉起且已执行到第一行 =====\r\n`, { flag: 'a' })
} catch (e) {
  // 忽略
}

// 强制输出启动第一步日志
console.log('===== [SparkAI 主进程] 正在启动 =====')

app.disableHardwareAcceleration()
console.log('[SparkAI 主进程] 成功禁用 GPU 硬件加速')

function logPhys(msg: string) {
  try {
    fs.writeFileSync(logPath, `[${new Date().toISOString()}] ${msg}\r\n`, { flag: 'a' })
  } catch (e) {
    // 忽略
  }
}

// 捕获全局未捕获异常并物理记录
process.on('uncaughtException', (err) => {
  logPhys(`[物理诊断] 致命未捕获异常 (uncaughtException): ${err.message}\r\n堆栈:\r\n${err.stack}`)
})

process.on('unhandledRejection', (reason) => {
  logPhys(`[物理诊断] 致命未处理的 Promise 拒绝 (unhandledRejection): ${reason}`)
})

let mainWindow: BrowserWindow | null = null

function createWindow() {
  logPhys('[物理诊断] 正在创建主窗口 (createWindow)...')
  
  try {
    mainWindow = new BrowserWindow({
      width: 1280,
      height: 800,
      minWidth: 960,
      minHeight: 640,
      frame: false,
      hasShadow: true,
      show: true, // 显式指定直接曝光显示
      backgroundColor: '#0A0C10',
      webPreferences: {
        preload: path.join(__dirname, 'preload.js'),
        nodeIntegration: false,
        contextIsolation: true
      }
    })
    logPhys('[物理诊断] 主窗口 BrowserWindow 实例实例化成功！')
  } catch (err: any) {
    logPhys(`[物理诊断] 实例化 BrowserWindow 发生致命崩溃！错误: ${err.message}\r\n堆栈:\r\n${err.stack}`)
    return
  }

  // 强行无条件唤醒并在最前台获取焦点展示
  mainWindow.show()
  mainWindow.focus()

  Menu.setApplicationMenu(null)

  const filePath = path.join(__dirname, '../dist/index.html')
  logPhys(`[物理诊断] 准备加载本地页面，绝对路径: ${filePath}`)

  // 监听渲染进程生命周期
  mainWindow.webContents.on('did-finish-load', () => {
    logPhys('[物理诊断] 页面成功渲染完成！(did-finish-load)')
  })

  mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription, validatedURL) => {
    logPhys(`[物理诊断] 页面加载失败！错误码: ${errorCode}, 描述: ${errorDescription}, URL: ${validatedURL}`)
  })

  mainWindow.webContents.on('render-process-gone', (event, details) => {
    logPhys(`[物理诊断] 渲染进程夭折！原因: ${details.reason}, 退出码: ${details.exitCode}`)
  })

  try {
    if (process.env.VITE_DEV_SERVER_URL) {
      logPhys(`[物理诊断] 正在通过 URL 加载开发环境: ${process.env.VITE_DEV_SERVER_URL}`)
      mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
    } else {
      logPhys('[物理诊断] 正在通过 loadFile 加载本地物理文件...')
      mainWindow.loadFile(filePath)
    }
    logPhys('[物理诊断] loadFile 接口执行调用已派发。')
  } catch (err: any) {
    logPhys(`[物理诊断] loadFile/loadURL 发生致命崩溃！错误: ${err.message}\r\n堆栈:\r\n${err.stack}`)
  }

  mainWindow.on('closed', () => {
    logPhys('[物理诊断] 主窗口被销毁 (closed)')
    mainWindow = null
  })
}

// 注册无边框窗口的 IPC 控制逻辑
ipcMain.on('window-minimize', () => {
  mainWindow?.minimize()
})

ipcMain.on('window-maximize', () => {
  if (mainWindow) {
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize()
    } else {
      mainWindow.maximize()
    }
  }
})

ipcMain.on('window-close', () => {
  mainWindow?.close()
})

// 注册局域网文件互传控制接口
ipcMain.handle('start-file-server', async () => {
  try {
    const port = await startFileServer()
    const ips = getIPAddresses()
    return { success: true, ips, port }
  } catch (err: any) {
    return { success: false, error: err.message }
  }
})

ipcMain.handle('select-and-send-file', async () => {
  if (!mainWindow) return { success: false, error: '主窗口未创建' }
  try {
    const result = await dialog.showOpenDialog(mainWindow, {
      title: '选择发送给手机的文件',
      properties: ['openFile']
    })
    
    if (result.canceled || result.filePaths.length === 0) {
      return { success: false, cancelled: true }
    }

    const filePath = result.filePaths[0]
    const filename = path.basename(filePath)
    const stat = fs.statSync(filePath)
    
    // 注册到下载池并获取唯一 ID
    const fileId = registerFileOffer(filePath, filename, stat.size)
    
    // 通过 SSE 广播向手机下发要约
    broadcastSSE('file-offer', {
      id: fileId,
      filename,
      size: stat.size
    })
    
    return { success: true, filename, size: stat.size }
  } catch (err: any) {
    return { success: false, error: err.message }
  }
})

ipcMain.on('open-save-dir', () => {
  shell.openPath(SAVE_DIR)
})

logPhys('[物理诊断] 准备监听 app.whenReady()...')

app.whenReady().then(() => {
  logPhys('[物理诊断] app.whenReady() 成功触发！')
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('window-all-closed', () => {
  logPhys('[物理诊断] 所有窗口被关闭 (window-all-closed)')
  stopFileServer()
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
