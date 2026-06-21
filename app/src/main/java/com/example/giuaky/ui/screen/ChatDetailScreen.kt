package com.example.giuaky.ui.screen

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.giuaky.data.model.ChatMessage
import com.example.giuaky.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    roomId: String,
    otherUserId: String,
    initialOtherUserName: String = "",
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(roomId, otherUserId) {
        viewModel.loadChatDetail(roomId, otherUserId)
    }

    // Tự động cuộn xuống khi có tin nhắn mới
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Chuyển URI thành Base64 để gửi ảnh
            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
            viewModel.sendMessage(otherUserId, "", "data:image/jpeg;base64,$base64")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarModel = remember(uiState.otherUser?.avatarUrl, uiState.otherUser?.displayName) {
                            val url = uiState.otherUser?.avatarUrl?.trim() ?: ""
                            if (url.isEmpty() || url == "null") {
                                "https://ui-avatars.com/api/?name=${(uiState.otherUser?.displayName ?: initialOtherUserName.ifEmpty { "U" }).replace(" ", "+")}&background=2E7D32&color=fff"
                            } else if (url.startsWith("http")) {
                                url
                            } else {
                                try {
                                    val cleanBase64 = url.substringAfter("base64,")
                                    Base64.decode(cleanBase64, Base64.DEFAULT)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(avatarModel)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.otherUser?.displayName ?: initialOtherUserName.ifEmpty { "Đang tải..." })
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Gửi ảnh", tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nhập tin nhắn...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(otherUserId, messageText)
                                messageText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.senderId == currentUserId
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMine: Boolean) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = sdf.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.imageUrl.isNotEmpty()) {
                    val imageModel = remember(message.imageUrl) {
                        if (message.imageUrl.startsWith("http")) {
                            message.imageUrl
                        } else {
                            try {
                                val cleanBase64 = message.imageUrl.substringAfter("base64,")
                                Base64.decode(cleanBase64, Base64.DEFAULT)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Chat image",
                        modifier = Modifier
                            .sizeIn(maxWidth = 200.dp, maxHeight = 300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    if (message.message.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
                
                if (message.message.isNotEmpty()) {
                    Text(text = message.message, style = MaterialTheme.typography.bodyLarge)
                }

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    color = (if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.6f)
                )
            }
        }
    }
}
