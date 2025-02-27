package com.yerayyas.chatappkotlinproject.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: DatabaseReference,
    private val firebaseStorage: FirebaseStorage
) : ChatRepository {

    private val firebaseUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override suspend fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return
        val currentUserId = firebaseUser?.uid ?: return

        val chatId = generateChatId(currentUserId, receiverId)
        val messageKey = database.child("Chats").child("Messages").child(chatId).push().key ?: return

        val chatMessageMap = mapOf(
            "id" to messageKey,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "message" to messageText,
            "mediaUrl" to null,
            "messageType" to "text",
            "timestamp" to ServerValue.TIMESTAMP
        )

        database.child("Chats")
            .child("Messages")
            .child(chatId)
            .child(messageKey)
            .updateChildren(chatMessageMap)
    }

    override suspend fun sendMediaMessage(receiverId: String, fileUri: Uri, messageType: String) {
        val currentUserId = firebaseUser?.uid ?: return
        val chatId = generateChatId(currentUserId, receiverId)

        val filename = "${System.currentTimeMillis()}_${fileUri.lastPathSegment}"
        val storageRef = firebaseStorage.reference.child("chatMedia").child(chatId).child(filename)

        val downloadUrl = withContext(Dispatchers.IO) {
            storageRef.putFile(fileUri).await().storage.downloadUrl.await().toString()
        }

        val messageKey = database.child("Chats").child("Messages").child(chatId).push().key ?: return

        val chatMessageMap = mapOf(
            "id" to messageKey,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "message" to "",
            "mediaUrl" to downloadUrl,
            "messageType" to messageType,
            "timestamp" to ServerValue.TIMESTAMP
        )

        database.child("Chats").child("Messages").child(chatId).child(messageKey).updateChildren(chatMessageMap)
    }

    override fun loadMessages(receiverId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = firebaseUser?.uid ?: return@callbackFlow
        val chatId = generateChatId(currentUserId, receiverId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messagesList = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(messagesList.sortedBy { it.timestamp }).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val messagesRef = database.child("Chats").child("Messages").child(chatId)
        messagesRef.addValueEventListener(listener)

        awaitClose { messagesRef.removeEventListener(listener) }
    }

    private fun generateChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }
}
