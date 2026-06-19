package com.example.giuaky.data.repository

import com.example.giuaky.data.model.Comment
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

class CommentRepository {
    private val db = FirebaseDatabase.getInstance().reference
    private val notificationRepo = NotificationRepository()

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val ref = db.child("comments").child(postId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children.mapNotNull { data ->
                    data.getValue(Comment::class.java)?.copy(id = data.key ?: "")
                }.sortedBy { it.timestamp }
                trySend(comments)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            val ref = db.child("comments").child(comment.postId)
            val commentId = ref.push().key ?: return Result.failure(Exception("Lỗi tạo ID"))
            val newComment = comment.copy(id = commentId)
            ref.child(commentId).setValue(newComment).await()

            // Update comment count
            val countRef = db.child("posts").child(comment.postId).child("commentsCount")
            val currentCount = countRef.get().await().getValue(Int::class.java) ?: 0
            countRef.setValue(currentCount + 1).await()

            // Gửi thông báo khi có người bình luận
            val postSnapshot = db.child("posts").child(comment.postId).get().await()
            val post = postSnapshot.getValue(Post::class.java)
            val userSnapshot = db.child("users").child(comment.userId).get().await()
            val user = userSnapshot.getValue(User::class.java)

            if (post != null && user != null) {
                val notification = Notification(
                    toUserId = post.userId,
                    fromUserId = comment.userId,
                    fromUserName = user.displayName,
                    fromUserAvatar = user.avatarUrl,
                    postId = comment.postId,
                    type = "comment",
                    content = "đã bình luận: ${comment.content}",
                    timestamp = System.currentTimeMillis()
                )
                notificationRepo.addNotification(notification)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            db.child("comments").child(postId).child(commentId).removeValue().await()
            val countRef = db.child("posts").child(postId).child("commentsCount")
            val currentCount = countRef.get().await().getValue(Int::class.java) ?: 0
            countRef.setValue(maxOf(0, currentCount - 1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
