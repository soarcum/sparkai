export interface AAMSLog {
  time: string;
  level: 'info' | 'success' | 'warn' | 'error';
  message: string;
}

export interface AAMSModule {
  id: string;          // 模块唯一标识符
  name: string;        // 模块名称
  description: string; // 模块描述
  prompt: string;      // 用户提出的原始自然语言需求
  code: string;        // AI 撰写的 JS 脚本源代码
  enabled: boolean;    // 是否处于启用激活状态
  logs: AAMSLog[];     // 运行时的控制台调试日志
}

export interface AIScriptSDK {
  dom: {
    find: (selector: string) => HTMLElement | null;
    findAll: (selector: string) => HTMLElement[];
    insertAfter: (target: HTMLElement, html: string) => HTMLElement | null;
    insertBefore: (target: HTMLElement, html: string) => HTMLElement | null;
    append: (target: HTMLElement, html: string) => HTMLElement | null;
    highlight: (target: HTMLElement, keyword: string, className?: string) => void;
  };
  ai: {
    analyzeText: (text: string, prompt: string) => Promise<string>;
  };
  ui: {
    toast: (message: string, type?: 'info' | 'success' | 'warn' | 'error') => void;
  };
  storage: {
    get: (key: string) => any;
    set: (key: string, value: any) => void;
  };
}

export interface AAMSLifecyleObject {
  onLoad?: (sdk: AIScriptSDK) => void;
  onUnload?: (sdk: AIScriptSDK) => void;
}
