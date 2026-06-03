import { AIScriptSDK, AAMSLifecyleObject } from './types'

// 追踪当前处于运行活跃状态的模块实例，用于后续统一管理和垃圾回收
const activeModules = new Map<string, AAMSLifecyleObject>();

/**
 * 编译并执行 AI 动态脚本，安全注入 SDK
 * @param moduleId 模块唯一标识符
 * @param codeStr AI 编写的原始 JS 代码
 * @param sdk 专有 SDK 实例
 * @param addLog 日志追加器函数
 */
export function loadAndRunModule(
  moduleId: string,
  codeStr: string,
  sdk: AIScriptSDK,
  addLog: (level: 'info' | 'success' | 'warn' | 'error', msg: string) => void
): boolean {
  try {
    // 1. 如果该模块已经在运行，先进行卸载
    if (activeModules.has(moduleId)) {
      unloadModule(moduleId, sdk, addLog);
    }

    addLog('info', '开始编译 AI 模块脚本...');
    
    // 2. 将代码包装在安全的闭包中，并执行
    // 脚本可以直接 return 一个包含 onLoad/onUnload 的对象，或者将它们赋给 module.exports / 局部变量
    const wrappedCode = `
      return (function() {
        try {
          ${codeStr}
        } catch(e) {
          throw new Error("运行时语法编译错误: " + e.message);
        }
      })();
    `;

    const scriptFunction = new Function('sdk', wrappedCode);
    const result = scriptFunction(sdk);

    if (!result) {
      throw new Error('脚本执行未返回任何有效的生命周期对象，请确保最后有 return { onLoad, onUnload }');
    }

    const lifecycle: AAMSLifecyleObject = result;
    activeModules.set(moduleId, lifecycle);
    
    addLog('success', '脚本编译成功！准备执行挂载钩子...');

    // 3. 执行 onLoad 生命周期挂载
    if (lifecycle.onLoad) {
      try {
        lifecycle.onLoad(sdk);
        addLog('success', '✅ onLoad 挂载完成，AI 模块已成功激活运行');
      } catch (err: any) {
        addLog('error', `onLoad 执行抛出异常: ${err.message || err}`);
        // 挂载出错，自动执行卸载以清理现场
        unloadModule(moduleId, sdk, addLog);
        return false;
      }
    } else {
      addLog('warn', '模块未定义 onLoad 挂载钩子，静默启用');
    }

    return true;
  } catch (err: any) {
    const errorMsg = `模块加载失败: ${err.message || err}`;
    addLog('error', errorMsg);
    // UI 反馈
    window.dispatchEvent(new CustomEvent('aams-toast', {
      detail: { message: errorMsg, type: 'error' }
    }));
    return false;
  }
}

/**
 * 卸载指定的 AI 动态模块，彻底清除 DOM 标记和解绑事件
 * @param moduleId 模块唯一标识符
 * @param sdk 专有 SDK 实例
 * @param addLog 日志追加器函数
 */
export function unloadModule(
  moduleId: string,
  sdk: AIScriptSDK,
  addLog: (level: 'info' | 'success' | 'warn' | 'error', msg: string) => void
): void {
  const lifecycle = activeModules.get(moduleId);
  
  // 1. 调用脚本本身的 onUnload 钩子进行主动释放
  if (lifecycle && lifecycle.onUnload) {
    try {
      addLog('info', '正在调用脚本内定义的 onUnload 清理钩子...');
      lifecycle.onUnload(sdk);
      addLog('info', '脚本自主清理完毕');
    } catch (err: any) {
      addLog('warn', `脚本 onUnload 清理时抛出异常 (已忽略，将由底层兜底回收): ${err.message || err}`);
    }
  }

  // 2. 底层超级垃圾回收 (Garbage Collection) 兜底：一键清除所有带有该模块 data-aams-id 标记的 DOM 元素
  try {
    const elementsToCleanup = document.querySelectorAll(`[data-aams-id="${moduleId}"]`);
    if (elementsToCleanup.length > 0) {
      addLog('info', `正在执行底层安全垃圾回收，回收受污染 DOM 节点数: ${elementsToCleanup.length}`);
      elementsToCleanup.forEach(el => {
        // 如果是高亮包装的 span，需要将文本内容还原，否则直接 remove 会导致部分文本缺失
        // 比如：<span data-aams-id="xxx">123</span>，应该用 123 文本替换 span
        if (el.tagName.toLowerCase() === 'span' && el.classList.contains('bg-yellow-500/20') || el.hasAttribute('data-aams-id')) {
          // 如果只是插入的全新 DOM，直接 remove 即可；如果是高亮的 span，可用子节点替换它自身
          const parent = el.parentNode;
          if (parent) {
            // 如果它含有子元素/纯文本且父级并非 body 或者是我们插入的直接父级
            // 对于高亮，通常是个 span 包含文本
            if (el.classList.contains('bg-yellow-500/20') || el.classList.contains('text-yellow-300')) {
              const textNode = document.createTextNode(el.textContent || '');
              parent.replaceChild(textNode, el);
            } else {
              el.remove();
            }
          }
        } else {
          el.remove();
        }
      });
      addLog('success', '🧹 底层垃圾回收完成，DOM 环境已完美还原');
    }
  } catch (err: any) {
    addLog('error', `垃圾回收执行失败: ${err.message || err}`);
  }

  // 3. 从活动字典中移除
  activeModules.delete(moduleId);
  addLog('success', '❌ 模块已成功卸载并停用');
}
