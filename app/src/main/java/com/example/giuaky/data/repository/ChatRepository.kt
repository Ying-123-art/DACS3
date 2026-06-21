package com.example.giuaky.data.repository

import com.example.giuaky.data.model.ChatMessage
import com.example.giuaky.data.model.ChatRoom
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseDatabase.getInstance().reference

    fun getChatRooms(userId: String): Flow<List<ChatRoom>> = callbackFlow {
        val roomsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = snapshot.children.mapNotNull { it.getValue(ChatRoom::class.java)?.apply { id = it.key ?: "" } }
                    .filter { it.participantIds.contains(userId) }
                trySend(rooms)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        
        val roomsRef = db.child("chatRooms")
        roomsRef.addValueEventListener(roomsListener)
        awaitClose { roomsRef.removeEventListener(roomsListener) }
    }

    fun getMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = db.child("messages").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java)?.apply { id = it.key ?: "" } }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    suspend fun sendMessage(senderId: String, receiverId: String, messageText: String, imageUrl: String = ""): Result<Unit> {
        return try {
            val roomId = if (senderId < receiverId) "${senderId}_${receiverId}" else "${receiverId}_${senderId}"
            val messageId = db.child("messages").child(roomId).push().key ?: return Result.failure(Exception("Could not generate ID"))
            
            val timestamp = System.currentTimeMillis()
            val chatMessage = ChatMessage(
                id = messageId,
                senderId = senderId,
                receiverId = receiverId,
                message = messageText,
                imageUrl = imageUrl,
                timestamp = timestamp
            )

            val updates = hashMapOf<String, Any>(
                "messages/$roomId/$messageId" to chatMessage,
                "chatRooms/$roomId/lastMessage" to if (imageUrl.isNotEmpty() && messageText.isEmpty()) "Đã gửi một ảnh" else messageText,
                "chatRooms/$roomId/lastMessageTimestamp" to timestamp,
                "chatRooms/$roomId/participantIds" to listOf(senderId, receiverId),
                "chatRooms/$roomId/id" to roomId
            )

            db.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
