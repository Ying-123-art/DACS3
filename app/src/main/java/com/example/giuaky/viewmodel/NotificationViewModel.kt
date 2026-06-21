package com.example.giuaky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.Notification
import com.example.giuaky.data.model.User
import com.example.giuaky.data.repository.NotificationRepository
import com.example.giuaky.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val users: Map<String, User> = emptyMap(), // Thêm map để lấy avatar mới nhất
    val isLoading: Boolean = true,
    val error: String? = null
)

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
        loadNotifications()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers().onSuccess { userList ->
                val usersMap = userList.associateBy { it.uid }
                _uiState.update { it.copy(users = usersMap) }
            }
        }
    }

    private fun loadNotifications() {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getNotifications(currentUserId).collect { notifications ->
                _uiState.update { it.copy(notifications = notifications, isLoading = false) }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.markAsRead(currentUserId, notificationId)
        }
    }
}
