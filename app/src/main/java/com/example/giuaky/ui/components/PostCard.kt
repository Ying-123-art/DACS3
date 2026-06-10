package com.example.giuaky.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.giuaky.data.model.Post
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    onEditClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLiked = post.likedBy.containsKey(currentUserId)
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(post.timestamp))

    // Like animation
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
                        model = post.authorAvatarUrl.ifEmpty {
                            "https://ui-avatars.com/api/?name=${post.authorName.replace(" ", "+")}&background=2E7D32&color=fff&size=128"
                        }
                    ),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(text = post.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = dateString, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        if (post.location.isNotEmpty()) {
                            Text(" · ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Icon(Icons.Default.LocationOn, null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text(text = post.location, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (post.userId == currentUserId) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.MoreVert, null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }

            // Title & Content
            if (post.title.isNotEmpty()) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }

            // Image
            if (post.imageUrl.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Image(
                    painter = rememberAsyncImagePainter(post.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Likes count
            if (post.likesCount > 0 || post.commentsCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (post.likesCount > 0) {
                        Text("${post.likesCount} lượt thích",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    if (post.commentsCount > 0) {
                        Text("${post.commentsCount} bình luận",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text("Thích", color = likeColor, fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal)
                }
                TextButton(onClick = onCommentClick) {
                    Icon(Icons.Default.ChatBubbleOutline, null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.width(4.dp))
                    Text("Bình luận", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                TextButton(onClick = {}) {
                    Icon(Icons.Default.Share, null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.width(4.dp))
                    Text("Chia sẻ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
