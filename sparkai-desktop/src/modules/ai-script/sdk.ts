import { AIScriptSDK } from './types'

/**
 * 为特定的 AI 自动模块创建一个沙箱 SDK 实例
 * @param moduleId 模块唯一标识符
 * @param addLog 日志追加器回调函数
 * @param getAIConfig 获取 AI 核心配置的回调函数
 */
export function createSDKInstance(
  moduleId: string, 
  addLog: (level: 'info' | 'success' | 'warn' | 'error', msg: string) => void,
  getAIConfig: () => { apiKey: string; baseUrl: string; model: string }
): AIScriptSDK {
  
  // DOM 自动打标函数，打上特定模块 ID
  const markElement = (el: HTMLElement) => {
    el.setAttribute('data-aams-id', moduleId);
    el.classList.add(`aams-module-inserted-${moduleId}`);
  };

  // 从 HTML 字符串模板创建 DOM 元素并打标
  const createDOMFromHTML = (html: string): HTMLElement | null => {
    const template = document.createElement('template');
    template.innerHTML = html.trim();
    const el = template.content.firstElementChild as HTMLElement | null;
    if (el) {
      markElement(el);
      // 递归打标所有后代节点
      const descendants = el.querySelectorAll('*');
      descendants.forEach(child => {
        (child as HTMLElement).setAttribute('data-aams-id', moduleId);
      });
    }
    return el;
  };

  return {
    dom: {
      find(selector: string) {
        return document.querySelector(selector);
      },
      findAll(selector: string) {
        return Array.from(document.querySelectorAll(selector));
      },
      insertAfter(target: HTMLElement, html: string) {
        if (!target) return null;
        const el = createDOMFromHTML(html);
        if (el && target.parentNode) {
          target.parentNode.insertBefore(el, target.nextSibling);
          addLog('info', `DOM 插入: 在 <${target.tagName.toLowerCase()}> 元素后成功插入元素`);
          return el;
        }
        return null;
      },
      insertBefore(target: HTMLElement, html: string) {
        if (!target) return null;
        const el = createDOMFromHTML(html);
        if (el && target.parentNode) {
          target.parentNode.insertBefore(el, target);
          addLog('info', `DOM 插入: 在 <${target.tagName.toLowerCase()}> 元素前成功插入元素`);
          return el;
        }
        return null;
      },
      append(target: HTMLElement, html: string) {
        if (!target) return null;
        const el = createDOMFromHTML(html);
        if (el) {
          target.appendChild(el);
          addLog('info', `DOM 插入: 向 <${target.tagName.toLowerCase()}> 内部追加子元素`);
          return el;
        }
        return null;
      },
      highlight(target: HTMLElement, keyword: string, className = 'bg-yellow-500/20 text-yellow-300 border-b border-yellow-500/50 px-0.5 mx-0.5 rounded') {
        if (!target) return;
        const text = target.innerHTML;
        // 简单安全的全局忽略大小写替换
        const regex = new RegExp(`(${keyword})`, 'gi');
        const highlightedText = text.replace(regex, `<span class="${className}" data-aams-id="${moduleId}">$1</span>`);
        target.innerHTML = highlightedText;
        addLog('info', `高亮圈词: 成功高亮元素中的关键词 "${keyword}"`);
      }
    },
    ai: {
      async analyzeText(text: string, prompt: string) {
        addLog('info', `AI 深度文本处理中，输入字符长度: ${text.length}`);
        const config = getAIConfig();
        if (!config.apiKey) {
          const err = '未配置 AI API Key，请在“参数配置”中填写您的密钥';
          addLog('error', err);
          throw new Error(err);
        }
        
        try {
          const response = await fetch(`${config.baseUrl}/chat/completions`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${config.apiKey}`
            },
            body: JSON.stringify({
              model: config.model || 'mimo-v2.5-pro',
              messages: [
                { 
                  role: 'system', 
                  content: '你是一个内置于应用脚本中的 AI 文本分析助手。请严格按照用户 Prompt 的要求，对提供的网页上下文文本进行深度分析，仅返回你需要提取/翻译/总结的纯文本结果。' 
                },
                { 
                  role: 'user', 
                  content: `Prompt指令: ${prompt}\n\n需要处理的网页文本：\n"""\n${text}\n"""` 
                }
              ],
              temperature: 0.3
            })
          });
          
          if (!response.ok) {
            const errText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errText}`);
          }
          
          const result = await response.json();
          const content = result.choices?.[0]?.message?.content || '';
          addLog('success', `AI 分析就绪，返回结果长度: ${content.length}`);
          return content;
        } catch (error: any) {
          const errMsg = `AI 分析请求异常: ${error.message || error}`;
          addLog('error', errMsg);
          throw new Error(errMsg);
        }
      }
    },
    ui: {
      toast(message: string, type = 'info') {
        window.dispatchEvent(new CustomEvent('aams-toast', {
          detail: { message, type }
        }));
        addLog(type, `Toast 提醒 [${type}]: ${message}`);
      }
    },
    storage: {
      get(key: string) {
        const fullKey = `aams_module_kv_${moduleId}_${key}`;
        const val = localStorage.getItem(fullKey);
        try {
          return val ? JSON.parse(val) : null;
        } catch {
          return val;
        }
      },
      set(key: string, value: any) {
        const fullKey = `aams_module_kv_${moduleId}_${key}`;
        localStorage.setItem(fullKey, JSON.stringify(value));
      }
    }
  }
}
