package com.soar.sparkai.feature.transfer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            ConnectionCard(viewModel)

            Spacer(modifier = Modifier.height(16.dp))

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
fun ConnectionCard(viewModel: FileTransferViewModel) {
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
                    modifier = Modifier.width(80.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (viewModel.isConnected) viewModel.disconnect() else viewModel.connect() },
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C3E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.type == TransferType.UPLOAD) Icons.Rounded.FileUpload else Icons.Rounded.FileDownload,
                    contentDescription = "方向",
                    tint = if (item.type == TransferType.UPLOAD) Color(0xFF6C5CE7) else Color(0xFF00B894),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    Text("${(item.size / 1024f / 1024f).toString().take(4)} MB | ${item.timestamp}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
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
