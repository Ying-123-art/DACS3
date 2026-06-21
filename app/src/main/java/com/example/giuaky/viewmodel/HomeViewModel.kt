package com.example.giuaky.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.local.AppDatabase
import com.example.giuaky.data.local.PostEntity
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.repository.PostRepository
import com.example.giuaky.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val filteredPosts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val currentUserId: String = "",
    val mapFocusPoint: Pair<Double, Double>? = null,
    val error: String? = null
)

class HomeViewModel(context: Context) : ViewModel() {
    private val repository = PostRepository()
    private val userRepository = UserRepository()
    private val postDao = AppDatabase.getInstance(context).postDao()
    
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _mapFocusPoint = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _currentUserId = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid ?: "")
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _posts, _isLoading, _searchQuery, _mapFocusPoint, _currentUserId, _error
    ) { args ->
        val posts = args[0] as List<Post>
        val isLoading = args[1] as Boolean
        val searchQuery = args[2] as String
        val mapFocusPoint = args[3] as Pair<Double, Double>?
        val currentUserId = args[4] as String
        val error = args[5] as String?

        val filtered = if (searchQuery.isBlank()) {
            posts
        } else {
            posts.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true) ||
                it.sharedContent.contains(searchQuery, ignoreCase = true)
            }
        }
        
        HomeUiState(
            posts = posts,
            filteredPosts = filtered,
            isLoading = isLoading,
            searchQuery = searchQuery,
            currentUserId = currentUserId,
            mapFocusPoint = mapFocusPoint,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        loadPosts()
        observeCachedPosts(context)
    }

    private fun observeCachedPosts(context: Context) {
        viewModelScope.launch {
            postDao.getAllPosts().collect { cached ->
                if (_isLoading.value && cached.isNotEmpty()) {
                    val approvedCached = cached.map { e -> e.toPost() }.filter { it.status == "approved" }
                    _posts.value = approvedCached
                    _isLoading.value = false
                }
            }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            repository.getAllPosts().collect { posts ->
                val approvedPosts = posts.filter { it.status == "approved" }
                _posts.value = approvedPosts
                _isLoading.value = false
                
                val entities = posts.map { it.toEntity() }
                postDao.upsertAll(entities)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLike(postId: String) {
        val currentUserId = _currentUserId.value
        val post = _posts.value.find { it.id == postId } ?: return
        val currentlyLiked = post.likedBy.containsKey(currentUserId)
        viewModelScope.launch {
            repository.toggleLike(postId, currentUserId, currentlyLiked)
        }
    }

    fun sharePost(originalPost: Post, sharedText: String) {
        val currentUserId = _currentUserId.value
        viewModelScope.launch {
            val userResult = userRepository.getUserProfile(currentUserId)
            userResult.fold(
                onSuccess = { currentUser ->
                    repository.sharePost(originalPost, currentUserId, currentUser, sharedText)
                },
                onFailure = { e ->
                    _error.value = "Không thể lấy thông tin người dùng để chia sẻ"
                }
            )
        }
    }

    fun setMapFocus(lat: Double, lon: Double) {
        _mapFocusPoint.value = Pair(lat, lon)
    }

    fun clearMapFocus() {
        _mapFocusPoint.value = null
    }

    fun clearError() {
        _error.value = null
    }
}

private fun Post.toEntity() = PostEntity(
    id = id, userId = userId, authorName = authorName, authorAvatarUrl = authorAvatarUrl,
    title = title, content = content, imageUrls = imageUrls, location = location,
    latitude = latitude, longitude = longitude, timestamp = timestamp,
    likesCount = likesCount, commentsCount = commentsCount,
    status = status,
    isShared = isShared,
    originalPostId = originalPostId,
    sharedContent = sharedContent,
    originalAuthorName = originalAuthorName
)

private fun PostEntity.toPost() = Post(
    id = id, userId = userId, authorName = authorName, authorAvatarUrl = authorAvatarUrl,
    title = title, content = content, imageUrls = imageUrls, location = location,
    latitude = latitude, longitude = longitude, timestamp = timestamp,
    likesCount = likesCount, commentsCount = commentsCount,
    status = status,
    isShared = isShared,
    originalPostId = originalPostId,
    sharedContent = sharedContent,
    originalAuthorName = originalAuthorName
)
