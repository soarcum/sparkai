# SparkAI 多端协作智能系统

欢迎使用 **SparkAI**。这是一个结合了 Android 原生客户端与轻量级桌面网桥的多端协作系统。

> [!IMPORTANT]
> ### 🚨 终极核心开发原则（红线规范）
> 1. **APP 端（Android 客户端）是唯一的“主开发战场”**：所有核心业务功能（如 AI 极速流式对话、自定义 Prompt、常驻屏幕助手、无线麦克风投音、以及未来的 AI 自动模块动态加载）均在手机 APP 端进行。
> 2. **桌面端（Windows 客户端）仅为“局域网传输网桥”**：桌面端主要负责与手机端进行极速连接、局域网高速文件/文本互传、以及承载手机投送过来的无线音频播放，**不做任何核心业务的迭代开发**。
> 3. **AI 自动模块与脚本生成开发，未来必须 100% 在 APP 端承载**：为了让 AI 能够直接在手机 APP 上生成动态功能（如在页面上累加统计传输文件数字的和、高亮关键词等），手机端将向大模型暴露一组标准的原生桥接 APIs（如下文所列）。

---

## 📱 手机端 AI 自动模块系统 (AAMS) 大模型 API 规范

未来，为了让大模型能直接为手机端 APP 编写并自动加载动态模块，手机端将基于 Android 原生/Compose 环境向 AI 暴露以下 **Standard SparkAI-App SDK**。

任何 AI 大模型在为手机端自动生成功能模块时，必须且只能使用这套接口：

### 1. 动态 UI 注入接口 (`app.ui`)
- `app.ui.insertText(pageId: String, containerTag: String, text: String, colorHex: String): Unit`
  - **用途**：在指定页面（如 `FileTransfer` 局域网传输页）的特定组件或文字后面，追加显示自定义字符串。
  - **经典测试用例（显示数字的和）**：
    ```javascript
    // AI 自动生成的数字累加求和模块
    const items = app.data.getTransferHistory();
    let totalBytes = 0;
    items.forEach(item => { totalBytes += item.size; });
    const totalMB = (totalBytes / (1024 * 1024)).toFixed(2) + " MB";
    // 动态写入页面上
    app.ui.insertText("FileTransfer", "transfer_record_title", " (总大小: " + totalMB + ")", "#00B894");
    ```
- `app.ui.createDynamicCard(title: String, markdownContent: String, pageId: String): Unit`
  - **用途**：在指定的手机页面上，动态注入一张用 Markdown 渲染的极客风交互卡片。
- `app.ui.showToast(message: String, level: String): Unit`
  - **用途**：在手机屏幕下方弹出精美通知（level 可为 `info` | `success` | `warn` | `error`）。

### 2. 核心数据订阅接口 (`app.data`)
- `app.data.getTransferHistory(): List<TransferItem>`
  - **用途**：获取当前的局域网互传任务历史记录。
  - **数据结构**：每个 `TransferItem` 包含：
    - `id: String` (任务ID)
    - `name: String` (文件名或文本内容)
    - `size: Long` (数值大小，单位：字节)
    - `type: String` (上传/下载)
    - `status: String` (传输状态)
- `app.data.getNetworkStats(): NetworkStats`
  - **用途**：获取当前的局域网网桥延迟、实时传输速率等。

### 3. 多模态 AI 破译接口 (`app.ai`)
- `async app.ai.chat(prompt: String, text: String): Promise<String>`
  - **用途**：调用手机端已内置的小米 MiMo 大模型（如 `mimo-v2.5-pro`），对给定的文本根据 Prompt 进行分析并返回。
- `async app.ai.extractVision(imageBytes: ByteArray, prompt: String): Promise<String>`
  - **用途**：调用多模态视觉大模型，对屏幕截图或传入的图片字节流进行破译分析。

### 4. 系统底层桥接接口 (`app.system`)
- `app.system.setFloatWindowEnabled(enable: Boolean): Unit`
  - **用途**：动态开启或禁用手机端的“常驻屏幕助手悬浮窗”。
- `app.system.vibrate(durationMs: Long): Unit`
  - **用途**：触发真机马达轻微震动。

---

## 📂 项目结构概览

```
sparkai/
├── app/                              # 📱 核心开发战场：手机 APP 端 (Android 原生 / Jetpack Compose)
│   └── src/main/java/com/soar/sparkai/
│       ├── core/                     # 系统底层通信、音频与核心引擎
│       └── feature/                  # 各大核心业务特性
│           ├── ai/                   # 小米 MiMo 大模型对话与多模态破译
│           ├── transfer/             # 局域网高速传输页与实时投音 (求和功能已在此集成测试)
│           └── floatwindow/          # 常驻屏幕助手悬浮窗
└── sparkai-desktop/                  # 💻 局域网网桥辅助工具：电脑桌面端 (Electron + Vue 3 + Vite)
```

---

## 🔬 本地极速无线测试规范 (对标 CLAUDE.md)

为了防止直接推送到 GitHub 触发漫长的打包，所有开发者和 AI 助手在开发手机 APP 端时，必须使用本地集成工具 `test_helper.py` 执行极速调试闭环：

1. **无线连接**：确保手机开启“无线调试”并获取 `IP:PORT`，运行 `python test_helper.py connect <IP:PORT>` 建立 adb 桥接。
2. **打包安装**：修改完手机端 Kotlin/Compose 代码后，在根目录下运行 `python test_helper.py deploy` 执行本地 Gradle 增量打包并无线推送到真机拉起。
3. **截图验证**：运行 `python test_helper.py screenshot` 自动截取真机当前画面并在聊天窗口中展示，确认 UI 像素与显示无误。
