package com.example.giuaky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.model.User
import com.example.giuaky.data.repository.PostRepository
import com.example.giuaky.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AdminStats(
    val totalPosts: Int = 0,
    val totalUsers: Int = 0,
    val todayPosts: Int = 0,
    val postsPerDay: Map<String, Int> = emptyMap()
)

data class AdminUiState(
    val stats: AdminStats = AdminStats(),
    val allPosts: List<Post> = emptyList(),
    val allUsers: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val postRepo = PostRepository()
    private val userRepo = UserRepository()
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    val pendingPosts: StateFlow<List<Post>> = uiState.map { state ->
        state.allPosts.filter { it.status == "pending" }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        // Load users once separately
        viewModelScope.launch {
            val usersResult = userRepo.getAllUsers()
            val users = usersResult.getOrDefault(emptyList())
            _uiState.update { it.copy(allUsers = users) }
        }
        // Listen to posts realtime
        viewModelScope.launch {
            postRepo.getAllPosts().collect { posts ->
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }.timeInMillis
                val todayPosts = posts.count { it.timestamp >= today }
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                val postsPerDay = posts.groupBy { sdf.format(it.timestamp) }
                    .mapValues { it.value.size }
                    .entries.sortedByDescending { it.key }
                    .take(7).associate { it.toPair() }

                _uiState.update {
                    it.copy(
                        stats = AdminStats(
                            totalPosts = posts.size,
                            totalUsers = it.allUsers.size,
                            todayPosts = todayPosts,
                            postsPerDay = postsPerDay
                        ),
                        allPosts = posts,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun approvePost(postId: String) {
        viewModelScope.launch {
            postRepo.updatePostStatus(postId, "approved")
        }
    }

    fun rejectPost(postId: String) {
        viewModelScope.launch {
            postRepo.updatePostStatus(postId, "rejected")
        }
    }

    fun deletePost(postId: String, imageUrl: String) {
        viewModelScope.launch {
            postRepo.deletePost(postId, imageUrl)
        }
    }
}
