package com.soar.sparkai.feature.transfer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soar.sparkai.feature.transfer.model.TransferItem
import com.soar.sparkai.feature.transfer.model.TransferStatus
import com.soar.sparkai.feature.transfer.model.TransferType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTransferScreen(
    onBack: () -> Unit,
    viewModel: FileTransferViewModel = viewModel()
) {
    val context = LocalContext.current
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadSelectedFile(context, it) }
    }

    var showScanner by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A)) // 炫酷暗夜底色
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 顶栏 (带返回按钮)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "局域网高速互传",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. 连接状态配置卡片
            ConnectionCard(viewModel) {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔍 自动发现附近的电脑设备 (仅未连接时显示)
            if (!viewModel.isConnected && viewModel.discoveredDevices.isNotEmpty()) {
                Text(
                    text = "🔍 附近发现可用电脑 (${viewModel.discoveredDevices.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.discoveredDevices) { device ->
                        Card(
                            onClick = {
                                viewModel.ip = device.ip
                                viewModel.port = device.port.toString()
                                viewModel.connect(context)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF00B894), RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = device.hostname,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${device.ip}:${device.port}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "一键桥接连接",
                                    color = Color(0xFF00B894),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. 文件上传控制卡片 (仅连接成功时可用)
            if (viewModel.isConnected) {
                Button(
                    onClick = { pickFileLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = "上传", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择手机文件并发送给电脑", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // 2.5 文本与剪贴板极速共享卡片
                var inputText by remember { mutableStateOf("") }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF00B894), Color(0xFF6C5CE7))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "剪贴板",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("剪贴板与文本极速共享", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("在此输入文本或网页链接...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6C5CE7),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendText(inputText)
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("发送文本", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = clipboard.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                                            if (clipText.isNotBlank()) {
                                                viewModel.sendText(clipText)
                                            } else {
                                                android.widget.Toast.makeText(context, "📋 剪贴板文本为空", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "📋 剪贴板中无内容", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "读取剪贴板失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("发送剪贴板", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. 传输记录列表
            Text(
                text = "传输任务记录 (${viewModel.transferList.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(viewModel.transferList, key = { it.id }) { item ->
                    TransferItemRow(item)
                }
            }
        }

        // 扫码弹窗
        if (showScanner) {
            Dialog(onDismissRequest = { showScanner = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "请对准电脑端二维码",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .background(Color.Black, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            QRCodeScannerView { result ->
                                try {
                                    val url = java.net.URL(result)
                                    viewModel.ip = url.host
                                    viewModel.port = url.port.toString()
                                    viewModel.connect(context)
                                } catch (e: Exception) {
                                    val regex = Regex("""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)""")
                                    val match = regex.find(result)
                                    if (match != null) {
                                        viewModel.ip = match.groupValues[1]
                                        viewModel.port = match.groupValues[2]
                                        viewModel.connect(context)
                                    } else {
                                        viewModel.ip = result
                                        viewModel.port = "9090"
                                        viewModel.connect(context)
                                    }
                                }
                                showScanner = false
                            }
                        }
                        TextButton(onClick = { showScanner = false }) {
                            Text("取消扫描", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        // 电脑端推送要约时的 Dialog 弹窗
        viewModel.activeOffer?.let { offer ->
            AlertDialog(
                onDismissRequest = { viewModel.rejectOffer() },
                containerColor = Color(0xFF1E1E2F),
                shape = RoundedCornerShape(24.dp),
                title = { Text("收到电脑文件分享", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "电脑端想向您发送文件：\n\n📄 ${offer.name}\n⚖ 大小: ${(offer.size / 1024f / 1024f).toString().take(5)} MB\n\n是否立即接收并下载？",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.acceptOffer(context, offer) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894))
                    ) {
                        Text("接收", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.rejectOffer() }) {
                        Text("拒绝", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }
    }
}

@Composable
fun ConnectionCard(viewModel: FileTransferViewModel, onScanClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (viewModel.isConnected)
                                    listOf(Color(0xFF00B894), Color(0xFF05C46B))
                                else
                                    listOf(Color(0xFFE94057), Color(0xFF8A2387))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (viewModel.isConnected) Icons.Rounded.SwapHorizontalCircle else Icons.Rounded.WifiOff,
                        contentDescription = "通信",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("电脑互传桥接器", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = if (viewModel.isConnected) "通信在线 - 长连接已就绪" else "未连接 - 请先开启电脑端引擎",
                        color = if (viewModel.isConnected) Color(0xFF00B894) else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // IP 地址与端口输入框
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.ip,
                    onValueChange = { if (!viewModel.isConnected) viewModel.ip = it },
                    label = { Text("电脑 IP", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = viewModel.port,
                    onValueChange = { if (!viewModel.isConnected) viewModel.port = it },
                    label = { Text("端口", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.width(76.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!viewModel.isConnected) {
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = onScanClick,
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF6C5CE7).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "扫码",
                            tint = Color(0xFF6C5CE7)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (viewModel.isConnected) viewModel.disconnect() else viewModel.connect(context) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isConnected) Color(0xFFE94057) else Color(0xFF00B894)
                )
            ) {
                Text(
                    text = if (viewModel.isConnected) "断开局域网桥接" else "连接电脑端引擎",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TransferItemRow(item: TransferItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C3E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 智能识别类型和方向
                val isUpload = item.type == TransferType.UPLOAD || item.type == TransferType.UPLOAD_TEXT || item.type == TransferType.UPLOAD_LINK
                val isText = item.type == TransferType.UPLOAD_TEXT || item.type == TransferType.DOWNLOAD_TEXT
                val isLink = item.type == TransferType.UPLOAD_LINK || item.type == TransferType.DOWNLOAD_LINK
                
                val icon = if (isText) Icons.Rounded.Article 
                           else if (isLink) Icons.Rounded.Link 
                           else if (isUpload) Icons.Rounded.FileUpload 
                           else Icons.Rounded.FileDownload
                           
                val iconColor = if (isText) Color(0xFF00B894)
                                else if (isLink) Color(0xFFEC4899)
                                else if (isUpload) Color(0xFF6C5CE7)
                                else Color(0xFF00B894)

                Icon(
                    imageVector = icon,
                    contentDescription = "类型",
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isText) "[文本] ${item.name}" else if (isLink) "[链接] ${item.name}" else item.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    
                    val detailText = if (isText || isLink) {
                        "${item.size} 字符 | ${item.timestamp}"
                    } else {
                        "${(item.size / 1024f / 1024f).toString().take(4)} MB | ${item.timestamp}"
                    }
                    Text(detailText, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 当成功传输文本或链接时，提供一键操作按钮
                    if (item.status == TransferStatus.SUCCESS && (isText || isLink)) {
                        IconButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("SparkAI text", item.name)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "📋 已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "复制失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "复制", tint = Color(0xFF00B894), modifier = Modifier.size(16.dp))
                        }
                        
                        if (isLink) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(item.name))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "无法打开链接: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = "打开", tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    
                    Text(
                        text = when (item.status) {
                            TransferStatus.WAITING -> "等待中"
                            TransferStatus.TRANSFERRING -> item.speed
                            TransferStatus.SUCCESS -> "完成"
                            TransferStatus.FAILED -> "失败"
                        },
                        color = when (item.status) {
                            TransferStatus.SUCCESS -> Color(0xFF00B894)
                            TransferStatus.FAILED -> Color(0xFFE94057)
                            else -> Color.White.copy(alpha = 0.8f)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (item.status == TransferStatus.TRANSFERRING) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = item.progress / 100f,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (item.type == TransferType.UPLOAD) Color(0xFF6C5CE7) else Color(0xFF00B894),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}
