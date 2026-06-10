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
            val snapshot = db.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java) ?: User(uid = uid, email = email)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Đăng ký thất bại"))

            // Update Firebase Auth display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            // Save user profile to Realtime Database
            val user = User(
                uid = uid,
                displayName = displayName,
                email = email,
                role = "user",
                createdAt = System.currentTimeMillis()
            )
            db.child("users").child(uid).setValue(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRole(uid: String): String {
        return try {
            val snapshot = db.child("users").child(uid).child("role").get().await()
            snapshot.getValue(String::class.java) ?: "user"
        } catch (e: Exception) {
            "user"
        }
    }

    fun logout() = auth.signOut()
}
