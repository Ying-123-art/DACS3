package com.example.giuaky.ui.components

import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.giuaky.data.model.Post
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    onAuthorClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onLocationClick: (Double, Double) -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    authorAvatarUrl: String = post.authorAvatarUrl, // Thêm parameter để nhận avatar mới nhất
    authorName: String = post.authorName // Thêm parameter để nhận tên mới nhất
) {
    val isLiked = post.likedBy.containsKey(currentUserId)
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(post.timestamp))
    val context = LocalContext.current

    var likeClicked by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (likeClicked) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { likeClicked = false },
        label = "likeScale"
    )
    val likeColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        label = "likeColor"
    )

    // Logic xử lý Avatar cho tác giả bài viết
    val authorAvatarModel = remember(authorAvatarUrl) {
        val url = authorAvatarUrl
        if (url.isEmpty()) {
            "https://ui-avatars.com/api/?name=${authorName.replace(" ", "+")}&background=2E7D32&color=fff"
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(authorAvatarModel)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onAuthorClick(post.userId) },
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f)
                        .clickable { onAuthorClick(post.userId) }
                ) {
                    Text(text = authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (post.userId == currentUserId && !post.isShared) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }

            // Title & Content
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                if (post.title.isNotEmpty()) {
                    Text(text = post.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                }
                if (post.content.isNotEmpty()) {
                    Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
            
            // Location Display
            if (!post.location.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp)
                        .clickable { onLocationClick(post.latitude, post.longitude) }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = post.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Image Display (Bài đăng)
            if (post.imageUrl.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))

                val postImageModel = remember(post.imageUrl) {
                    if (post.imageUrl.startsWith("http")) {
                        post.imageUrl
                    } else {
                        try {
                            val cleanBase64 = post.imageUrl.substringAfter("base64,")
                            Base64.decode(cleanBase64, Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(postImageModel)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 400.dp)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Likes and Comments Count
            if (post.likesCount > 0 || post.commentsCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (post.likesCount > 0) {
                        Text(
                            text = "${post.likesCount} lượt thích",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (post.commentsCount > 0) {
                        Text(
                            text = "${post.commentsCount} bình luận",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { 
                    likeClicked = true
                    onLikeClick() 
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = likeColor,
                        modifier = Modifier.size(20.dp).scale(likeScale)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Thích",
                        color = likeColor,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
                    )
                }
                TextButton(onClick = onCommentClick) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Bình luận",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
