package com.example.giuaky

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.giuaky.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomescreen(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Bài Viết", "Người Dùng")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🌿 WildLog Admin", style = MaterialTheme.typography.titleLarge)
                        Text("Bảng điều khiển quản trị", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Đăng xuất", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> DashboardTab(uiState.stats.totalPosts, uiState.stats.totalUsers,
                        uiState.stats.todayPosts, uiState.stats.postsPerDay)
                    1 -> PostsTab(viewModel)
                    2 -> UsersTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun DashboardTab(totalPosts: Int, totalUsers: Int, todayPosts: Int, postsPerDay: Map<String, Int>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Thống Kê Tổng Quan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Bài Đăng", totalPosts.toString(), Icons.Default.Article, Color(0xFF2E7D32), Modifier.weight(1f))
                StatCard("Người Dùng", totalUsers.toString(), Icons.Default.Group, Color(0xFF0277BD), Modifier.weight(1f))
                StatCard("Hôm Nay", todayPosts.toString(), Icons.Default.TrendingUp, Color(0xFFFF8F00), Modifier.weight(1f))
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Bài đăng 7 ngày gần nhất", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (postsPerDay.isEmpty()) {
                Text("Chưa có dữ liệu", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                val maxValue = postsPerDay.values.maxOrNull() ?: 1
                postsPerDay.entries.take(7).forEach { (date, count) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(date, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(progress = { count.toFloat() / maxValue }, modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("$count", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PostsTab(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var filterMode by remember { mutableIntStateOf(0) }

    val displayPosts = if (filterMode == 0) {
        uiState.allPosts.filter { it.status == "pending" }
    } else {
        uiState.allPosts
    }

    deleteTarget?.let { postId ->
        val post = uiState.allPosts.find { it.id == postId }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa bài viết?") },
            text = { Text("Bài viết \"${post?.title ?: ""}\" sẽ bị xóa vĩnh viễn.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deletePost(postId, post?.imageUrl ?: "")
                    deleteTarget = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Xóa")
                }
            },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text("Hủy") } }
        )
    }

    Column {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            SegmentedButton(selected = filterMode == 0, onClick = { filterMode = 0 }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) {
                Text("Chờ duyệt (${uiState.allPosts.count { it.status == "pending" }})")
            }
            SegmentedButton(selected = filterMode == 1, onClick = { filterMode = 1 }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) {
                Text("Tất cả")
            }
        }

        if (displayPosts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không có bài viết nào", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(displayPosts, key = { it.id }) { post ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (post.imageUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(context)
                                                .data(post.imageUrl)
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(post.title.ifEmpty { "(Không có tiêu đề)" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Bởi: ${post.authorName}", style = MaterialTheme.typography.bodySmall)
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(post.status.uppercase()) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            labelColor = when(post.status) {
                                                "approved" -> Color(0xFF2E7D32)
                                                "pending" -> Color(0xFFE65100)
                                                else -> Color.Red
                                            }
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(post.content, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (post.status == "pending") {
                                    IconButton(onClick = { viewModel.approvePost(post.id) }) { Icon(Icons.Default.Check, "Duyệt", tint = Color(0xFF2E7D32)) }
                                    IconButton(onClick = { viewModel.rejectPost(post.id) }) { Icon(Icons.Default.Close, "Từ chối", tint = MaterialTheme.colorScheme.error) }
                                }
                                IconButton(onClick = { deleteTarget = post.id }) { Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsersTab(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(8.dp)) {
        items(uiState.allUsers, key = { it.uid }) { user ->
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(user.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.displayName.replace(" ", "+")}&background=2E7D32&color=fff" })
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName.ifEmpty { "Chưa đặt tên" }, fontWeight = FontWeight.SemiBold)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Badge(containerColor = if (user.role == "admin") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
                        Text(user.role.uppercase(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
