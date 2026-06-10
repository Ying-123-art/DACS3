package com.example.giuaky.data.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
