package com.example.giuaky.data.repository

import android.net.Uri
import com.example.giuaky.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

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

    suspend fun uploadAvatar(uid: String, uri: Uri): Result<String> {
        return try {
            val ref = storage.child("avatars/$uid/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            db.child("users").child(uid).child("avatarUrl").setValue(url).await()
            Result.success(url)
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
}
