/// <reference types="vite/client" />

interface Window {
  electronAPI?: {
    minimize: () => void
    maximize: () => void
    close: () => void
    startFileServer: () => Promise<any>
    selectAndSendFile: () => Promise<any>
    openSaveDir: () => void
    onServerLog: (callback: (data: any) => void) => void
    onTransferProgress: (callback: (data: any) => void) => void
    onFileReceived: (callback: (data: any) => void) => void
  }
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
