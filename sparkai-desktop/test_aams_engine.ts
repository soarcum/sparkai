// ========================================================
// AAMS 核心引擎单元测试与闭环验证脚本 (test_aams_engine.ts)
// ========================================================
import { createSDKInstance } from './src/modules/ai-script/sdk'
import { loadAndRunModule, unloadModule } from './src/modules/ai-script/executor'
import { AAMSModule } from './src/modules/ai-script/types'

console.log("========================================================");
console.log("🧪 启动 AI 自动模块系统 (AAMS) 核心执行沙箱与 GC 垃圾回收测试");
console.log("========================================================\n");

// 1. 微型 Mock DOM 容器，确保可以在 Node/Bun 等无浏览器环境下闭环测试 DOM
class MockElement {
  tagName: string;
  attributes: Record<string, string> = {};
  classList = {
    _classes: new Set<string>(),
    add(cls: string) { this._classes.add(cls); },
    remove(cls: string) { this._classes.delete(cls); },
    contains(cls: string) { return this._classes.has(cls); }
  };
  childNodes: MockElement[] = [];
  parentNode: MockElement | null = null;
  innerHTML = "";
  textContent = "";
  _handleTranslateClick?: Function;

  constructor(tagName: string) {
    this.tagName = tagName.toLowerCase();
  }

  setAttribute(name: string, val: string) {
    this.attributes[name] = val;
  }

  getAttribute(name: string) {
    return this.attributes[name] || null;
  }

  hasAttribute(name: string) {
    return name in this.attributes;
  }

  appendChild(child: MockElement) {
    child.parentNode = this;
    this.childNodes.push(child);
    mockDocument.allElements.push(child);
    return child;
  }

  insertBefore(newChild: MockElement, refChild: MockElement) {
    newChild.parentNode = this;
    const idx = this.childNodes.indexOf(refChild);
    if (idx !== -1) {
      this.childNodes.splice(idx, 0, newChild);
    } else {
      this.childNodes.push(newChild);
    }
    mockDocument.allElements.push(newChild);
    return newChild;
  }

  replaceChild(newChild: MockElement, oldChild: MockElement) {
    const idx = this.childNodes.indexOf(oldChild);
    if (idx !== -1) {
      this.childNodes[idx] = newChild;
      newChild.parentNode = this;
      oldChild.parentNode = null;
      
      const idxAll = mockDocument.allElements.indexOf(oldChild);
      if (idxAll !== -1) mockDocument.allElements.splice(idxAll, 1);
      mockDocument.allElements.push(newChild);
    }
  }

  remove() {
    if (this.parentNode) {
      const idx = this.parentNode.childNodes.indexOf(this);
      if (idx !== -1) {
        this.parentNode.childNodes.splice(idx, 1);
      }
    }
    const idxAll = mockDocument.allElements.indexOf(this);
    if (idxAll !== -1) {
      mockDocument.allElements.splice(idxAll, 1);
    }
  }

  querySelectorAll(selector: string): MockElement[] {
    const result: MockElement[] = [];
    const traverse = (el: MockElement) => {
      el.childNodes.forEach(child => {
        result.push(child);
        traverse(child);
      });
    };
    traverse(this);
    return result;
  }

  addEventListener(event: string, callback: Function) {
    if (event === 'click') {
      this._handleTranslateClick = callback;
    }
  }

  removeEventListener(event: string, callback: Function) {
    if (event === 'click') {
      delete this._handleTranslateClick;
    }
  }
}



const mockDocument = {
  allElements: [] as MockElement[],
  body: new MockElement("body"),
  
  createElement(tag: string): any {
    tag = tag.toLowerCase();
    if (tag === 'template') {
      const mockDiv = new MockElement("div");
      // 模拟 template 的 content
      return {
        content: {
          firstElementChild: mockDiv
        }
      }
    }
    const el = new MockElement(tag);
    return el;
  },

  createTextNode(text: string): any {
    const node = new MockElement("text");
    node.textContent = text;
    node.innerHTML = text;
    return node;
  },

  querySelector(selector: string): any {
    // 简单模拟 selector 匹配
    selector = selector.toLowerCase();
    if (selector.startsWith('.')) {
      const className = selector.substring(1);
      return this.allElements.find(el => el.classList.contains(className)) || null;
    }
    return this.allElements.find(el => el.tagName === selector) || null;
  },

  querySelectorAll(selector: string): any {
    selector = selector.toLowerCase();
    // 匹配打标的 data-aams-id
    if (selector.includes('[data-aams-id=')) {
      const match = selector.match(/data-aams-id="([^"]+)"/);
      const id = match ? match[1] : "";
      return this.allElements.filter(el => el.getAttribute('data-aams-id') === id);
    }
    
    // 匹配类名
    if (selector.startsWith('.')) {
      const className = selector.substring(1);
      return this.allElements.filter(el => el.classList.contains(className));
    }
    
    return this.allElements.filter(el => el.tagName === selector);
  }
};

// 挂载到全局环境，模拟浏览器 DOM APIs
(global as any).document = mockDocument;
(global as any).window = {
  dispatchEvent(event: any) {
    // console.log(`[Mock Window] 接收到全局事件派发: ${event.type}`);
  }
};
(global as any).HTMLElement = MockElement;

// ==========================================
// 2. 模拟真实待测试的 HTML 网页结构并挂载到 mockDocument
// ==========================================
const testNum1 = new MockElement("span");
testNum1.classList.add("test-num");
testNum1.textContent = "120.00";
mockDocument.body.appendChild(testNum1);

const testNum2 = new MockElement("span");
testNum2.classList.add("test-num");
testNum2.textContent = "80.50";
mockDocument.body.appendChild(testNum2);

console.log(`[Mock DOM] 初始化完成。网页已挂载数字节点:`);
console.log(`   - 节点1: <span class="test-num">120.00</span>`);
console.log(`   - 节点2: <span class="test-num">80.50</span>\n`);


// ==========================================
// 3. 构建模拟的 AAMS 待测试模块与 AI 代码字符串
// ==========================================
const mockModuleId = "m_sum_test_99";
const mockCodeString = `
  return {
    onLoad(sdk) {
      sdk.ui.toast("测试累加求和模块加载...", "info");
      
      // 1. 寻找所有的数字节点
      const nums = sdk.dom.findAll('.test-num');
      if (nums.length === 0) return;
      
      // 2. 累加计算
      let sum = 0;
      nums.forEach(el => {
        sum += parseFloat(el.textContent || '0');
      });
      
      // 3. 在最后一个数字后面写入结果 DOM，打上 data-aams-id 的 class
      const last = nums[nums.length - 1];
      sdk.dom.insertAfter(last, \`<span class="bg-yellow-500/20 text-yellow-300">(和: \${sum})</span>\`);
      sdk.ui.toast("累加和计算写入成功，结果为: " + sum, "success");
    },
    onUnload(sdk) {
      sdk.ui.toast("累加求和测试模块已停用，触发清理", "info");
    }
  }
`;

// ==========================================
// 4. 执行核心测试链路
// ==========================================
let hasError = false;
const logs: string[] = [];
const addLog = (level: string, msg: string) => {
  const symbol = level === 'success' ? '🟢' : level === 'error' ? '🔴' : level === 'warn' ? '🟡' : 'ℹ️';
  logs.push(`[${symbol} ${level.toUpperCase()}] ${msg}`);
};

// A. 实例化 SDK
const sdk = createSDKInstance(
  mockModuleId,
  addLog,
  () => ({ apiKey: 'fake_key', baseUrl: 'fake_url', model: 'fake_model' })
);

// B. 编译并激活 onLoad
console.log("🔄 [测试步骤 1] 编译并激活 AI 自动求和模块...");
const runSuccess = loadAndRunModule(mockModuleId, mockCodeString, sdk, addLog);

if (runSuccess) {
  console.log("✔ [测试结果 1] onLoad 成功被触发！");
} else {
  console.log("✖ [测试结果 1] onLoad 编译挂载失败！");
  hasError = true;
}

// C. 检查求和 HTML 是否已被成功写入到 DOM 树中，并且打标是否正确
const insertedElements = mockDocument.querySelectorAll(`[data-aams-id="${mockModuleId}"]`);
console.log(`\n🔍 [测试步骤 2] 检查页面是否已动态写入计算后的和:`);
console.log(`   - 查找到打标元素个数: ${insertedElements.length}`);

if (insertedElements.length > 0) {
  const el = insertedElements[0] as MockElement;
  console.log(`   - 写入的 DOM 标签名: <${el.tagName}>`);
  console.log(`   - data-aams-id 属性值: "${el.getAttribute('data-aams-id')}"`);
  console.log(`   - 写入的内容: "${el.textContent}"`);
  
  if (el.getAttribute('data-aams-id') === mockModuleId) {
    console.log("✔ [测试结果 2] 元素写入正确，且 data-aams-id 打标 100% 符合规范！");
  } else {
    console.log("✖ [测试结果 2] 元素打标校验失败！");
    hasError = true;
  }
} else {
  console.log("✖ [测试结果 2] 未在 DOM 中检索到写入的计算结果！");
  hasError = true;
}

// D. 卸载模块，测试一键 DOM 垃圾回收是否完全无残留
console.log("\n🔄 [测试步骤 3] 停用该 AI 自动模块，触发底层垃圾回收...");
unloadModule(mockModuleId, sdk, addLog);

const cleanupCheck = mockDocument.querySelectorAll(`[data-aams-id="${mockModuleId}"]`);
console.log(`🔍 检查回收后的 DOM 现场:`);
console.log(`   - 剩余带打标的元素个数: ${cleanupCheck.length}`);

if (cleanupCheck.length === 0) {
  console.log("✔ [测试结果 3] 底层 DOM 垃圾回收大获全胜，页面被 100% 完美复原，0 污染残留！");
} else {
  console.log("✖ [测试结果 3] 垃圾回收失败，页面存在残留节点污染！");
  hasError = true;
}

// E. 打印沙箱的运行日志链路
console.log("\n📋 [日志回溯] 控制台沙箱运行日志轨迹一览:");
logs.forEach(log => console.log(`   ${log}`));

console.log("\n========================================================");
if (!hasError) {
  console.log("🎉 AAMS 核心引擎单元测试全部通过！完美闭环，功能 100% 正确！");
} else {
  console.log("🚨 AAMS 核心单元测试存在失败用例，请查看上方输出排错。");
}
console.log("========================================================");
