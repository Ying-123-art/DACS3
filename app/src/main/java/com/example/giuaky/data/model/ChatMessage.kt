package com.example.giuaky.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class ChatMessage(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("senderId") @set:PropertyName("senderId") var senderId: String = "",
    @get:PropertyName("receiverId") @set:PropertyName("receiverId") var receiverId: String = "",
    @get:PropertyName("message") @set:PropertyName("message") var message: String = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = 0L,
    @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false
)

@IgnoreExtraProperties
data class ChatRoom(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("participantIds") @set:PropertyName("participantIds") var participantIds: List<String> = emptyList(),
    @get:PropertyName("lastMessage") @set:PropertyName("lastMessage") var lastMessage: String = "",
    @get:PropertyName("lastMessageTimestamp") @set:PropertyName("lastMessageTimestamp") var lastMessageTimestamp: Long = 0L,
    // Helper fields for UI
    var otherUserName: String = "",
    var otherUserAvatar: String = ""
)
