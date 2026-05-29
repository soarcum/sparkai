package com.soar.sparkai.feature.transfer.model

/**
 * 局域网文件传输任务的状态数据模型
 */
data class TransferItem(
    val id: String,
    val name: String,
    val size: Long,
    val type: TransferType,
    val progress: Float = 0f,
    val status: TransferStatus = TransferStatus.WAITING,
    val speed: String = "0 KB/s",
    val error: String? = null,
    val timestamp: String = ""
)

enum class TransferType {
    UPLOAD,        // 手机发送给电脑
    DOWNLOAD,      // 电脑接收来自手机 (或手机下载电脑的要约)
    UPLOAD_TEXT,   // 手机发送文本给电脑
    DOWNLOAD_TEXT, // 手机下载/接收电脑端的文本
    UPLOAD_LINK,   // 手机发送链接给电脑
    DOWNLOAD_LINK  // 手机接收电脑端的链接
}

enum class TransferStatus {
    WAITING,       // 等待中
    TRANSFERRING,  // 传输中
    SUCCESS,       // 传输成功
    FAILED         // 传输失败
}
