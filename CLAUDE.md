# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## 5. 构建与发布策略

**默认只打包 Android 端。桌面端打包需先询问用户。**

- 执行 `.\bump_and_push.ps1` 时，默认只触发 Android CI/CD（tag push 触发）
- 桌面端构建工作流（build-desktop.yml）已改为仅 `workflow_dispatch`（手动触发）
- 如需打包桌面端，先询问用户确认，再手动触发工作流
- 桌面端构建目前存在问题（electron-builder 在 CI 环境持续失败），需进一步排查

## 6. 本地无线调试与功能验证规范

当新功能完成或 Bug 修复后，**严禁**直接推送到 GitHub 触发漫长的打包。必须使用本地无线调试工具进行秒级部署与自动截图验证：

### ⚙️ 调试辅助脚本：`test_helper.py`
项目根目录下提供了本地联调的集成工具 `test_helper.py`：
- **连接设备**：`python test_helper.py connect <IP:PORT>` (如连接 `192.168.110.32:40445`)
- **一键打包部署**：`python test_helper.py deploy` (一键执行 Gradle 本地增量打包、无线推送安装至真机并自动拉起应用)
- **真机截图**：`python test_helper.py screenshot` (实时截取手机屏幕并拉取到项目根目录下，方便 AI 助手展示在聊天中进行 UI 验证)
- **拉取错误日志**：`python test_helper.py logcat` (实时拉取真机底层的崩溃与报错日志)

### 📋 AI 助手本地测试准则
1. **本地调试优先**：修改代码后，AI 助手必须在后台启动 `python test_helper.py deploy` 部署至用户的已连接手机，确保本地编译成功且没有运行 Crash。
2. **提交成果前提供截图**：功能开发完毕后，AI 助手必须主动运行 `python test_helper.py screenshot` 截取真机首屏/操作界面，并在对话中通过 Markdown 渲染展示给用户进行直观确认。
3. **遇到闪退主动排查**：若用户测试反馈闪退，AI 助手必须在第一时间内执行 `python test_helper.py logcat` 捕获报错堆栈并主动修复，严禁无声吞没错误。

