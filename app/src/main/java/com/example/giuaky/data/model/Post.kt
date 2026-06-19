package com.example.giuaky.data.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "Anonymous",
    val authorAvatarUrl: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val likedBy: Map<String, Boolean> = emptyMap(),
    val status: String = "pending", // "pending", "approved", "rejected"
    
    // Sharing properties
    val isShared: Boolean = false,
    val originalPostId: String? = null,
    val sharedContent: String = "", // The text added by the person sharing
    val originalAuthorName: String = ""
)
