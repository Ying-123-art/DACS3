package com.example.giuaky.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.model.User
import com.example.giuaky.data.repository.PostRepository
import com.example.giuaky.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ProfileUiState(
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val userRepo = UserRepository()
    private val postRepo = PostRepository()
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(uid: String, currentUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userResult = userRepo.getUserProfile(uid)
            userResult.onSuccess { user ->
                val following = userRepo.isFollowing(currentUid, uid)
                _uiState.update { it.copy(user = user, isFollowing = following, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        viewModelScope.launch {
            postRepo.getPostsByUser(uid).collect { posts ->
                _uiState.update { it.copy(posts = posts) }
            }
        }
    }

    fun toggleFollow(currentUid: String, targetUid: String) {
        viewModelScope.launch {
            val isCurrentlyFollowing = _uiState.value.isFollowing
            val result = if (isCurrentlyFollowing) {
                userRepo.unfollowUser(currentUid, targetUid)
            } else {
                userRepo.followUser(currentUid, targetUid)
            }
            
            result.onSuccess {
                _uiState.update { it.copy(isFollowing = !isCurrentlyFollowing) }
            }
        }
    }

    fun updateProfile(uid: String, displayName: String, bio: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = userRepo.updateUserProfile(uid, displayName, bio)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            saveSuccess = true,
                            user = state.user?.copy(displayName = displayName, bio = bio)
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
            )
        }
    }

    fun uploadAvatar(context: Context, uid: String, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val base64Avatar = uriToBase64(context, uri)
            if (base64Avatar.isNotEmpty()) {
                val result = userRepo.updateAvatar(uid, base64Avatar)
                result.onSuccess {
                    _uiState.update { it.copy(isSaving = false, user = it.user?.copy(avatarUrl = base64Avatar)) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Lỗi xử lý ảnh") }
            }
        }
    }

    private suspend fun uriToBase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return@withContext ""
            
            val scaledBitmap = scaleBitmap(bitmap)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = 250
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val ratio: Float = width.toFloat() / height.toFloat()
        var finalWidth = maxSize
        var finalHeight = maxSize
        if (width > height) finalHeight = (maxSize / ratio).toInt()
        else finalWidth = (maxSize * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    fun sharePost(originalPost: Post, sharedText: String) {
        val currentUser = _uiState.value.user ?: return
        viewModelScope.launch {
            postRepo.sharePost(originalPost, currentUser.uid, currentUser, sharedText)
        }
    }

    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }
}
