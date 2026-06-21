package com.example.giuaky.ui.screen

import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.giuaky.data.model.ChatRoom
import com.example.giuaky.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onNavigateToChat: (String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadChatRooms()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.rooms.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Chưa có cuộc hội thoại nào", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.rooms, key = { it.id }) { room ->
                    ChatRoomItem(room = room) {
                        val otherUserId = room.participantIds.find { it != uiState.currentUserId } ?: ""
                        onNavigateToChat(room.id, otherUserId, room.otherUserName)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomItem(room: ChatRoom, onClick: () -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = if (room.lastMessageTimestamp > 0) sdf.format(Date(room.lastMessageTimestamp)) else ""

    val avatarModel = remember(room.otherUserAvatar) {
        val url = room.otherUserAvatar.trim()
        if (url.isEmpty() || url == "null") {
            "https://ui-avatars.com/api/?name=${room.otherUserName.ifEmpty { "U" }.replace(" ", "+")}&background=2E7D32&color=fff"
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(avatarModel)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.otherUserName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Text(
                    text = room.lastMessage.ifEmpty { "Bắt đầu cuộc trò chuyện" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
