package com.example.giuaky.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(), // Store as list, will use TypeConverter
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val status: String = "pending",
    
    // Sharing fields
    val isShared: Boolean = false,
    val originalPostId: String? = null,
    val sharedContent: String = "",
    val originalAuthorName: String = ""
)
