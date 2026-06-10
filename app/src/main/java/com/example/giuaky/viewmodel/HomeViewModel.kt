package com.example.giuaky.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.local.AppDatabase
import com.example.giuaky.data.local.PostEntity
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val currentUserId: String = ""
)

class HomeViewModel(context: Context) : ViewModel() {
    private val repository = PostRepository()
    private val postDao = AppDatabase.getInstance(context).postDao()
    private val _uiState = MutableStateFlow(HomeUiState(
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
        observeCachedPosts(context)
    }

    private fun observeCachedPosts(context: Context) {
        viewModelScope.launch {
            postDao.getAllPosts().collect { cached ->
                if (_uiState.value.isLoading && cached.isNotEmpty()) {
                    _uiState.update { it.copy(posts = cached.map { e -> e.toPost() }, isLoading = false) }
                }
            }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            repository.getAllPosts().collect { posts ->
                _uiState.update { it.copy(posts = posts, isLoading = false) }
                // Cache to Room
                val entities = posts.map { it.toEntity() }
                postDao.upsertAll(entities)
            }
        }
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun toggleLike(postId: String) {
        val currentUserId = _uiState.value.currentUserId
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        val currentlyLiked = post.likedBy.containsKey(currentUserId)
        viewModelScope.launch {
            repository.toggleLike(postId, currentUserId, currentlyLiked)
        }
    }

    val filteredPosts: List<Post>
        get() {
            val query = _uiState.value.searchQuery
            return if (query.isBlank()) _uiState.value.posts
            else _uiState.value.posts.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
            }
        }
}

private fun Post.toEntity() = PostEntity(
    id = id, userId = userId, authorName = authorName, authorAvatarUrl = authorAvatarUrl,
    title = title, content = content, imageUrl = imageUrl, location = location,
    latitude = latitude, longitude = longitude, timestamp = timestamp,
    likesCount = likesCount, commentsCount = commentsCount
)

private fun PostEntity.toPost() = Post(
    id = id, userId = userId, authorName = authorName, authorAvatarUrl = authorAvatarUrl,
    title = title, content = content, imageUrl = imageUrl, location = location,
    latitude = latitude, longitude = longitude, timestamp = timestamp,
    likesCount = likesCount, commentsCount = commentsCount
)
