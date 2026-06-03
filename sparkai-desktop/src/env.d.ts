/// <reference types="vite/client" />

interface Window {
  electronAPI?: {
    minimize: () => void
    maximize: () => void
    close: () => void
    startFileServer: () => Promise<any>
    selectAndSendFile: () => Promise<any>
    openSaveDir: () => void
    openFile: (filePath: string) => void
    sendTextOffer: (text: string, isUrl: boolean) => Promise<string>
    readClipboardImageAndOffer: () => Promise<any>
    getSaveDir: () => Promise<{ customPath: string; defaultPath: string; activePath: string }>
    selectSaveDir: () => Promise<any>
    setSaveDir: (dirPath: string) => Promise<any>
    onServerLog: (callback: (data: any) => void) => void
    onTransferProgress: (callback: (data: any) => void) => void
    onFileReceived: (callback: (data: any) => void) => void
    onTextReceived: (callback: (data: any) => void) => void
    onAudioStreamData: (callback: (chunk: Uint8Array) => void) => void
    onAudioStreamEnd: (callback: () => void) => void
  }
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'qrcode' {
  const QRCode: {
    toCanvas(canvas: HTMLCanvasElement, text: string, options?: any): Promise<void>
  }
  export default QRCode
}
