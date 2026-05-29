---
description: comit&build
---

1.提交代码
2.运行 `.\bump_and_push.ps1` 升版本并触发 CI/CD release
3.监听action,出现错误,修正,直到打包成功

注意：
- 默认只打包 Android 端（tag push 自动触发）
- 桌面端打包需先询问用户是否需要，再手动触发 workflow_dispatch
- 桌面端构建目前存在问题，暂不自动触发