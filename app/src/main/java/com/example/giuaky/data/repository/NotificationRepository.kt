package com.example.giuaky.data.repository

import com.example.giuaky.data.model.Notification
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseDatabase.getInstance().getReference("notifications")

    fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val query = db.child(userId).orderByChild("timestamp")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = snapshot.children.mapNotNull { data ->
                    data.getValue(Notification::class.java)?.copy(id = data.key ?: "")
                }.reversed() // Most recent first
                trySend(notifications)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun addNotification(notification: Notification) {
        if (notification.toUserId == notification.fromUserId) return // Don't notify self
        try {
            val ref = db.child(notification.toUserId)
            val id = ref.push().key ?: return
            ref.child(id).setValue(notification.copy(id = id)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAsRead(userId: String, notificationId: String) {
        try {
            db.child(userId).child(notificationId).child("read").setValue(true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
