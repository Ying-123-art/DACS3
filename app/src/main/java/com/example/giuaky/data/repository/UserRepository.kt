package com.example.giuaky.data.repository

import com.example.giuaky.data.model.Notification
import com.example.giuaky.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val notificationRepo = NotificationRepository()

    suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val snapshot = db.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java) ?: User(uid = uid)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, displayName: String, bio: String): Result<Unit> {
        return try {
            val updates = mapOf("displayName" to displayName, "bio" to bio)
            db.child("users").child(uid).updateChildren(updates).await()
            
            // Cập nhật cả tên trong các bài đăng của user này
            val postsSnapshot = db.child("posts").orderByChild("userId").equalTo(uid).get().await()
            val postUpdates = mutableMapOf<String, Any?>()
            postsSnapshot.children.forEach { post ->
                postUpdates["posts/${post.key}/authorName"] = displayName
            }
            if (postUpdates.isNotEmpty()) {
                db.updateChildren(postUpdates).await()
            }

            // Also update Firebase Auth display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAvatar(uid: String, base64Avatar: String): Result<Unit> {
        return try {
            // 1. Cập nhật avatar trong profile user
            db.child("users").child(uid).child("avatarUrl").setValue(base64Avatar).await()

            // 2. Cập nhật avatar trong tất cả bài đăng của user này để đồng bộ
            val postsSnapshot = db.child("posts").orderByChild("userId").equalTo(uid).get().await()
            val postUpdates = mutableMapOf<String, Any?>()
            postsSnapshot.children.forEach { post ->
                postUpdates["posts/${post.key}/authorAvatarUrl"] = base64Avatar
            }
            
            if (postUpdates.isNotEmpty()) {
                db.updateChildren(postUpdates).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = db.child("users").get().await()
            val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FOLLOW SYSTEM ---

    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            db.child("following").child(currentUserId).child(targetUserId).setValue(true).await()
            db.child("followers").child(targetUserId).child(currentUserId).setValue(true).await()

            // Gửi thông báo khi có người theo dõi
            val currentUserSnapshot = db.child("users").child(currentUserId).get().await()
            val currentUser = currentUserSnapshot.getValue(User::class.java)

            if (currentUser != null) {
                val notification = Notification(
                    toUserId = targetUserId,
                    fromUserId = currentUserId,
                    fromUserName = currentUser.displayName,
                    fromUserAvatar = currentUser.avatarUrl,
                    postId = "", 
                    type = "follow",
                    content = "đã bắt đầu theo dõi bạn",
                    timestamp = System.currentTimeMillis()
                )
                notificationRepo.addNotification(notification)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            db.child("following").child(currentUserId).child(targetUserId).removeValue().await()
            db.child("followers").child(targetUserId).child(currentUserId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val snapshot = db.child("following").child(currentUserId).child(targetUserId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowers(userId: String): List<String> {
        return try {
            val snapshot = db.child("followers").child(userId).get().await()
            snapshot.children.mapNotNull { it.key }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- CÁC HÀM CHO ADMIN ---
    suspend fun getUserRole(uid: String): String {
        return try {
            val snapshot = db.child("users").child(uid).child("role").get().await()
            snapshot.getValue(String::class.java) ?: "user"
        } catch (e: Exception) {
            "user"
        }
    }

    suspend fun updateUserRole(uid: String, newRole: String): Result<Unit> {
        return try {
            db.child("users").child(uid).child("role").setValue(newRole).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
