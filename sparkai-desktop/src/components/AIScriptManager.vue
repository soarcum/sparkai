<template>
  <div class="flex-1 flex flex-col space-y-6 overflow-hidden">
    <!-- 顶栏欢迎卡片与操作中心 -->
    <section class="p-6 rounded-2xl border border-brand-border bg-gradient-to-br from-brand-primary/15 via-brand-secondary/5 to-transparent relative overflow-hidden shrink-0">
      <div class="relative z-10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div class="space-y-2">
          <div class="flex items-center space-x-2">
            <svg class="w-6 h-6 text-brand-secondary neon-text-cyan animate-pulse animate-duration-3000" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 21l8.982-11.725h-5.228l.836-5.092L4.5 15.904h5.313z" />
            </svg>
            <h2 class="text-xl font-bold tracking-tight bg-gradient-to-r from-white to-gray-300 bg-clip-text text-transparent">AI 自动模块魔盒 (AAMS)</h2>
          </div>
          <p class="text-xs text-brand-textMuted max-w-xl leading-relaxed">
            零代码开发新纪元。只需输入自然语言，大模型即可为您撰写专属 JS 脚本。开启后即可动态操纵网页 DOM，关闭时底层自动进行完美垃圾回收。
          </p>
        </div>

        <div class="flex items-center space-x-3 self-stretch md:self-auto shrink-0">
          <button @click="showConfigModal = true" class="px-4 py-2 text-xs font-semibold rounded-xl border border-white/10 hover:border-brand-secondary/40 bg-white/5 hover:bg-brand-secondary/10 text-brand-textMuted hover:text-white transition-all duration-300 flex items-center space-x-2">
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span>配置 AI 密钥</span>
          </button>
          
          <button @click="openCreateModal" class="px-5 py-2 text-xs font-semibold rounded-xl bg-gradient-to-r from-brand-secondary to-brand-primary text-white hover:shadow-glow-cyan transition-all duration-300 flex items-center space-x-2">
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>AI 生成模块</span>
          </button>
        </div>
      </div>
      <div class="absolute -right-10 -bottom-10 w-40 h-40 bg-brand-primary/10 rounded-full blur-3xl"></div>
    </section>

    <!-- 沙箱测试与模块列表混合区 -->
    <div class="flex-1 flex gap-6 overflow-hidden">
      <!-- 左侧：模块列表面板 -->
      <div class="flex-1 flex flex-col space-y-4 overflow-y-auto pr-1">
        <div class="text-xs font-semibold text-brand-textMuted tracking-wider uppercase flex items-center justify-between">
          <span>我的模块列表 ({{ modulesList.length }})</span>
          <span v-if="modulesList.length === 0" class="text-[10px] text-brand-primary border border-brand-primary/30 px-1.5 py-0.5 rounded bg-brand-primary/5 select-none animate-pulse">待生成</span>
        </div>

        <!-- 模块空状态 -->
        <div v-if="modulesList.length === 0" class="flex-1 min-h-[300px] border border-dashed border-brand-border rounded-2xl flex flex-col items-center justify-center space-y-4 p-8 text-center bg-white/[0.01]">
          <div class="w-14 h-14 rounded-full bg-white/5 flex items-center justify-center border border-white/10">
            <svg class="w-7 h-7 text-brand-textMuted" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <div class="space-y-1">
            <h4 class="text-sm font-bold text-white">暂无可用的 AI 自动模块</h4>
            <p class="text-xs text-brand-textMuted max-w-sm">点击右上角“AI 生成模块”或使用下方的“一键测试沙箱”来呼叫 AI 魔法师撰写您的第一个自动化交互模块！</p>
          </div>
        </div>

        <!-- 模块网格 -->
        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-for="mod in modulesList" :key="mod.id" 
               :class="[mod.enabled ? 'border-brand-secondary/40 shadow-glow-cyan/5 bg-gradient-to-b from-brand-secondary/5 to-transparent' : 'border-brand-border bg-white/[0.01]']"
               class="p-5 rounded-2xl border glass-panel flex flex-col justify-between space-y-4 transition-all duration-300 hover:translate-y-[-2px]">
            
            <!-- 卡片头部 -->
            <div class="space-y-1.5">
              <div class="flex items-center justify-between">
                <span class="font-bold text-sm tracking-wide text-white flex items-center space-x-1.5">
                  <span class="w-1.5 h-1.5 rounded-full" :class="[mod.enabled ? 'bg-brand-secondary dot-pulse shadow-glow-cyan' : 'bg-brand-textMuted']"></span>
                  <span>{{ mod.name }}</span>
                </span>
                
                <!-- 科技感开关 Switch -->
                <button @click="toggleModule(mod.id)" 
                        :class="[mod.enabled ? 'bg-brand-secondary/20 border-brand-secondary/50 text-brand-secondary' : 'bg-white/5 border-white/10 text-brand-textMuted']"
                        class="w-12 h-6 rounded-full border p-0.5 flex items-center transition-all duration-300 relative">
                  <div :class="[mod.enabled ? 'translate-x-6 bg-brand-secondary shadow-glow-cyan' : 'translate-x-0 bg-brand-textMuted']"
                       class="w-4.5 h-4.5 rounded-full transition-transform duration-300"></div>
                </button>
              </div>
              <p class="text-xs text-brand-textMuted leading-relaxed line-clamp-2 h-8">{{ mod.description }}</p>
            </div>

            <!-- 卡片底部操作与调试 -->
            <div class="flex items-center justify-between pt-3 border-t border-brand-border/40 text-[11px] shrink-0">
              <span class="text-brand-textMuted font-mono">ID: {{ mod.id }}</span>
              <div class="flex space-x-3.5">
                <button @click="openDrawer(mod)" class="text-brand-secondary hover:text-white transition-colors flex items-center space-x-1">
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  <span>代码与日志</span>
                </button>
                <button @click="deleteModule(mod.id)" class="text-red-400/80 hover:text-red-400 transition-colors flex items-center space-x-1">
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  <span>删除</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：内建 DOM 一键沙箱测试面板 -->
      <aside class="w-80 border border-brand-border glass-panel rounded-2xl p-5 flex flex-col space-y-4 shrink-0 overflow-y-auto">
        <div class="text-xs font-semibold text-brand-secondary tracking-wider uppercase flex items-center space-x-1.5 shrink-0">
          <svg class="w-4 h-4 text-brand-secondary animate-pulse" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
          </svg>
          <span>局部一键调试沙箱</span>
        </div>
        
        <p class="text-[11px] text-brand-textMuted leading-relaxed">
          此处内置了一块测试区域。您可以开启对应的 AI 自动模块，直接在下面实时看到修改、求和累加和高亮效果。
        </p>

        <!-- 测试数字区域 -->
        <div class="p-4 rounded-xl border border-brand-border bg-black/40 space-y-3">
          <div class="text-xs font-bold text-white border-b border-brand-border pb-1.5 flex justify-between items-center">
            <span>🧮 静态数字测试库</span>
            <span class="text-[9px] bg-brand-secondary/15 text-brand-secondary px-1.5 rounded uppercase font-mono">DOM NODE</span>
          </div>
          <div class="space-y-2 text-xs">
            <div class="flex items-center justify-between text-brand-textMuted">
              <span>核心任务耗时：</span>
              <span class="test-num font-mono text-white font-semibold">145.50</span>
            </div>
            <div class="flex items-center justify-between text-brand-textMuted">
              <span>系统冗余缓存：</span>
              <span class="test-num font-mono text-white font-semibold">89.20</span>
            </div>
            <div class="flex items-center justify-between text-brand-textMuted">
              <span>局域网网桥延迟：</span>
              <span class="test-num font-mono text-white font-semibold">18.00</span>
            </div>
          </div>
        </div>

        <!-- 测试文本区域 -->
        <div class="p-4 rounded-xl border border-brand-border bg-black/40 space-y-2">
          <div class="text-xs font-bold text-white border-b border-brand-border pb-1.5 flex justify-between items-center">
            <span>📖 圈词高亮与翻译测试库</span>
            <span class="text-[9px] bg-brand-primary/15 text-brand-primary px-1.5 rounded uppercase font-mono">DOM TEXT</span>
          </div>
          <!-- 英文和带 SparkAI 关键词的测试段落 -->
          <p class="text-xs text-brand-textMuted leading-relaxed select-text translate-target">
            Welcome to the SparkAI background agent interface. This is a local network bridge environment.
          </p>
          <p class="text-xs text-brand-textMuted leading-relaxed select-text translate-target">
            AI can read this text block, auto highlight key components and translate English text in real time.
          </p>
        </div>

        <div class="rounded-xl border border-brand-border p-3.5 bg-brand-secondary/5 flex flex-col space-y-2 shrink-0">
          <span class="text-[10px] text-brand-secondary font-bold tracking-widest uppercase">💡 一键求和模块测试教程</span>
          <p class="text-[10px] text-brand-textMuted leading-normal">
            1. 点击右上角“AI生成模块”。<br/>
            2. 在弹窗里点击下方的<b>“求和模块一键填入”</b>快捷需求。<br/>
            3. 点击“魔法生成”，生成完毕后在列表中开启它。<br/>
            4. 观察左侧测试库的第三个数字后是否瞬间浮现出了<b>(求和总计: 252.70)</b>！
          </p>
        </div>
      </aside>
    </div>

    <!-- ========================================== -->
    <!-- 弹窗 A: AI 密钥核心参数配置弹窗 -->
    <!-- ========================================== -->
    <div v-if="showConfigModal" class="fixed inset-0 bg-black/70 backdrop-blur-md flex items-center justify-center z-50 animate-fade-in p-4">
      <div class="w-full max-w-md rounded-2xl border border-brand-border glass-panel p-6 space-y-5 animate-scale-in relative">
        <div class="flex items-center justify-between border-b border-brand-border pb-3">
          <h3 class="text-sm font-bold text-white flex items-center space-x-2">
            <svg class="w-4 h-4 text-brand-secondary" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            </svg>
            <span>AI 大模型核心配置参数</span>
          </h3>
          <button @click="showConfigModal = false" class="text-brand-textMuted hover:text-white transition-colors">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div class="space-y-4 text-xs">
          <!-- API Key -->
          <div class="space-y-1.5">
            <label class="text-brand-textMuted font-medium">大模型 API Key / Token</label>
            <input type="password" v-model="aiConfig.apiKey" placeholder="输入您的大模型 API 密钥" 
                   class="w-full px-3 py-2.5 rounded-xl border border-white/10 bg-black/40 text-white outline-none focus:border-brand-secondary/60 focus:bg-black/60 transition-all font-mono" />
          </div>

          <!-- API URL -->
          <div class="space-y-1.5">
            <label class="text-brand-textMuted font-medium">API Base URL 地址</label>
            <input type="text" v-model="aiConfig.baseUrl" placeholder="https://token-plan-cn.xiaomimimo.com/v1" 
                   class="w-full px-3 py-2.5 rounded-xl border border-white/10 bg-black/40 text-white outline-none focus:border-brand-secondary/60 focus:bg-black/60 transition-all font-mono" />
          </div>

          <!-- Model -->
          <div class="space-y-1.5">
            <label class="text-brand-textMuted font-medium">大模型选择 (Model Name)</label>
            <input type="text" v-model="aiConfig.model" placeholder="mimo-v2.5-pro" 
                   class="w-full px-3 py-2.5 rounded-xl border border-white/10 bg-black/40 text-white outline-none focus:border-brand-secondary/60 focus:bg-black/60 transition-all font-mono" />
          </div>
        </div>

        <div class="pt-2 flex justify-end">
          <button @click="showConfigModal = false" class="px-5 py-2.5 rounded-xl bg-brand-secondary text-white font-semibold hover:shadow-glow-cyan text-xs transition-all duration-300">
            保存配置
          </button>
        </div>
      </div>
    </div>

    <!-- ========================================== -->
    <!-- 弹窗 B: 新建 AI 模块向导弹窗 -->
    <!-- ========================================== -->
    <div v-if="showCreateModal" class="fixed inset-0 bg-black/70 backdrop-blur-md flex items-center justify-center z-50 animate-fade-in p-4">
      <!-- 生成中极客代码雨动效蒙版 -->
      <div v-if="generating" class="w-full max-w-xl rounded-2xl border border-brand-secondary/40 bg-black p-8 flex flex-col items-center justify-center min-h-[350px] relative overflow-hidden z-50 shadow-glow-cyan/20">
        <!-- 满屏代码雨下滑模拟 -->
        <div class="absolute inset-0 opacity-10 font-mono text-[9px] text-brand-secondary select-none pointer-events-none overflow-hidden leading-tight">
          <div v-for="n in 12" :key="n" class="whitespace-nowrap animate-matrix-rain animate-duration-5000" :style="{ animationDelay: (n * 0.4) + 's', marginLeft: (n * 8) + '%' }">
            createSDKInstance(moduleId) { return { dom: { insertAfter: (t, h) => { console.log('inject DOM'); } } } };<br/>
            document.querySelectorAll('.test-num').forEach(el => { sum += parseFloat(el.innerText); });<br/>
            sdk.ui.toast("AI modules ready", "success"); onLoad(sdk); onUnload(sdk);<br/>
            function sandbox() { return { onLoad, onUnload }; }<br/>
            sdk.ai.analyzeText(originalText, "translate to chinese");<br/>
            [data-aams-id] { animation: pulse 2s infinite; }<br/>
            AAMSModuleExecutor.loadAndRunModule(m_id, scriptCode, sdk);
          </div>
        </div>
        
        <!-- 3D 立体加载波纹 -->
        <div class="relative w-24 h-24 mb-6">
          <div class="absolute inset-0 rounded-full border border-brand-secondary/30 animate-ping"></div>
          <div class="absolute inset-2 rounded-full border border-brand-secondary/50 animate-pulse animate-duration-1000"></div>
          <div class="absolute inset-0 flex items-center justify-center">
            <svg class="w-10 h-10 text-brand-secondary animate-spin animate-duration-3000" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 12c0-1.232-.046-2.453-.138-3.662a4.006 4.006 0 00-3.7-3.7 48.678 48.678 0 00-7.324 0 4.006 4.006 0 00-3.7 3.7C4.793 9.547 4.75 10.768 4.75 12s.043 2.453.138 3.662a4.006 4.006 0 003.7 3.7 48.656 48.656 0 007.324 0 4.006 4.006 0 003.7-3.7c.092-1.209.138-2.43.138-3.662z" />
            </svg>
          </div>
        </div>
        <h4 class="text-sm font-bold text-white select-none relative z-10 tracking-wider">MiMo 魔法大模型构思中...</h4>
        <p class="text-xs text-brand-textMuted mt-2 max-w-xs text-center select-none relative z-10 leading-relaxed font-mono">
          正在为您动态编译 SDK 上下文，并生成适配此 DOM 的 JavaScript 结构钩子...
        </p>
      </div>

      <!-- 常规新建向导表单 -->
      <div v-else class="w-full max-w-xl rounded-2xl border border-brand-border glass-panel p-6 space-y-5 animate-scale-in">
        <div class="flex items-center justify-between border-b border-brand-border pb-3">
          <h3 class="text-sm font-bold text-white flex items-center space-x-2">
            <svg class="w-4.5 h-4.5 text-brand-secondary animate-pulse" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 21l8.982-11.725h-5.228l.836-5.092L4.5 15.904h5.313z" />
            </svg>
            <span>呼叫 AI 魔法生成新自动模块</span>
          </h3>
          <button @click="showCreateModal = false" class="text-brand-textMuted hover:text-white transition-colors">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div class="space-y-4 text-xs">
          <!-- 模块命名与描述 -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label class="text-brand-textMuted font-medium">✨ 模块命名</label>
              <input type="text" v-model="newModuleName" placeholder="例如：数字求和助手" 
                     class="w-full px-3 py-2 rounded-lg border border-white/10 bg-black/40 text-white outline-none focus:border-brand-secondary/60 focus:bg-black/60 transition-all" />
            </div>
            <div class="space-y-1.5">
              <label class="text-brand-textMuted font-medium">📝 功能简述</label>
              <input type="text" v-model="newModuleDesc" placeholder="例如：自动累加页面数字并插入页面显示" 
                     class="w-full px-3 py-2 rounded-lg border border-white/10 bg-black/40 text-white outline-none focus:border-brand-secondary/60 focus:bg-black/60 transition-all" />
            </div>
          </div>

          <!-- 需求输入框 -->
          <div class="space-y-1.5">
            <label class="text-brand-textMuted font-medium">💬 用自然语言描述您的交互需求：</label>
            <textarea v-model="userPromptInput" rows="4" placeholder="例如：我想让我页面上的所有带有 class 为 test-num 的数值元素进行加和计算，并把最终结果以 (求和总计: xxx) 的括号形式拼接在最后一个数字元素的后面。" 
                      class="w-full px-3 py-2 rounded-lg border border-white/10 bg-black/40 text-white outline-none focus:border-brand-secondary/60 focus:bg-black/60 transition-all leading-relaxed resize-none"></textarea>
          </div>

          <!-- 快捷一键预填模板 -->
          <div class="space-y-1.5 shrink-0">
            <span class="text-brand-textMuted font-medium block">🎯 快捷测试预设模板（点击即预填）：</span>
            <div class="flex flex-wrap gap-2.5">
              <button v-for="tpl in presetTemplates" :key="tpl.title" @click="applyTemplate(tpl)"
                      class="px-2.5 py-1.5 rounded bg-white/5 border border-white/10 hover:border-brand-secondary/40 text-brand-secondary hover:bg-brand-secondary/5 cursor-pointer transition-colors text-[10px]">
                {{ tpl.title }}
              </button>
            </div>
          </div>
        </div>

        <div class="pt-2 flex justify-between items-center shrink-0">
          <span v-if="!aiConfig.apiKey" class="text-[10px] text-amber-400 font-bold">⚠️ 请先在右上角配置您的 AI 大模型 API Key</span>
          <span v-else class="text-[10px] text-brand-textMuted">⚡ AI 将自动生成纯 JS 代码段</span>
          
          <button @click="triggerAICodeGeneration" :disabled="!aiConfig.apiKey || !newModuleName || !userPromptInput"
                  :class="[!aiConfig.apiKey || !newModuleName || !userPromptInput ? 'opacity-40 cursor-not-allowed bg-white/10' : 'bg-gradient-to-r from-brand-secondary to-brand-primary hover:shadow-glow-cyan']"
                  class="px-5 py-2.5 rounded-xl text-white font-semibold text-xs transition-all duration-300 flex items-center space-x-1.5">
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 21l8.982-11.725h-5.228l.836-5.092L4.5 15.904h5.313z" />
            </svg>
            <span>呼叫 AI 魔法生成</span>
          </button>
        </div>
      </div>
    </div>

    <!-- ========================================== -->
    <!-- 抽屉 C: 代码与运行日志调试抽屉 -->
    <!-- ========================================== -->
    <div v-if="showDrawer" class="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 animate-fade-in flex justify-end">
      <div class="w-full max-w-4xl bg-black/90 border-l border-brand-border h-full shadow-2xl flex flex-col animate-slide-in p-6">
        <!-- 头部 -->
        <div class="flex items-center justify-between border-b border-brand-border pb-3.5 shrink-0">
          <div class="flex items-center space-x-2">
            <span class="w-2.5 h-2.5 rounded-full" :class="[selectedDrawerModule.enabled ? 'bg-brand-secondary dot-pulse shadow-glow-cyan' : 'bg-brand-textMuted']"></span>
            <h3 class="text-sm font-bold text-white">{{ selectedDrawerModule.name }} (代码与调试)</h3>
          </div>
          <button @click="closeDrawer" class="w-8 h-8 rounded-lg hover:bg-white/10 flex items-center justify-center text-brand-textMuted hover:text-white transition-colors">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <!-- 双栏：左侧代码编辑器，右侧日志调试台 -->
        <div class="flex-1 flex gap-5 min-h-0 py-5">
          <!-- 左栏：可编程的模拟极客代码编辑器 -->
          <div class="flex-1 flex flex-col space-y-2.5 min-h-0">
            <div class="text-[11px] font-semibold text-brand-textMuted tracking-wider uppercase flex justify-between items-center shrink-0">
              <span>💻 JS 代码段 (支持手动微调测试)</span>
              <span class="text-[9px] px-1.5 py-0.5 rounded bg-black text-brand-secondary border border-brand-secondary/20 font-mono">SANDBOX JS</span>
            </div>
            
            <div class="flex-1 rounded-xl border border-brand-border bg-black/60 flex overflow-hidden font-mono text-[11.5px] leading-relaxed relative min-h-0">
              <!-- 模拟行号 -->
              <div class="w-10 bg-white/[0.02] border-r border-white/5 py-3 text-right pr-2.5 text-brand-textMuted/40 select-none font-mono">
                <div v-for="n in 25" :key="n">{{ n }}</div>
              </div>
              
              <!-- 真实文本输入区 -->
              <textarea v-model="drawerCodeTemp" spellcheck="false" 
                        class="flex-1 py-3 px-3.5 bg-transparent text-gray-200 outline-none resize-none leading-relaxed overflow-y-auto w-full select-text"></textarea>
            </div>
            
            <button @click="applyCodeModification" class="w-full py-2 rounded-xl bg-brand-secondary/15 hover:bg-brand-secondary text-brand-secondary hover:text-white border border-brand-secondary/40 hover:border-transparent font-semibold text-xs transition-all duration-300 flex items-center justify-center space-x-1.5 shrink-0">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 12H18.5" />
              </svg>
              <span>重新编译并激活运行</span>
            </button>
          </div>

          <!-- 右栏：实时运行日志台 -->
          <div class="w-80 flex flex-col space-y-2.5 min-h-0 shrink-0">
            <div class="text-[11px] font-semibold text-brand-textMuted tracking-wider uppercase flex justify-between items-center shrink-0">
              <span>📜 模块运行控制台实时日志</span>
              <button @click="clearLogs" class="text-[10px] text-brand-textMuted hover:text-white transition-all">清除</button>
            </div>

            <div ref="drawerLogContainer" class="flex-1 rounded-xl border border-brand-border bg-black/80 font-mono text-[10.5px] p-3.5 overflow-y-auto space-y-2 min-h-0 select-text">
              <div v-for="(log, idx) in selectedDrawerModule.logs" :key="idx" class="flex space-x-2 leading-relaxed">
                <span class="text-brand-secondary/70 select-none">[{{ log.time }}]</span>
                <span :class="[log.level === 'success' ? 'text-emerald-400' : log.level === 'warn' ? 'text-amber-400' : log.level === 'error' ? 'text-red-400' : 'text-brand-secondary']" class="font-bold select-none shrink-0 uppercase">
                  {{ log.level }}
                </span>
                <span class="text-gray-300 break-all">{{ log.message }}</span>
              </div>
              <div v-if="!selectedDrawerModule.logs || selectedDrawerModule.logs.length === 0" class="text-brand-textMuted text-center pt-10 font-mono">
                暂无实时输出，开启后将监听事件
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { AAMSModule } from '../modules/ai-script/types'
import { 
  aiConfig, 
  modulesList, 
  initModules, 
  toggleModule, 
  deleteModule, 
  addModule, 
  updateModuleCode,
  clearModuleLogs
} from '../modules/ai-script/store'
import { generateModuleCode } from '../modules/ai-script/aiPrompt'

// 1. 各个模态窗及抽屉状态控制
const showConfigModal = ref(false)
const showCreateModal = ref(false)
const showDrawer = ref(false)
const generating = ref(false)

// 2. 模块生成表单状态
const newModuleName = ref('')
const newModuleDesc = ref('')
const userPromptInput = ref('')

// 3. 抽屉内调试选中的模块与临时代码
const selectedDrawerModule = ref<AAMSModule>({} as any)
const drawerCodeTemp = ref('')
const drawerLogContainer = ref<HTMLElement | null>(null)

// 4. 快捷一键预填模板定义
const presetTemplates = [
  {
    title: '🧮 自动累加求和模块',
    name: '数字求和助手',
    desc: '自动提取测试库中的三个数字，完成加总并在末尾动态显示',
    prompt: '我想让我页面上的所有带有 class 为 test-num 的数值元素进行加和计算，并把最终结果以 (求和总计: xxx) 的形式拼接在最后一个数字元素的后面。'
  },
  {
    title: '🌐 英文悬浮点击翻译',
    name: '英文悬浮翻译官',
    desc: '点击测试段落，调用 AI 进行极速翻译并以紫色气泡展示在下方',
    prompt: '帮我寻找页面上 class 为 translate-target 的段落元素，给它们加上点击事件。当点击时，调用 sdk.ai.analyzeText 将文本翻译为中文，并将翻译结果包裹在包含 "🤖 译文: " 字样的紫色气泡 DOM 元素中插入在该段落的下方。'
  },
  {
    title: '🏷️ SparkAI 关键词高亮',
    name: 'SparkAI 高亮器',
    desc: '自动搜索 translate-target 内的 SparkAI 词汇并以醒目的高亮形式圈出',
    prompt: '帮我搜索页面中所有带有 class 为 translate-target 的段落元素，将其中的 "SparkAI" 这一词汇使用黄色的高亮 span 标签圈出高亮展示。'
  }
]

// 应用快捷模板
const applyTemplate = (tpl: any) => {
  newModuleName.value = tpl.name
  newModuleDesc.value = tpl.desc
  userPromptInput.value = tpl.prompt
}

// 打开创建弹窗
const openCreateModal = () => {
  newModuleName.value = ''
  newModuleDesc.value = ''
  userPromptInput.value = ''
  showCreateModal.value = true
}

// 触发 AI 代码自动生成
const triggerAICodeGeneration = async () => {
  if (!newModuleName.value || !userPromptInput.value) return
  
  generating.value = true
  
  // 提取一下测试 DOM 结构作为上下文，提供给大模型进行 Few-shot 注入参考
  const testDomContext = `
    <!-- 静态数字测试库 -->
    <div class="space-y-2 text-xs">
      <span class="test-num">145.50</span>
      <span class="test-num">89.20</span>
      <span class="test-num">18.00</span>
    </div>
    <!-- 圈词高亮与翻译测试库 -->
    <p class="translate-target">Welcome to the SparkAI background agent interface. This is a local network bridge environment.</p>
    <p class="translate-target">AI can read this text block, auto highlight key components and translate English text in real time.</p>
  `.trim()

  try {
    const code = await generateModuleCode(
      userPromptInput.value,
      {
        apiKey: aiConfig.apiKey,
        baseUrl: aiConfig.baseUrl,
        model: aiConfig.model
      },
      testDomContext
    )
    
    // 创建新模块
    const mod = addModule(
      newModuleName.value,
      newModuleDesc.value || 'AI 魔法创建的自动化插件',
      userPromptInput.value,
      code
    )
    
    generating.value = false
    showCreateModal.value = false
    
    // 成功通知
    window.dispatchEvent(new CustomEvent('aams-toast', {
      detail: { message: `AI 模块 [${mod.name}] 生成成功！`, type: 'success' }
    }))
  } catch (error: any) {
    generating.value = false
    window.dispatchEvent(new CustomEvent('aams-toast', {
      detail: { message: `生成失败: ${error.message || error}`, type: 'error' }
    }))
  }
}

// 抽屉日志控制
const openDrawer = (mod: AAMSModule) => {
  selectedDrawerModule.value = mod
  drawerCodeTemp.value = mod.code
  showDrawer.value = true
  scrollToBottom()
}

const closeDrawer = () => {
  showDrawer.value = false
}

// 手动应用代码修改
const applyCodeModification = () => {
  if (selectedDrawerModule.value && selectedDrawerModule.value.id) {
    updateModuleCode(selectedDrawerModule.value.id, drawerCodeTemp.value)
    window.dispatchEvent(new CustomEvent('aams-toast', {
      detail: { message: '代码修改成功，已重新热加载运行！', type: 'success' }
    }))
  }
}

// 清除特定日志
const clearLogs = () => {
  if (selectedDrawerModule.value && selectedDrawerModule.value.id) {
    clearModuleLogs(selectedDrawerModule.value.id)
  }
}

// 自动滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (drawerLogContainer.value) {
      drawerLogContainer.value.scrollTop = drawerLogContainer.value.scrollHeight
    }
  })
}

// 监听日志更新事件
const handleLogUpdate = (e: any) => {
  if (selectedDrawerModule.value && e.detail && e.detail.moduleId === selectedDrawerModule.value.id) {
    scrollToBottom()
  }
}

onMounted(() => {
  initModules()
  window.addEventListener('aams-log-updated', handleLogUpdate)
})

onUnmounted(() => {
  window.removeEventListener('aams-log-updated', handleLogUpdate)
})
</script>

<style scoped>
/* 极客代码矩阵下坠流动画 */
@keyframes matrix-rain {
  0% {
    transform: translateY(-100%);
  }
  100% {
    transform: translateY(100%);
  }
}

.animate-matrix-rain {
  animation: matrix-rain linear infinite;
}

.animate-duration-3000 {
  animation-duration: 3000ms;
}
.animate-duration-5000 {
  animation-duration: 5000ms;
}
.animate-duration-1000 {
  animation-duration: 1000ms;
}

/* 渐变流光渐入动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.animate-fade-in {
  animation: fadeIn 0.2s ease-out forwards;
}
.animate-scale-in {
  animation: scaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.animate-slide-in {
  animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
</style>
