package com.soar.sparkai.feature.ai.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.soar.sparkai.core.log.AppLogger
import java.io.File

/**
 * 屏幕截图自动寻回加载辅助器
 *
 * 作用：优先查询 Android 系统 MediaStore 公共数据库中 DISPLAY_NAME 类似 "SparkAI_"
 * 开头的最新一张相册图片，若数据库检索失败，智能降级从 App 本地私有 Pictures 目录进行检索。
 * 一键将检索到的截图解析为 Bitmap & Uri，彻底免除用户繁琐的文件查找流程。
 */
object ScreenshotLoader {

    /**
     * 自动载入最新的一张 SparkAI 截图图片
     *
     * @return 包含 Uri 与 Bitmap 的键值对，若无有效截图则返回 null
     */
    fun loadLatestSparkScreenshot(context: Context): Pair<Uri, Bitmap>? {
        AppLogger.i("ScreenshotLoader", "开始检索由 SparkAI 生成的最新截图...")
        val resolver = context.contentResolver

        // 1. 优先通过 MediaStore 公共影音库查询最新图片
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )

        // 筛选名字类似 SparkAI_ 开头的图片
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("SparkAI_%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            val cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    AppLogger.i("ScreenshotLoader", "成功通过 MediaStore 匹配到最新截图: $name, Uri: $contentUri")
                    
                    // 通过 ContentResolver 将 Uri 安全转码为内存 Bitmap
                    resolver.openInputStream(contentUri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            return Pair(contentUri, bitmap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ScreenshotLoader", "MediaStore 库检索失败，错误: ${e.message}", e)
        }

        // 2. 降级自愈方案：从 App 的外部私有图片目录检索最新文件 (Android 9级以下或MediaStore未同步时)
        try {
            val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (imagesDir != null && imagesDir.exists()) {
                val files = imagesDir.listFiles { _, name -> 
                    name.startsWith("SparkAI_") && name.endsWith(".png") 
                }
                if (!files.isNullOrEmpty()) {
                    // 按物理修改时间进行降序排列，拿最新的一个
                    val latestFile = files.maxByOrNull { it.lastModified() }
                    if (latestFile != null) {
                        AppLogger.i("ScreenshotLoader", "成功通过私有物理目录检索到最新截图: ${latestFile.name}")
                        val bitmap = BitmapFactory.decodeFile(latestFile.absolutePath)
                        if (bitmap != null) {
                            val uri = Uri.fromFile(latestFile)
                            return Pair(uri, bitmap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ScreenshotLoader", "外部私有物理目录检索异常: ${e.message}", e)
        }

        AppLogger.w("ScreenshotLoader", "相册及本地目录中暂未发现由悬浮球截取的任何 SparkAI 截图图片。")
        return null
    }
}
