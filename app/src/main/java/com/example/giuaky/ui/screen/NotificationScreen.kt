package com.example.giuaky.ui.screen

import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.giuaky.data.model.Notification
import com.example.giuaky.data.model.User
import com.example.giuaky.navigation.NavRoutes
import com.example.giuaky.ui.components.BottomNavBar
import com.example.giuaky.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel,
    onNavigateToPost: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông báo", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            BottomNavBar(currentRoute = NavRoutes.NOTIFICATIONS) { route ->
                when (route) {
                    NavRoutes.HOME -> onNavigateToHome()
                    NavRoutes.MAP -> onNavigateToMap()
                    NavRoutes.PROFILE -> onNavigateToProfile()
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Chưa có thông báo nào",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.notifications, key = { it.id }) { notification ->
                    // Lấy user mới nhất từ Map
                    val latestUser = uiState.users[notification.fromUserId]
                    
                    NotificationItem(
                        notification = notification,
                        latestUser = latestUser,
                        onClick = {
                            viewModel.markAsRead(notification.id)
                            if (notification.type == "follow") {
                                onNavigateToUserProfile(notification.fromUserId)
                            } else if (notification.postId.isNotEmpty()) {
                                onNavigateToPost(notification.postId)
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    latestUser: User?,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(notification.timestamp))
    val context = LocalContext.current

    // Ưu tiên dùng thông tin từ User Profile mới nhất
    val avatarUrl = latestUser?.avatarUrl ?: notification.fromUserAvatar
    val fromUserName = latestUser?.displayName ?: notification.fromUserName

    val avatarModel = remember(avatarUrl) {
        if (avatarUrl.isEmpty()) {
            "https://ui-avatars.com/api/?name=${fromUserName.replace(" ", "+")}&background=2E7D32&color=fff"
        } else if (avatarUrl.startsWith("http")) {
            avatarUrl
        } else {
            try {
                val cleanBase64 = avatarUrl.substringAfter("base64,")
                Base64.decode(cleanBase64, Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(avatarModel)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            "like" -> Color(0xFFE53935)
                            "comment" -> Color(0xFF1E88E5)
                            "share" -> Color(0xFF2E7D32)
                            "follow" -> Color(0xFF9C27B0)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "like" -> Icons.Default.Favorite
                        "comment" -> Icons.Default.ChatBubble
                        "share" -> Icons.Default.Share
                        "follow" -> Icons.Default.Person
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fromUserName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = notification.content,
                    fontSize = 14.sp,
                    maxLines = 2
                )
            }
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
