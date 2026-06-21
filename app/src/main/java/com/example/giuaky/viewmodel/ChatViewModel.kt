package com.example.giuaky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giuaky.data.model.ChatMessage
import com.example.giuaky.data.model.ChatRoom
import com.example.giuaky.data.model.User
import com.example.giuaky.data.repository.ChatRepository
import com.example.giuaky.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val currentUserId: String = "",
    val otherUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {
    private val chatRepo = ChatRepository()
    private val userRepo = UserRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(ChatUiState(currentUserId = currentUserId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadChatRooms() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            chatRepo.getChatRooms(currentUserId).collect { rooms ->
                val roomsWithUserInfo = rooms.map { room ->
                    val otherUserId = room.participantIds.find { it != currentUserId } ?: ""
                    val otherUser = userRepo.getUserProfile(otherUserId).getOrNull()
                    room.copy(
                        otherUserName = otherUser?.displayName ?: "Người dùng",
                        otherUserAvatar = otherUser?.avatarUrl ?: ""
                    )
                }
                _uiState.update { it.copy(rooms = roomsWithUserInfo, isLoading = false) }
            }
        }
    }

    fun loadChatDetail(roomId: String, otherUserId: String) {
        // Tải thông tin người kia để hiển thị tên trên thanh tiêu đề ngay lập tức
        viewModelScope.launch {
            val userResult = userRepo.getUserProfile(otherUserId)
            _uiState.update { it.copy(otherUser = userResult.getOrNull()) }
        }
        
        // Tải tin nhắn
        viewModelScope.launch {
            chatRepo.getMessages(roomId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage(receiverId: String, messageText: String, imageUrl: String = "") {
        if (messageText.isBlank() && imageUrl.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(currentUserId, receiverId, messageText, imageUrl)
        }
    }
}
