package com.example.giuaky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Comment
import com.example.giuaky.data.model.User
import com.example.giuaky.data.repository.CommentRepository
import com.example.giuaky.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentUiState(
    val comments: List<Comment> = emptyList(),
    val users: Map<String, User> = emptyMap(), // Map để tra cứu thông tin user mới nhất
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)

class CommentViewModel : ViewModel() {
    private val repository = CommentRepository()
    private val userRepository = UserRepository()
    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch {
            // Tải danh sách user trước để có thông tin avatar
            userRepository.getAllUsers().onSuccess { userList ->
                val usersMap = userList.associateBy { it.uid }
                _uiState.update { it.copy(users = usersMap) }
            }
            
            repository.getComments(postId).collect { comments ->
                _uiState.update { it.copy(comments = comments, isLoading = false) }
            }
        }
    }

    fun addComment(postId: String, content: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            
            // Lấy thông tin user hiện tại để gửi (dùng làm fallback)
            val userProfile = userRepository.getUserProfile(user.uid).getOrNull()
            
            val comment = Comment(
                postId = postId,
                userId = user.uid,
                authorName = userProfile?.displayName ?: user.displayName ?: "Người dùng",
                authorAvatarUrl = userProfile?.avatarUrl ?: "",
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.addComment(comment)
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(postId, commentId)
        }
    }
}
