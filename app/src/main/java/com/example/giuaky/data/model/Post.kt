package com.example.giuaky.data.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Post(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("authorName") @set:PropertyName("authorName") var authorName: String = "Anonymous",
    @get:PropertyName("authorAvatarUrl") @set:PropertyName("authorAvatarUrl") var authorAvatarUrl: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("content") @set:PropertyName("content") var content: String = "",
    @get:PropertyName("imageUrls") @set:PropertyName("imageUrls") var imageUrls: List<String> = emptyList(),
    @get:PropertyName("location") @set:PropertyName("location") var location: String = "",
    @get:PropertyName("latitude") @set:PropertyName("latitude") var latitude: Double = 0.0,
    @get:PropertyName("longitude") @set:PropertyName("longitude") var longitude: Double = 0.0,
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = 0L,
    @get:PropertyName("likesCount") @set:PropertyName("likesCount") var likesCount: Int = 0,
    @get:PropertyName("commentsCount") @set:PropertyName("commentsCount") var commentsCount: Int = 0,
    @get:PropertyName("likedBy") @set:PropertyName("likedBy") var likedBy: Map<String, Boolean> = emptyMap(),
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "pending",
    
    // Sharing properties
    @get:PropertyName("isShared") @set:PropertyName("isShared") var isShared: Boolean = false,
    @get:PropertyName("originalPostId") @set:PropertyName("originalPostId") var originalPostId: String? = null,
    @get:PropertyName("sharedContent") @set:PropertyName("sharedContent") var sharedContent: String = "",
    @get:PropertyName("originalAuthorName") @set:PropertyName("originalAuthorName") var originalAuthorName: String = ""
) {
    // Backward compatibility for single imageUrl if needed
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String
        get() = imageUrls.firstOrNull() ?: ""
        set(value) {
            if (imageUrls.isEmpty() && value.isNotEmpty()) {
                imageUrls = listOf(value)
            }
        }
}
