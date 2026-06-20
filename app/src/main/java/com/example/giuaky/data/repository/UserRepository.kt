package com.example.giuaky.data.repository

import com.example.giuaky.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

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
            db.child("users").child(uid).child("avatarUrl").setValue(base64Avatar).await()
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

    // --- CÁC HÀM CHO ADMIN (SỬ DỤNG REALTIME DATABASE) ---

    // 1. Kiểm tra role của một user
    suspend fun getUserRole(uid: String): String {
        return try {
            val snapshot = db.child("users").child(uid).child("role").get().await()
            snapshot.getValue(String::class.java) ?: "user"
        } catch (e: Exception) {
            "user"
        }
    }

    // 2. Thay đổi role
    suspend fun updateUserRole(uid: String, newRole: String): Result<Unit> {
        return try {
            db.child("users").child(uid).child("role").setValue(newRole).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
