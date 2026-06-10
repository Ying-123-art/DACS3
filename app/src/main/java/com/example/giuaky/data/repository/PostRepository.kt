package com.example.giuaky.data.repository

import android.net.Uri
import com.example.giuaky.data.model.Post
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository {
    private val db = FirebaseDatabase.getInstance().getReference("posts")
    private val storage = FirebaseStorage.getInstance().reference

    fun getAllPosts(): Flow<List<Post>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.mapNotNull { data ->
                    data.getValue(Post::class.java)?.copy(id = data.key ?: "")
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
                    data.getValue(Post::class.java)?.copy(id = data.key ?: "")
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
            snapshot.getValue(Post::class.java)?.copy(id = postId)
        } catch (e: Exception) { null }
    }

    suspend fun createPost(post: Post, imageUri: Uri?): Result<Unit> {
        return try {
            val postId = db.push().key ?: return Result.failure(Exception("Lỗi tạo ID"))
            val imageUrl = if (imageUri != null) uploadImage(imageUri) else ""
            val newPost = post.copy(id = postId, imageUrl = imageUrl, status = "pending")
            db.child(postId).setValue(newPost).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, title: String, content: String, imageUrl: String, newImageUri: Uri?): Result<Unit> {
        return try {
            val finalImageUrl = if (newImageUri != null) uploadImage(newImageUri) else imageUrl
            val updates = mapOf(
                "title" to title,
                "content" to content,
                "imageUrl" to finalImageUrl,
                "status" to "pending" // Reset to pending after edit
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String, imageUrl: String): Result<Unit> {
        return try {
            db.child(postId).removeValue().await()
            if (imageUrl.isNotEmpty()) {
                try { storage.storage.getReferenceFromUrl(imageUrl).delete().await() } catch (_: Exception) {}
            }
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
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPostsOnce(): List<Post> {
        return try {
            val snapshot = db.get().await()
            snapshot.children.mapNotNull { data ->
                data.getValue(Post::class.java)?.copy(id = data.key ?: "")
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAllUsersCount(): Int {
        return try {
            val snapshot = FirebaseDatabase.getInstance().getReference("users").get().await()
            snapshot.childrenCount.toInt()
        } catch (e: Exception) { 0 }
    }

    private suspend fun uploadImage(uri: Uri): String {
        val ref = storage.child("images/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
