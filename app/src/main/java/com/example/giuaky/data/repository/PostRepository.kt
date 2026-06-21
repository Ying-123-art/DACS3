package com.example.giuaky.data.repository

import com.example.giuaky.data.model.Notification
import com.example.giuaky.data.model.Post
import com.example.giuaky.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository {
    private val db = FirebaseDatabase.getInstance().getReference("posts")
    private val notificationRepo = NotificationRepository()

    fun getAllPosts(): Flow<List<Post>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.mapNotNull { data ->
                    data.getValue(Post::class.java)?.apply { id = data.key ?: "" }
                }.sortedByDescending { it.timestamp }
                trySend(posts)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        db.addValueEventListener(listener)
        awaitClose { db.removeEventListener(listener) }
    }

    fun getPostsByUser(userId: String): Flow<List<Post>> = callbackFlow {
        val query = db.orderByChild("userId").equalTo(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.mapNotNull { data ->
                    data.getValue(Post::class.java)?.apply { id = data.key ?: "" }
                }.sortedByDescending { it.timestamp }
                trySend(posts)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun getPost(postId: String): Post? {
        return try {
            val snapshot = db.child(postId).get().await()
            snapshot.getValue(Post::class.java)?.apply { id = postId }
        } catch (e: Exception) { null }
    }

    suspend fun createPost(post: Post): Result<Unit> {
        return try {
            val postId = db.push().key ?: return Result.failure(Exception("Lỗi tạo ID"))
            val newPost = post.apply { 
                id = postId
                status = "pending" 
            }
            db.child(postId).setValue(newPost).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sharePost(originalPost: Post, currentUserId: String, currentUser: User, sharedText: String): Result<Unit> {
        return try {
            val postId = db.push().key ?: return Result.failure(Exception("Lỗi tạo ID"))
            val sharedPost = Post(
                id = postId,
                userId = currentUserId,
                authorName = currentUser.displayName,
                authorAvatarUrl = currentUser.avatarUrl,
                title = originalPost.title,
                content = originalPost.content,
                imageUrls = originalPost.imageUrls,
                location = originalPost.location,
                latitude = originalPost.latitude,
                longitude = originalPost.longitude,
                timestamp = System.currentTimeMillis(),
                status = "approved", 
                isShared = true,
                originalPostId = originalPost.id,
                sharedContent = sharedText,
                originalAuthorName = originalPost.authorName
            )
            db.child(postId).setValue(sharedPost).await()

            val notification = Notification(
                toUserId = originalPost.userId,
                fromUserId = currentUserId,
                fromUserName = currentUser.displayName,
                fromUserAvatar = currentUser.avatarUrl,
                postId = postId,
                type = "share",
                content = "đã chia sẻ bài viết của bạn",
                timestamp = System.currentTimeMillis()
            )
            notificationRepo.addNotification(notification)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, title: String, content: String, imageUrls: List<String>): Result<Unit> {
        return try {
            val updates = mapOf(
                "title" to title,
                "content" to content,
                "imageUrls" to imageUrls,
                "status" to "pending"
            )
            db.child(postId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePostStatus(postId: String, status: String): Result<Unit> {
        return try {
            db.child(postId).child("status").setValue(status).await()
            if (status == "approved") {
                val post = getPost(postId)
                if (post != null) {
                    val followers = UserRepository().getFollowers(post.userId)
                    followers.forEach { followerId ->
                        val notification = Notification(
                            toUserId = followerId,
                            fromUserId = post.userId,
                            fromUserName = post.authorName,
                            fromUserAvatar = post.authorAvatarUrl,
                            postId = post.id,
                            type = "new_post",
                            content = "vừa đăng bài viết mới: ${post.title}",
                            timestamp = System.currentTimeMillis()
                        )
                        notificationRepo.addNotification(notification)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            db.child(postId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String, userId: String, currentlyLiked: Boolean): Result<Unit> {
        return try {
            val likeRef = db.child(postId).child("likedBy").child(userId)
            val countRef = db.child(postId).child("likesCount")
            if (currentlyLiked) {
                likeRef.removeValue().await()
                countRef.get().await().getValue(Int::class.java)?.let { count ->
                    countRef.setValue(maxOf(0, count - 1)).await()
                }
            } else {
                likeRef.setValue(true).await()
                countRef.get().await().getValue(Int::class.java)?.let { count ->
                    countRef.setValue(count + 1).await()
                }
                
                val postSnapshot = db.child(postId).get().await()
                val post = postSnapshot.getValue(Post::class.java)
                val userSnapshot = FirebaseDatabase.getInstance().getReference("users").child(userId).get().await()
                val user = userSnapshot.getValue(User::class.java)

                if (post != null && user != null) {
                    val notification = Notification(
                        toUserId = post.userId,
                        fromUserId = userId,
                        fromUserName = user.displayName,
                        fromUserAvatar = user.avatarUrl,
                        postId = postId,
                        type = "like",
                        content = "đã thích bài viết của bạn",
                        timestamp = System.currentTimeMillis()
                    )
                    notificationRepo.addNotification(notification)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
