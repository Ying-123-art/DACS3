package com.example.giuaky.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.giuaky.data.model.Post
import com.example.giuaky.ui.components.PostCard
import com.example.giuaky.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onNavigateToMap: (Double, Double) -> Unit,
    onShareClick: (Post, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    
    var sharingPost by remember { mutableStateOf<Post?>(null) }
    var shareText by remember { mutableStateOf("") }

    LaunchedEffect(currentUid) {
        viewModel.loadProfile(currentUid)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            isEditing = false
            viewModel.clearSaveSuccess()
        }
    }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(context, currentUid, it) }
    }

    sharingPost?.let { post ->
        AlertDialog(
            onDismissRequest = { sharingPost = null },
            title = { Text("Chia sẻ lại kỷ niệm") },
            text = {
                Column {
                    Text("Viết gì đó về bài đăng này...", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shareText,
                        onValueChange = { shareText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cảm nghĩ của bạn...") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onShareClick(post, shareText)
                    sharingPost = null
                    shareText = ""
                }) {
                    Text("Chia sẻ")
                }
            },
            dismissButton = {
                TextButton(onClick = { sharingPost = null }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang Cá Nhân") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = {
                        editName = uiState.user?.displayName ?: ""
                        editBio = uiState.user?.bio ?: ""
                        isEditing = !isEditing
                    }) {
                        Icon(Icons.Default.Edit, "Chỉnh sửa")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, "Đăng xuất", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = uiState.user?.avatarUrl?.ifEmpty {
                                    "https://ui-avatars.com/api/?name=${(uiState.user?.displayName ?: "U").replace(" ", "+")}&background=2E7D32&color=fff&size=256"
                                } ?: "https://ui-avatars.com/api/?name=U&background=2E7D32&color=fff&size=256"
                            ),
                            contentDescription = "Avatar",
                            modifier = Modifier.size(100.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        if (uiState.isSaving) {
                            Box(
                                modifier = Modifier.size(100.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(Modifier.size(32.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { avatarLauncher.launch("image/*") },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Đổi ảnh đại diện", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editName, onValueChange = { editName = it },
                            label = { Text("Tên hiển thị") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editBio, onValueChange = { editBio = it },
                            label = { Text("Giới thiệu bản thân") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f)) {
                                Text("Hủy")
                            }
                            Button(
                                onClick = { viewModel.updateProfile(currentUid, editName, editBio) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving
                            ) {
                                if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Text("Lưu")
                            }
                        }
                    } else {
                        Text(
                            uiState.user?.displayName ?: "Người dùng",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            uiState.user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (!uiState.user?.bio.isNullOrEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                uiState.user?.bio ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${uiState.posts.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Bài đăng", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                HorizontalDivider()
                Text(
                    "Bài đăng của tôi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (uiState.posts.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có bài đăng nào", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                items(uiState.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        currentUserId = currentUid,
                        onEditClick = {}, 
                        onLikeClick = {}, 
                        onCommentClick = {},
                        onLocationClick = { lat, lon -> onNavigateToMap(lat, lon) },
                        onShareClick = { sharingPost = post }
                    )
                }
            }
        }
    }
}
