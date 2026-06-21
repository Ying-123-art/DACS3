package com.example.giuaky.ui.screen

import android.util.Base64
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
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
import coil.request.ImageRequest
import com.example.giuaky.data.model.Post
import com.example.giuaky.ui.components.PostCard
import com.example.giuaky.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: String? = null,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onNavigateToMap: (Double, Double) -> Unit,
    onNavigateToOtherProfile: (String) -> Unit,
    onShareClick: (Post, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val targetUid = userId ?: currentUid
    val isOwnProfile = targetUid == currentUid

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    LaunchedEffect(targetUid) {
        viewModel.loadProfile(targetUid, currentUid)
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

    // Logic xử lý avatar model (URL hoặc Base64)
    val avatarModel = remember(uiState.user?.avatarUrl) {
        val url = uiState.user?.avatarUrl ?: ""
        if (url.isEmpty()) {
            "https://ui-avatars.com/api/?name=${(uiState.user?.displayName ?: "U").replace(" ", "+")}&background=2E7D32&color=fff&size=256"
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOwnProfile) "Trang Cá Nhân" else "Người Dùng") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    if (isOwnProfile) {
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
                                model = ImageRequest.Builder(context)
                                    .data(avatarModel)
                                    .crossfade(true)
                                    .build()
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

                    Spacer(Modifier.height(12.dp))

                    if (!isOwnProfile) {
                        Button(
                            onClick = { viewModel.toggleFollow(currentUid, targetUid) },
                            colors = if (uiState.isFollowing) 
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                if (uiState.isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                                null, Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isFollowing) "Đang theo dõi" else "Theo dõi")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { avatarLauncher.launch("image/*") },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Đổi ảnh đại diện", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isEditing && isOwnProfile) {
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
                }

                HorizontalDivider()
                Text(
                    if (isOwnProfile) "Bài đăng của tôi" else "Bài đăng của ${uiState.user?.displayName ?: "người dùng"}",
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
                        authorAvatarUrl = uiState.user?.avatarUrl ?: post.authorAvatarUrl, // Ghi đè bằng avatar mới nhất từ Profile
                        authorName = uiState.user?.displayName ?: post.authorName, // Ghi đè bằng tên mới nhất
                        onAuthorClick = { authorId ->
                            if (authorId != targetUid) onNavigateToOtherProfile(authorId)
                        },
                        onEditClick = {}, 
                        onLikeClick = {}, 
                        onCommentClick = {},
                        onLocationClick = { lat, lon -> onNavigateToMap(lat, lon) },
                        onShareClick = { }
                    )
                }
            }
        }
    }
}
