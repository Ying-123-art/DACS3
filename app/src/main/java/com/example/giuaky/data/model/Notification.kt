package com.example.giuaky.data.model

data class Notification(
    val id: String = "",
    val toUserId: String = "", // User who receives the notification
    val fromUserId: String = "", // User who triggered the notification
    val fromUserName: String = "",
    val fromUserAvatar: String = "",
    val postId: String = "",
    val type: String = "", // "like" or "comment"
    val content: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
