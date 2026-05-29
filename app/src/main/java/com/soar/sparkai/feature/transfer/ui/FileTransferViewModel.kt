package com.soar.sparkai.feature.transfer.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soar.sparkai.feature.transfer.FileTransferManager
import com.soar.sparkai.feature.transfer.model.TransferItem
import com.soar.sparkai.feature.transfer.model.TransferStatus
import com.soar.sparkai.feature.transfer.model.TransferType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiscoveredDevice(
    val hostname: String,
    val ip: String,
    val port: Int
)

class FileTransferViewModel : ViewModel() {

    var ip by mutableStateOf("192.168.1.")
    var port by mutableStateOf("9090")
    var isConnected by mutableStateOf(false)
    var isConnecting by mutableStateOf(false)
    var transferList by mutableStateOf<List<TransferItem>>(emptyList())
    var activeOffer by mutableStateOf<TransferItem?>(null)
    var isAudioStreaming by mutableStateOf(false)

    private var connectionJob: Job? = null
    private var audioJob: Job? = null

    // 建立局域网长连接
    fun connect(context: Context) {
        if (isConnected || isConnecting) return
        
        // 本地前置校验，防止诸如 "192.168.1." 的半成品 IP 发起网络连接
        val trimmedIp = ip.trim()
        if (trimmedIp.isBlank() || trimmedIp.endsWith(".") || trimmedIp.split(".").size != 4) {
            android.widget.Toast.makeText(context, "⚠️ 请输入完整的电脑 IP 地址", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        isConnecting = true
        connectionJob = viewModelScope.launch {
            FileTransferManager.startSSEConnection(
                ip = trimmedIp,
                port = port.trim().toIntOrNull() ?: 9090,
                onOfferReceived = { id, name, size ->
                    viewModelScope.launch(Dispatchers.Main) {
                        activeOffer = TransferItem(
                            id = id,
                            name = name,
                            size = size,
                            type = TransferType.DOWNLOAD,
                            status = TransferStatus.WAITING,
                            timestamp = getCurrentTime()
                        )
                    }
                },
                onTextOfferReceived = { id, text, isUrl ->
                    // 自动将接收到的文本写入手机系统剪贴板
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("SparkAI Share", text)
                        clipboard.setPrimaryClip(clip)
                    } catch (e: Exception) {
                        // 忽略剪贴板写入异常
                    }

                    viewModelScope.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "📋 收到电脑分享的${if (isUrl) "链接" else "文本"}，已自动复制", android.widget.Toast.LENGTH_SHORT).show()
                    }

                    val newItem = TransferItem(
                        id = id,
                        name = text,
                        size = text.length.toLong(),
                        type = if (isUrl) TransferType.DOWNLOAD_LINK else TransferType.DOWNLOAD_TEXT,
                        status = TransferStatus.SUCCESS,
                        speed = "完成",
                        timestamp = getCurrentTime()
                    )
                    viewModelScope.launch(Dispatchers.Main) {
                        transferList = listOf(newItem) + transferList
                    }
                },
                onStatusChanged = { connected ->
                    viewModelScope.launch(Dispatchers.Main) {
                        isConnected = connected
                        if (connected) {
                            isConnecting = false
                            // 成功连接，保存本次 IP 和端口
                            saveLastConnection(context, trimmedIp, port.trim())
                            android.widget.Toast.makeText(context, "🎉 桥接电脑端成功！", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            isConnecting = false
                        }
                    }
                },
                onError = { err ->
                    viewModelScope.launch(Dispatchers.Main) {
                        isConnecting = false
                        android.widget.Toast.makeText(context, "❌ 连接失败: $err", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    // 断开连接
    fun disconnect() {
        stopAudioStreaming()
        FileTransferManager.stopSSEConnection()
        connectionJob?.cancel()
        connectionJob = null
        isConnected = false
        isConnecting = false
    }

    // 启动手机麦克风流式投射到电脑
    fun startAudioStreaming(context: Context) {
        if (isAudioStreaming) return
        audioJob = viewModelScope.launch {
            com.soar.sparkai.feature.audio.AudioStreamer.startStreaming(
                ip = ip.trim(),
                port = port.trim().toIntOrNull() ?: 9090,
                onSuccess = {
                    viewModelScope.launch(Dispatchers.Main) {
                        isAudioStreaming = true
                        android.widget.Toast.makeText(context, "🎙 无线麦克风已成功投送到电脑端", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { err ->
                    viewModelScope.launch(Dispatchers.Main) {
                        isAudioStreaming = false
                        android.widget.Toast.makeText(context, "❌ 麦克风投送失败: $err", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    // 停止手机麦克风投射
    fun stopAudioStreaming() {
        com.soar.sparkai.feature.audio.AudioStreamer.stopStreaming()
        audioJob?.cancel()
        audioJob = null
        isAudioStreaming = false
    }

    // 上传文件
    fun uploadSelectedFile(context: Context, uri: Uri) {
        val filename = getFileName(context, uri)
        val size = getFileSize(context, uri)
        val taskId = (Math.random() * Int.MAX_VALUE).toInt().toString(36).substring(2, 10)
        
        val newItem = TransferItem(
            id = taskId,
            name = filename,
            size = size,
            type = TransferType.UPLOAD,
            status = TransferStatus.TRANSFERRING,
            timestamp = getCurrentTime()
        )
        
        transferList = listOf(newItem) + transferList

        viewModelScope.launch {
            FileTransferManager.uploadFile(
                context = context,
                ip = ip.trim(),
                port = port.trim().toIntOrNull() ?: 9090,
                fileUri = uri,
                onProgress = { progress ->
                    updateItemProgress(taskId, progress)
                },
                onSuccess = {
                    updateItemStatus(taskId, TransferStatus.SUCCESS)
                },
                onError = { err ->
                    updateItemStatus(taskId, TransferStatus.FAILED, err)
                }
            )
        }
    }

    // 发送文本/链接给电脑端
    fun sendText(text: String) {
        if (text.isBlank()) return
        val isLink = text.startsWith("http://") || text.startsWith("https://")
        val taskId = (Math.random() * Int.MAX_VALUE).toInt().toString(36).substring(2, 10)
        
        val newItem = TransferItem(
            id = taskId,
            name = text,
            size = text.length.toLong(),
            type = if (isLink) TransferType.UPLOAD_LINK else TransferType.UPLOAD_TEXT,
            status = TransferStatus.TRANSFERRING,
            speed = "发送中",
            timestamp = getCurrentTime()
        )
        transferList = listOf(newItem) + transferList

        viewModelScope.launch {
            FileTransferManager.uploadText(
                ip = ip.trim(),
                port = port.trim().toIntOrNull() ?: 9090,
                text = text,
                onSuccess = {
                    updateItemStatus(taskId, TransferStatus.SUCCESS)
                },
                onError = { err ->
                    updateItemStatus(taskId, TransferStatus.FAILED, err)
                }
            )
        }
    }

    // 接受电脑端的文件发送要约
    fun acceptOffer(context: Context, item: TransferItem) {
        activeOffer = null
        val taskId = item.id
        val newItem = item.copy(status = TransferStatus.TRANSFERRING)
        transferList = listOf(newItem) + transferList

        viewModelScope.launch {
            FileTransferManager.downloadFile(
                context = context,
                ip = ip.trim(),
                port = port.trim().toIntOrNull() ?: 9090,
                fileId = item.id,
                filename = item.name,
                totalSize = item.size,
                onProgress = { progress ->
                    updateItemProgress(taskId, progress)
                },
                onSuccess = {
                    updateItemStatus(taskId, TransferStatus.SUCCESS)
                },
                onError = { err ->
                    updateItemStatus(taskId, TransferStatus.FAILED, err)
                }
            )
        }
    }

    fun rejectOffer() {
        activeOffer = null
    }

    private fun updateItemProgress(id: String, progress: Float) {
        transferList = transferList.map {
            if (it.id == id) it.copy(progress = progress, speed = "${(progress).toInt()}%") else it
        }
    }

    private fun updateItemStatus(id: String, status: TransferStatus, error: String? = null) {
        transferList = transferList.map {
            if (it.id == id) it.copy(status = status, error = error, speed = if (status == TransferStatus.SUCCESS) "完成" else "失败") else it
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index != -1) {
                name = cursor.getString(index)
            }
        }
        return name
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && index != -1) {
                size = cursor.getLong(index)
            }
        }
        return size
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    var discoveredDevices by mutableStateOf<List<DiscoveredDevice>>(emptyList())
    private var udpJob: Job? = null

    init {
        startUdpDiscovery()
    }

    // 开启局域网 UDP 广播自动扫描发现
    fun startUdpDiscovery() {
        udpJob?.cancel()
        discoveredDevices = emptyList()
        udpJob = viewModelScope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(9092))
                }
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()
                    try {
                        val json = JSONObject(message)
                        if (json.optString("type") == "sparkai-server") {
                            val ipsArray = json.getJSONArray("ips")
                            val portVal = json.getInt("port")
                            val host = json.optString("hostname", "SparkAI 电脑端")

                            val senderIp = packet.address.hostAddress
                            var resolvedIp = ""
                            for (i in 0 until ipsArray.length()) {
                                val ipItem = ipsArray.getString(i)
                                if (ipItem == senderIp) {
                                    resolvedIp = ipItem
                                    break
                                }
                            }
                            if (resolvedIp.isEmpty() && ipsArray.length() > 0) {
                                resolvedIp = ipsArray.getString(0)
                            }

                            if (resolvedIp.isNotEmpty()) {
                                val device = DiscoveredDevice(hostname = host, ip = resolvedIp, port = portVal)
                                withContext(Dispatchers.Main) {
                                    if (!discoveredDevices.any { it.ip == device.ip && it.port == device.port }) {
                                        discoveredDevices = discoveredDevices + device
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 忽略解析错误
                    }
                }
            } catch (e: Exception) {
                // 忽略 Socket 异常
            } finally {
                socket?.close()
            }
        }
    }

    fun stopUdpDiscovery() {
        udpJob?.cancel()
        udpJob = null
        discoveredDevices = emptyList()
    }

    // 保存最后一次成功连接的 IP 和端口
    private fun saveLastConnection(context: Context, ipStr: String, portStr: String) {
        try {
            val prefs = context.getSharedPreferences("sparkai_transfer_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("last_connected_ip", ipStr)
                putString("last_connected_port", portStr)
                apply()
            }
        } catch (e: Exception) {
            // 忽略
        }
    }

    // 加载上一次成功连接的 IP 和端口
    fun loadLastConnection(context: Context) {
        try {
            val prefs = context.getSharedPreferences("sparkai_transfer_prefs", Context.MODE_PRIVATE)
            val savedIp = prefs.getString("last_connected_ip", null)
            val savedPort = prefs.getString("last_connected_port", null)
            if (!savedIp.isNullOrBlank()) {
                ip = savedIp
            }
            if (!savedPort.isNullOrBlank()) {
                port = savedPort
            }
        } catch (e: Exception) {
            // 忽略
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
        stopUdpDiscovery()
    }
}
