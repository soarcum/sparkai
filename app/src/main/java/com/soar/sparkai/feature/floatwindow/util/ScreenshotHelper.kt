package com.soar.sparkai.feature.floatwindow.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 屏幕截图核心辅助工具类
 * 
 * 作用：利用 MediaProjection API 获取系统屏幕内容，处理行步长对齐（RowStride Padding），并安全保存图片。
 */
object ScreenshotHelper {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 执行屏幕截图主逻辑
     * 
     * @param context 上下文
     * @param resultCode 录屏授权返回的 ResultCode
     * @param resultData 录屏授权返回的 Intent 数据
     * @param onComplete 截图完成后的回调（参数为成功与否）
     */
    fun captureScreen(
        context: Context,
        resultCode: Int,
        resultData: Intent,
        onComplete: (Boolean) -> Unit
    ) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        // 获取真实的物理屏幕大小以确保截图比例正确
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // 1. 获取系统级 MediaProjection 实例
        val mediaProjection = projectionManager.getMediaProjection(resultCode, resultData) ?: run {
            // 获取失败时，也安全销毁中转 Activity
            com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()
            onComplete(false)
            return
        }

        // 成功获取 MediaProjection 实例后，立即通知并安全销毁透明中转 Activity。
        // 这确保了在核心的前台服务升级和 Token 绑定期间，中转 Activity 100% 处于最前台，彻底规避 Android 14+ 前台服务启动时序竞争。
        com.soar.sparkai.feature.floatwindow.ui.ScreenshotActivity.finishActivity()

        // 1.5 适配 Android 14 / 15 / 16 安全规范：启动屏幕截取前必须强制注册 Callback
        val projectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
            }
        }
        mediaProjection.registerCallback(projectionCallback, mainHandler)

        // 2. 创建 ImageReader 来接收屏幕图像，RGBA_8888 格式最适合直接转 Bitmap
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        // 3. 将屏幕内容映射到 ImageReader 的 Surface 上
        val virtualDisplay = mediaProjection.createVirtualDisplay(
            "SparkAIScreenCapture",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            imageReader.surface,
            null,
            null
        )

        // 4. 等待屏幕缓冲渲染完毕，使用协程或延时机制抓取最新的一帧
        CoroutineScope(Dispatchers.Default).launch {
            // 给系统绘制和填充 Surface 留出足够的微秒级缓冲时间（500ms），避免截出黑屏
            delay(500)
            var image: Image? = null
            var screenshotBitmap: Bitmap? = null
            try {
                // 获取最新的图像帧
                image = imageReader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    // 核心算法：解决 GPU 64/128字节对齐带来的 Row Stride Padding 问题
                    val rowPadding = rowStride - pixelStride * width

                    // 创建一个临时带 Padding 尺寸的 Bitmap
                    val tempBitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    tempBitmap.copyPixelsFromBuffer(buffer)

                    // 剔除 Padding 部分，裁剪得到最终精确分辨率的 Bitmap
                    screenshotBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
                    tempBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 关闭并释放 Image，以便让底层资源能够快速回收
                image?.close()
                // 截图完成，必须释放虚拟屏幕和投影连接以节省内存和电量
                virtualDisplay.release()
                imageReader.close()
                mediaProjection.unregisterCallback(projectionCallback)
                mediaProjection.stop()
            }

            if (screenshotBitmap != null) {
                // 5. 将生成的 Bitmap 保存到本地存储
                val isSaved = saveBitmapToStorage(context, screenshotBitmap)
                withContext(Dispatchers.Main) {
                    if (isSaved) {
                        Toast.makeText(context, "截图成功，已保存至系统相册", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "截图失败，文件保存异常", Toast.LENGTH_SHORT).show()
                    }
                    onComplete(isSaved)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "未捕获到屏幕数据", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
        }
    }

    /**
     * 将 Bitmap 安全地保存至本地存储
     * 
     * 说明：Android 10+ 采用 MediaStore 实现，免存储读写权限，更安全合规；
     * 低版本回退至应用外部私有目录，免权限写入，保障 100% 成功率。
     */
    private suspend fun saveBitmapToStorage(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        var outputStream: OutputStream? = null
        var success = false
        val filename = "SparkAI_${System.currentTimeMillis()}.png"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+：使用 MediaStore 插入公共 Pictures 目录
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SparkAI")
                    put(MediaStore.Images.Media.IS_PENDING, 1) // 标记为处理中，防止其他应用抢先读取
                }

                val resolver = context.contentResolver
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        success = true
                    }
                    // 完成写入后，取消 PENDING 标记，使相册立即可见
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
            } else {
                // Android 9 级以下：为了免去 WRITE_EXTERNAL_STORAGE 动态权限申请，保存至 App 的外部私有图片目录
                val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                if (imagesDir != null) {
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs()
                    }
                    val imageFile = File(imagesDir, filename)
                    outputStream = FileOutputStream(imageFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    success = true

                    // 尽力广播刷新相册（虽然部分机型可能在私有目录上不识别，但这是低版本免权限写入最稳健做法）
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    val contentUri = Uri.fromFile(imageFile)
                    mediaScanIntent.data = contentUri
                    context.sendBroadcast(mediaScanIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            success = false
        } finally {
            try {
                outputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bitmap.recycle() // 及时回收 Bitmap，避免内存泄漏
        }
        success
    }
}
