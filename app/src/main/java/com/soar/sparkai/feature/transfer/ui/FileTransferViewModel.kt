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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileTransferViewModel : ViewModel() {

    var ip by mutableStateOf("192.168.1.")
    var port by mutableStateOf("9090")
    var isConnected by mutableStateOf(false)
    var transferList by mutableStateOf<List<TransferItem>>(emptyList())
    var activeOffer by mutableStateOf<TransferItem?>(null)

    private var connectionJob: Job? = null

    // 建立局域网长连接
    fun connect() {
        if (isConnected) return
        connectionJob = viewModelScope.launch {
            FileTransferManager.startSSEConnection(
                ip = ip.trim(),
                port = port.trim().toIntOrNull() ?: 9090,
                onOfferReceived = { id, name, size ->
                    activeOffer = TransferItem(
                        id = id,
                        name = name,
                        size = size,
                        type = TransferType.DOWNLOAD,
                        status = TransferStatus.WAITING,
                        timestamp = getCurrentTime()
                    )
                },
                onStatusChanged = { connected ->
                    isConnected = connected
                }
            )
        }
    }

    // 断开连接
    fun disconnect() {
        FileTransferManager.stopSSEConnection()
        connectionJob?.cancel()
        connectionJob = null
        isConnected = false
    }

    // 上传文件
    fun uploadSelectedFile(context: Context, uri: Uri) {
        val filename = getFileName(context, uri)
        val size = getFileSize(context, uri)
        val taskId = Math.random().toString(36).substring(2, 10)
        
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

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
