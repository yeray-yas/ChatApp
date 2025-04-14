package com.yerayyas.chatappkotlinproject.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatRepository"

/**
 * Repository responsible for handling chat-related operations.
 * This includes message retrieval, message sending, and marking messages as read.
 * It abstracts Firebase Realtime Database and Firebase Storage logic.
 *
 * @property context Application context used for error messages.
 * @property auth FirebaseAuth instance to access the current user.
 * @property database Firebase Realtime Database reference.
 * @property storage Firebase Storage reference for uploading images.
 */
@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val database: DatabaseReference,
    private val storage: FirebaseStorage
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    /**
     * Returns the ID of the currently authenticated user.
     *
     * @return The current user ID, or an empty string if no user is authenticated.
     */
    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Generates a unique chat ID based on two user IDs.
     *
     * @param userId1 ID of the first user.
     * @param userId2 ID of the second user.
     * @return A deterministic chat ID composed of both user IDs.
     */
    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "$userId1-$userId2"
        } else {
            "$userId2-$userId1"
        }
    }

    /**
     * Observes the list of messages in a chat as a cold Flow.
     * Automatically marks messages as read if they were unread and addressed to the current user.
     *
     * @param otherUserId ID of the other user in the chat.
     * @return A Flow emitting a sorted list of [ChatMessage]s by timestamp.
     */
    fun getMessages(otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = requireCurrentUserId()
        val chatId = getChatId(currentUserId, otherUserId)
        val messagesRef = database.child("Chats").child("Messages").child(chatId)
            .orderByChild("timestamp")

        val listener = messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val messagesList = snapshot.children.mapNotNull {
                        it.getValue(ChatMessage::class.java)
                    }.sortedBy { it.timestamp }

                    trySend(messagesList)

                    val unreadMessages = messagesList.filter {
                        it.receiverId == currentUserId && it.readStatus != ReadStatus.READ
                    }
                    if (unreadMessages.isNotEmpty()) {
                        repositoryScope.launch {
                            try {
                                markMessagesAsRead(chatId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error marking messages as read", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing messages", e)
                    close(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading messages", error.toException())
                close(error.toException())
            }
        })

        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    /**
     * Marks all unread messages directed to the current user in a specific chat as read.
     *
     * @param chatId The ID of the chat where messages should be updated.
     * @throws Exception If any database operation fails.
     */
    suspend fun markMessagesAsRead(chatId: String) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId.isEmpty()) return

            Log.d(TAG, "Marking messages as read for chat: $chatId")

            val messagesRef = database.child("Chats").child("Messages").child(chatId)
            val snapshot = messagesRef.get().await()

            var updatedCount = 0
            snapshot.children.forEach { messageSnapshot ->
                val message = messageSnapshot.getValue(ChatMessage::class.java)
                if (message?.receiverId == currentUserId && message.readStatus != ReadStatus.READ) {
                    Log.d(TAG, "Updating message ${message.id} to READ")
                    messageSnapshot.ref.child("readStatus").setValue(ReadStatus.READ).await()
                    updatedCount++
                }
            }

            Log.d(TAG, "Updated $updatedCount messages to READ")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            throw e
        }
    }

    /**
     * Sends a plain text message to another user.
     *
     * @param receiverId ID of the user who will receive the message.
     * @param messageText Content of the text message to send.
     * @throws Exception If message sending fails.
     */
    suspend fun sendTextMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return

        try {
            val currentUserId = requireCurrentUserId()
            val chatId = getChatId(currentUserId, receiverId)
            val messageRef = database.child("Chats").child("Messages").child(chatId).push()

            val message = ChatMessage(
                id = messageRef.key ?: UUID.randomUUID().toString(),
                senderId = currentUserId,
                receiverId = receiverId,
                message = messageText,
                timestamp = System.currentTimeMillis(),
                messageType = MessageType.TEXT,
                readStatus = ReadStatus.SENT
            )

            messageRef.setValue(message).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            throw e
        }
    }

    /**
     * Sends an image message to another user by uploading the image to Firebase Storage
     * and creating a message with the image's URL.
     *
     * @param receiverId ID of the user who will receive the image.
     * @param imageUri URI of the image to upload and send.
     * @throws Exception If image upload or message creation fails.
     */
    suspend fun sendImageMessage(receiverId: String, imageUri: Uri) {
        try {
            val currentUserId = requireCurrentUserId()
            val chatId = getChatId(currentUserId, receiverId)

            val imageFileName = "chat_images/${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child(imageFileName)
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            val messageRef = database.child("Chats").child("Messages").child(chatId).push()
            val message = ChatMessage(
                id = messageRef.key ?: UUID.randomUUID().toString(),
                senderId = currentUserId,
                receiverId = receiverId,
                message = "Image",
                timestamp = System.currentTimeMillis(),
                imageUrl = imageUrl,
                messageType = MessageType.IMAGE,
                readStatus = ReadStatus.SENT
            )

            messageRef.setValue(message).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending image", e)
            throw e
        }
    }

    /**
     * Returns the authenticated user's ID or throws an exception if the user is not signed in.
     *
     * @return The current user ID.
     * @throws IllegalStateException If no user is authenticated.
     */
    private fun requireCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException(context.getString(R.string.no_authenticated_user))
    }
}
