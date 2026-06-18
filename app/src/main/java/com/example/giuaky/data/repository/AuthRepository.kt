package com.example.giuaky.data.repository

import com.example.giuaky.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Không tìm thấy người dùng"))

            // Lấy role từ Realtime Database (bảng users, cột role)
            val role = getUserRoleFromRealtimeDB(uid)
            val user = User(uid = uid, email = email, role = role)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Đăng ký thất bại"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            val user = User(
                uid = uid,
                displayName = displayName,
                email = email,
                role = "user",
                createdAt = System.currentTimeMillis()
            )

            // Lưu vào Realtime Database
            db.child("users").child(uid).setValue(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRoleFromRealtimeDB(uid: String): String {
        return try {
            val snapshot = db.child("users").child(uid).child("role").get().await()
            snapshot.getValue(String::class.java) ?: "user"
        } catch (e: Exception) {
            "user"
        }
    }

    fun logout() = auth.signOut()
}
