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
    onNavigateToMap: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(NavRoutes.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🌿 WildLog", style = MaterialTheme.typography.titleLarge)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    NavRoutes.MAP -> onNavigateToMap()
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Tìm kiếm hành trình, địa điểm...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            if (uiState.isLoading) {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(4) { ShimmerPostCard() }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = viewModel.filteredPosts,
                        key = { it.id }
                    ) { post ->
                        PostCard(
                            post = post,
                            currentUserId = uiState.currentUserId,
                            onEditClick = { onNavigateToEdit(post.id) },
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onCommentClick = { onNavigateToComments(post.id) }
                        )
                    }
                }
            }
        }
    }
}
