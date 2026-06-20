package com.example.giuaky

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
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
import com.example.giuaky.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    postId: String,
    viewModel: PostViewModel,
    onBackToHome: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(uiState.post) {
        uiState.post?.let { post ->
            if (title.isEmpty()) title = post.title
            if (content.isEmpty()) content = post.content
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBackToHome()
            viewModel.clearState()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) newImageUri = uri
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa bài đăng?") },
            text = { Text("Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePost(postId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh Sửa Bài Đăng") },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.post == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Tiêu đề") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    singleLine = true, enabled = !uiState.isLoading
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("Nội dung") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    shape = RoundedCornerShape(12.dp), enabled = !uiState.isLoading
                )
                Spacer(Modifier.height(16.dp))

                val displayImage = newImageUri ?: uiState.post?.imageUrl.takeIf { it?.isNotEmpty() == true }
                
                if (displayImage != null) {
                    Image(
                        painter = rememberAsyncImagePainter(displayImage),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thay đổi ảnh")
                }

                uiState.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(24.dp))

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.updatePost(
                                context,
                                postId,
                                title,
                                content,
                                uiState.post?.imageUrl ?: "",
                                newImageUri
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = title.isNotEmpty() || content.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cập Nhật Bài Đăng", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Xóa Bài Đăng", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
