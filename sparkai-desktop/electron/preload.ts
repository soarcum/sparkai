import { contextBridge, ipcRenderer } from 'electron'

// 安全地将窗口控制与文件互传 API 暴露给渲染进程
contextBridge.exposeInMainWorld('electronAPI', {
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.send('window-maximize'),
  close: () => ipcRenderer.send('window-close'),
  
  // 局域网文件互传控制接口
  startFileServer: () => ipcRenderer.invoke('start-file-server'),
  selectAndSendFile: () => ipcRenderer.invoke('select-and-send-file'),
  openSaveDir: () => ipcRenderer.send('open-save-dir'),
  openFile: (filePath: string) => ipcRenderer.send('open-file', filePath),
  sendTextOffer: (text: string, isUrl: boolean) => ipcRenderer.invoke('send-text-offer', text, isUrl),
  readClipboardImageAndOffer: () => ipcRenderer.invoke('read-clipboard-image-and-offer'),
  getSaveDir: () => ipcRenderer.invoke('get-save-dir'),
  selectSaveDir: () => ipcRenderer.invoke('select-save-dir'),
  setSaveDir: (dirPath: string) => ipcRenderer.invoke('set-save-dir', dirPath),
  
  // 监听主进程推送的数据流事件
  onServerLog: (callback: (data: any) => void) => {
    ipcRenderer.on('server-log', (_, data) => callback(data))
  },
  onTransferProgress: (callback: (data: any) => void) => {
    ipcRenderer.on('transfer-progress', (_, data) => callback(data))
  },
  onFileReceived: (callback: (data: any) => void) => {
    ipcRenderer.on('file-received', (_, data) => callback(data))
  },
  onTextReceived: (callback: (data: any) => void) => {
    ipcRenderer.on('text-received', (_, data) => callback(data))
  },
  onAudioStreamData: (callback: (chunk: Uint8Array) => void) => {
    ipcRenderer.on('audio-stream-data', (_, data) => callback(data))
  },
  onAudioStreamEnd: (callback: () => void) => {
    ipcRenderer.on('audio-stream-end', (_) => callback())
  }
})
