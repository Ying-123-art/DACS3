package com.example.giuaky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Comment
import com.example.giuaky.data.repository.CommentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentUiState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)

class CommentViewModel : ViewModel() {
    private val repository = CommentRepository()
    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    fun loadComments(postId: String) {
        viewModelScope.launch {
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
            val comment = Comment(
                postId = postId,
                userId = user.uid,
                authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Người dùng",
                authorAvatarUrl = "",
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
