package com.soar.sparkai.feature.ai.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.feature.ai.model.AiMessage
import com.soar.sparkai.feature.ai.service.AiService
import com.soar.sparkai.feature.ai.util.AiConfigManager
import com.soar.sparkai.feature.ai.util.ScreenshotLoader
import kotlinx.coroutines.launch

/**
 * AI 智能助理及多模态截图分析主界面 Composable 视图
 *
 * 特色：
 * 1. 拟物化渐变对话气泡：用户消息是紫红渐变，助理消息是精美深色微透卡片。
 * 2. 完美的流式打字光标：在流式生成期间，在最底部字符旁闪烁竖线特效。
 * 3. 最新截图快捷挂载：提供一键检索系统相册导入最新一张悬浮球截图的功能。
 * 4. 全局参数配置入口：右侧直达配置中心，实现本地热同步持久化。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 对话列表状态缓存 (初始为空白)
    val messageList = remember { mutableStateListOf<AiMessage>() }
    val lazyListState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // 弹窗表单开关
    var showConfigDialog by remember { mutableStateOf(false) }

    // 用来控制打字光标的持续闪烁周期
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // 照片选取 Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                attachedUri = uri
                attachedBitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                }
                // 自动载入默认分析 Prompt
                if (inputText.isBlank()) {
                    inputText = AiConfigManager.getPresetPrompt(context)
                }
                AppLogger.i("AiChatScreen", "相册照片载入成功！")
            } catch (e: Exception) {
                AppLogger.e("AiChatScreen", "相册解码 Bitmap 发生异常: ${e.message}", e)
                Toast.makeText(context, "图片加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 每次添加消息平滑滚动至最底部
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            lazyListState.animateScrollToItem(messageList.size - 1)
        }
    }

    // 流式打字追加期间也平滑追随滚动到底部
    val listSize = messageList.size
    LaunchedEffect(listSize) {
        if (listSize > 0 && messageList.last().isStreaming) {
            lazyListState.scrollToItem(listSize - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI 智能对话探索",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "默认直连 mimo 视觉与文本网关",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "参数配置",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F1A)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. 聊天气泡记录LazyColumn
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messageList) { msg ->
                        val isUser = msg.role == "user"
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                // AI 头像
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF8A2387), Color(0xFFE94057))
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("AI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            // 聊天气泡卡片
                            Column(
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            brush = if (isUser) {
                                                Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFF8A2387), Color(0xFFE94057))
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFF1E1E2F), Color(0xFF1E1E2F))
                                                )
                                            },
                                            shape = if (isUser) {
                                                RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
                                            } else {
                                                RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (msg.isError) Color(0xFFD63031).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
                                            shape = if (isUser) {
                                                RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
                                            } else {
                                                RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
                                            }
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // 渲染图片
                                        if (msg.imageBitmap != null) {
                                            Image(
                                                bitmap = msg.imageBitmap.asImageBitmap(),
                                                contentDescription = "发送的图片",
                                                modifier = Modifier
                                                    .heightIn(max = 240.dp)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Fit
                                            )
                                        }

                                        // 渲染文字
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = msg.content,
                                                color = if (msg.isError) Color(0xFFFFA8A8) else Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 19.sp
                                            )
                                            
                                            // 正在生成打字特效
                                            if (msg.isStreaming) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .height(13.dp)
                                                        .background(Color(0xFF55EFC4).copy(alpha = cursorAlpha))
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                // 用户头像
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ME", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 空白状态欢迎展示
                    if (messageList.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.7f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.White.copy(alpha = 0.03f), shape = CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = "提示",
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("开启 AI 智能探索", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "输入问题与 AI 对话，或点击下方📷一键导入最新的常驻悬浮截图，进行全屏智能 Vision 破译分析。",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 2. 挂载栏预览
                AnimatedVisibility(
                    visible = attachedBitmap != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    attachedBitmap?.let { bitmap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(Color(0xFF1E1E2F), shape = RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFF55EFC4).copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "挂载截图预览",
                                modifier = Modifier
                                    .size(60.dp, 40.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("已成功挂载一张屏幕截图", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("发送将强制切换为多模态 mimo-v2.5 视觉模型", color = Color(0xFF55EFC4), fontSize = 9.sp)
                            }
                            IconButton(onClick = {
                                attachedBitmap = null
                                attachedUri = null
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "删除",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // 3. 📷 快捷工具栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 相册选择按钮
                    AssistChip(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        label = { Text("从相册选择", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = "相册",
                                tint = Color(0xFF55EFC4),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1E1E2F)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 自动载入最新截图按钮
                    AssistChip(
                        onClick = {
                            val res = ScreenshotLoader.loadLatestSparkScreenshot(context)
                            if (res != null) {
                                attachedUri = res.first
                                attachedBitmap = res.second
                                if (inputText.isBlank()) {
                                    inputText = AiConfigManager.getPresetPrompt(context)
                                }
                                Toast.makeText(context, "已成功导入最新截图！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "未在相册中检索到由 SparkAI 悬浮球截取的图片", Toast.LENGTH_LONG).show()
                            }
                        },
                        label = { Text("载入最新截图", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Crop,
                                contentDescription = "自动寻回",
                                tint = Color(0xFFE94057),
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1E1E2F)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 4. 对话输入发送卡片
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入需要询问 AI 的内容...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE94057),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color(0xFF1E1E2F),
                            unfocusedContainerColor = Color(0xFF1E1E2F)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // 渐变发送按钮
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF8A2387), Color(0xFFE94057))
                                ),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable(enabled = !isLoading && (inputText.isNotBlank() || attachedBitmap != null)) {
                                val text = inputText.trim()
                                val bitmap = attachedBitmap
                                val uri = attachedUri

                                attachedBitmap = null
                                attachedUri = null
                                inputText = ""
                                keyboardController?.hide()

                                coroutineScope.launch {
                                    // 1. 发射用户提问
                                    val userMsg = AiMessage(
                                        role = "user",
                                        content = text.ifEmpty { "开始智能截图分析" },
                                        imageUri = uri,
                                        imageBitmap = bitmap
                                    )
                                    messageList.add(userMsg)

                                    // 2. 发射 AI 初始空白消息气泡
                                    val aiMsg = AiMessage(
                                        role = "assistant",
                                        content = "",
                                        isStreaming = true
                                    )
                                    messageList.add(aiMsg)

                                    isLoading = true

                                    val apiKey = AiConfigManager.getApiKey(context)
                                    val baseUrl = AiConfigManager.getBaseUrl(context)
                                    val model = AiConfigManager.getDefaultModel(context)
                                    val prompt = AiConfigManager.getPresetPrompt(context)

                                    val historyContext = messageList.dropLast(2)

                                    try {
                                        AiService.sendChatRequestStream(
                                            apiKey = apiKey,
                                            baseUrl = baseUrl,
                                            model = model,
                                            promptText = text.ifEmpty { prompt },
                                            bitmap = bitmap,
                                            history = historyContext
                                        ).collect { chunk ->
                                            // 取出最后一个气泡并更新
                                            val index = messageList.size - 1
                                            if (index >= 0) {
                                                val lastMsg = messageList[index]
                                                messageList[index] = lastMsg.copy(
                                                    content = lastMsg.content + chunk
                                                )
                                            }
                                        }
                                        // 流式接收完成，撤销打字光标特效
                                        val index = messageList.size - 1
                                        if (index >= 0) {
                                            messageList[index] = messageList[index].copy(isStreaming = false)
                                        }
                                    } catch (err: Exception) {
                                        AppLogger.e("AiChatScreen", "网络交互遭遇致命异常: ${err.message}", err)
                                        val index = messageList.size - 1
                                        if (index >= 0) {
                                            messageList[index] = messageList[index].copy(
                                                content = "💥 请求小米大模型失败。\n错误详情: ${err.message ?: "连接超时"}\n\n请点击右上角⚙齿轮检查您的 API Key 与接口基础路径是否配置正确。",
                                                isError = true,
                                                isStreaming = false
                                            )
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = "发送",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 5. 挂载大模型配置 Dialog
            if (showConfigDialog) {
                AiConfigDialog(
                    onDismiss = { showConfigDialog = false },
                    onSaveSuccess = {
                        Toast.makeText(context, "配置更新保存成功！", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
