package com.example.giuaky.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.model.User
import com.example.giuaky.data.repository.PostRepository
import com.example.giuaky.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val posts: List<Post> = emptyList(),
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

    fun loadProfile(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userResult = userRepo.getUserProfile(uid)
            userResult.onSuccess { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        // Separate coroutine for posts flow
        viewModelScope.launch {
            postRepo.getPostsByUser(uid).collect { posts ->
                _uiState.update { it.copy(posts = posts) }
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

    fun uploadAvatar(uid: String, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = userRepo.uploadAvatar(uid, uri)
            result.onSuccess { url ->
                _uiState.update { it.copy(isSaving = false, user = it.user?.copy(avatarUrl = url)) }
            }
            result.onFailure { _uiState.update { it.copy(isSaving = false) } }
        }
    }

    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }
}
