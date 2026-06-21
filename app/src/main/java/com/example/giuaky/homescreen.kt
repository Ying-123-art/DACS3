package com.example.giuaky

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.giuaky.data.model.Post
import com.example.giuaky.navigation.NavRoutes
import com.example.giuaky.ui.components.BottomNavBar
import com.example.giuaky.ui.components.PostCard
import com.example.giuaky.ui.components.ShimmerPostCard
import com.example.giuaky.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToOtherProfile: (String) -> Unit,
    onNavigateToMap: (Double?, Double?) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(NavRoutes.HOME) }
    var sharingPost by remember { mutableStateOf<Post?>(null) }
    var shareText by remember { mutableStateOf("") }

    // Share Dialog
    sharingPost?.let { post ->
        AlertDialog(
            onDismissRequest = { sharingPost = null },
            title = { Text("Chia sẻ bài viết") },
            text = {
                Column {
                    Text("Bạn muốn nói gì về bài viết này?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shareText,
                        onValueChange = { shareText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nhập nội dung chia sẻ...") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.sharePost(post, shareText)
                    sharingPost = null
                    shareText = ""
                }) {
                    Text("Chia sẻ ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { sharingPost = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌿 WildLog", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo bài đăng")
            }
        },
        bottomBar = {
            BottomNavBar(currentRoute = currentTab) { route ->
                currentTab = route
                when (route) {
                    NavRoutes.PROFILE -> onNavigateToProfile()
                    NavRoutes.MAP -> onNavigateToMap(null, null)
                    NavRoutes.NOTIFICATIONS -> onNavigateToNotifications()
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Thanh tìm kiếm
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Tìm kiếm bài viết, địa điểm...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Text("✕")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            if (uiState.isLoading) {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(4) { ShimmerPostCard() }
                }
            } else {
                if (uiState.filteredPosts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Không tìm thấy kết quả phù hợp", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(items = uiState.filteredPosts, key = { it.id }) { post ->
                            // Lấy thông tin user mới nhất từ map
                            val latestUser = uiState.users[post.userId]
                            
                            PostCard(
                                post = post,
                                currentUserId = uiState.currentUserId,
                                authorAvatarUrl = latestUser?.avatarUrl ?: post.authorAvatarUrl,
                                authorName = latestUser?.displayName ?: post.authorName,
                                onAuthorClick = { userId ->
                                    if (userId == uiState.currentUserId) onNavigateToProfile()
                                    else onNavigateToOtherProfile(userId)
                                },
                                onEditClick = { onNavigateToEdit(post.id) },
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onCommentClick = { onNavigateToComments(post.id) },
                                onLocationClick = { lat, lon -> onNavigateToMap(lat, lon) },
                                onShareClick = { sharingPost = post }
                            )
                        }
                    }
                }
            }
        }
    }
}
