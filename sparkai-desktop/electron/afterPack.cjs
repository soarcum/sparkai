const fs = require('fs')
const path = require('path')

/**
 * electron-builder 打包后的后置处理器
 * 自动剔除 locales 目录中除中文 (zh-CN.pak) 以外的所有多国语言包，大幅精简应用体积
 */
exports.default = async function (context) {
  const localesDir = path.join(context.appOutDir, 'locales')
  
  if (fs.existsSync(localesDir)) {
    try {
      const files = fs.readdirSync(localesDir)
      let deletedCount = 0
      
      for (const file of files) {
        // 只保留中文包 (zh-CN.pak)，其余国家语言包全部物理剔除
        if (file !== 'zh-CN.pak') {
          fs.unlinkSync(path.join(localesDir, file))
          deletedCount++
        }
      }
      console.log(`\n[afterPack Hook] 成功精简多语言：已物理剔除 ${deletedCount} 个无用本地化 pak，只保留中文(zh-CN)包。`)
    } catch (err) {
      console.error('[afterPack Hook] 精简 locales 时发生异常:', err.message)
    }
  }
}
