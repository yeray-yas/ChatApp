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
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
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

private const val TAG = "ChatRepositoryImpl"

/**
 * A singleton implementation of the [ChatRepository] interface that handles all chat-related data operations.
 *
 * This repository orchestrates interactions with Firebase services:
 * - [FirebaseAuth] for retrieving the current user's session information.
 * - [FirebaseStorage] for uploading and managing image messages.
 * - [DatabaseReference] (Firebase Realtime Database) for storing and retrieving chat messages.
 *
 * It exposes methods for sending text and image messages, observing message streams, and managing message read status.
 *
 * @property context The application context, injected by Hilt.
 * @property auth An instance of [FirebaseAuth] to get the current authenticated user.
 * @property database A reference to the root of the Firebase Realtime Database.
 * @property storage An instance of [FirebaseStorage] for handling file uploads.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val database: DatabaseReference,
    private val storage: FirebaseStorage
) : ChatRepository {

    // A dedicated CoroutineScope for repository operations that should not be cancelled with the ViewModel.
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    /**
     * Returns the unique identifier (UID) of the currently authenticated user.
     *
     * @return The current user's UID as a [String], or an empty string if no user is signed in.
     */
    override fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Creates a deterministic, unique chat ID from two user IDs.
     * The ID is always the same regardless of the order of the user IDs.
     *
     * @param userId1 The ID of the first user.
     * @param userId2 The ID of the second user.
     * @return A unique [String] representing the chat channel between the two users.
     */
    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "$userId1-$userId2"
        } else {
            "$userId2-$userId1"
        }
    }

    /**
     * Establishes a real-time listener for messages in a specific chat and exposes them as a cold [Flow].
     *
     * When collected, this flow emits a list of all messages for the given chat, sorted by timestamp.
     * It also automatically triggers a background task to mark any unread messages for the current user as "READ".
     *
     * @param otherUserId The ID of the other participant in the chat.
     * @return A [Flow] that emits a `List<ChatMessage>` whenever the data changes in Firebase.
     */
    override fun getMessages(otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = requireCurrentUserId()
        val chatId = getChatId(currentUserId, otherUserId)
        val messagesRef = database.child("Chats").child("Messages").child(chatId)
            .orderByChild("timestamp")

        val listener = messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val messagesList = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                    trySend(messagesList)

                    // After sending the list, check for unread messages and mark them as read.
                    val hasUnread = messagesList.any { it.receiverId == currentUserId && it.readStatus != ReadStatus.READ }
                    if (hasUnread) {
                        repositoryScope.launch {
                            try {
                                markMessagesAsRead(chatId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to mark messages as read inside getMessages flow", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing messages onDataChange", e)
                    close(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Message listener cancelled", error.toException())
                close(error.toException())
            }
        })

        // When the flow is cancelled, remove the Firebase listener.
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    /**
     * Updates the `readStatus` of all messages sent to the current user in a specific chat to [ReadStatus.READ].
     *
     * @param chatId The unique ID of the chat to update.
     * @throws Exception if any of the database read or write operations fail.
     */
    override suspend fun markMessagesAsRead(chatId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) return

        val messagesRef = database.child("Chats").child("Messages").child(chatId)
        val snapshot = messagesRef.get().await()

        var updatedCount = 0
        snapshot.children.forEach { messageSnapshot ->
            val message = messageSnapshot.getValue(ChatMessage::class.java)
            if (message?.receiverId == currentUserId && message.readStatus != ReadStatus.READ) {
                messageSnapshot.ref.child("readStatus").setValue(ReadStatus.READ).await()
                updatedCount++
            }
        }

        if (updatedCount > 0) {
            Log.d(TAG, "Marked $updatedCount messages as READ in chat $chatId")
        }
    }

    /**
     * Marks a specific message as read by a user (for group chats).
     *
     * @param messageId The ID of the message to mark as read.
     * @param userId The ID of the user who read the message.
     * @throws Exception if the database operation fails.
     */
    override suspend fun markMessageAsRead(messageId: String, userId: String) {
        try {
            // For group messages, we'll store read receipts differently
            database.child("message_read_receipts")
                .child(messageId)
                .child(userId)
                .setValue(System.currentTimeMillis())
                .await()

            Log.d(TAG, "Marked message $messageId as read by user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark message as read", e)
            throw e
        }
    }

    /**
     * Sends a plain text message to another user.
     *
     * @param receiverId The ID of the user who will receive the message.
     * @param messageText The non-blank content of the text message.
     * @throws IllegalStateException if the current user is not authenticated.
     * @throws Exception if the database operation fails.
     */
    override suspend fun sendTextMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return

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
    }

    /**
     * Sends a plain text message as a reply to another message.
     *
     * @param receiverId The ID of the user who will receive the message.
     * @param messageText The non-blank content of the text message.
     * @param replyToMessage The original message being replied to.
     * @throws IllegalStateException if the current user is not authenticated.
     * @throws Exception if the database operation fails.
     */
    override suspend fun sendTextMessageReply(
        receiverId: String,
        messageText: String,
        replyToMessage: ChatMessage
    ) {
        if (messageText.isBlank()) return

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
            readStatus = ReadStatus.SENT,
            replyToMessageId = replyToMessage.id,
            replyToMessage = if (replyToMessage.messageType == MessageType.IMAGE) "Image" else replyToMessage.message,
            replyToSenderId = replyToMessage.senderId,
            replyToMessageType = replyToMessage.messageType,
            replyToImageUrl = if (replyToMessage.messageType == MessageType.IMAGE) replyToMessage.imageUrl else null
        )

        messageRef.setValue(message).await()
    }

    /**
     * Uploads an image to Firebase Storage and then sends a message containing the image URL.
     *
     * @param receiverId The ID of the user who will receive the image message.
     * @param imageUri The local [Uri] of the image to upload.
     * @throws IllegalStateException if the current user is not authenticated.
     * @throws Exception if the image upload or database operation fails.
     */
    override suspend fun sendImageMessage(receiverId: String, imageUri: Uri) {
        val currentUserId = requireCurrentUserId()
        val chatId = getChatId(currentUserId, receiverId)

        // Define the path and name for the image in Firebase Storage.
        val imageFileName = "chat_images/${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(imageFileName)

        // Upload the file and get its public URL.
        imageRef.putFile(imageUri).await()
        val imageUrl = imageRef.downloadUrl.await().toString()

        val messageRef = database.child("Chats").child("Messages").child(chatId).push()
        val message = ChatMessage(
            id = messageRef.key ?: UUID.randomUUID().toString(),
            senderId = currentUserId,
            receiverId = receiverId,
            message = "Image", // Fallback text for notifications or previews
            timestamp = System.currentTimeMillis(),
            imageUrl = imageUrl,
            messageType = MessageType.IMAGE,
            readStatus = ReadStatus.SENT
        )

        messageRef.setValue(message).await()
    }

    /**
     * Uploads an image to Firebase Storage and then sends a message containing the image URL as a reply.
     *
     * @param receiverId The ID of the user who will receive the image message.
     * @param imageUri The local [Uri] of the image to upload.
     * @param replyToMessage The original message being replied to.
     * @throws IllegalStateException if the current user is not authenticated.
     * @throws Exception if the image upload or database operation fails.
     */
    override suspend fun sendImageMessageReply(
        receiverId: String,
        imageUri: Uri,
        replyToMessage: ChatMessage
    ) {
        val currentUserId = requireCurrentUserId()
        val chatId = getChatId(currentUserId, receiverId)

        // Define the path and name for the image in Firebase Storage.
        val imageFileName = "chat_images/${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(imageFileName)

        // Upload the file and get its public URL.
        imageRef.putFile(imageUri).await()
        val imageUrl = imageRef.downloadUrl.await().toString()

        val messageRef = database.child("Chats").child("Messages").child(chatId).push()
        val message = ChatMessage(
            id = messageRef.key ?: UUID.randomUUID().toString(),
            senderId = currentUserId,
            receiverId = receiverId,
            message = "Image", // Fallback text for notifications or previews
            timestamp = System.currentTimeMillis(),
            imageUrl = imageUrl,
            messageType = MessageType.IMAGE,
            readStatus = ReadStatus.SENT,
            replyToMessageId = replyToMessage.id,
            replyToMessage = if (replyToMessage.messageType == MessageType.IMAGE) "Image" else replyToMessage.message,
            replyToSenderId = replyToMessage.senderId,
            replyToMessageType = replyToMessage.messageType,
            replyToImageUrl = if (replyToMessage.messageType == MessageType.IMAGE) replyToMessage.imageUrl else null
        )

        messageRef.setValue(message).await()
    }

    /**
     * A helper function that ensures a user is authenticated before proceeding.
     *
     * @return The current user's UID.
     * @throws IllegalStateException if no user is signed in.
     */
    private fun requireCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException(context.getString(R.string.no_authenticated_user))
    }
}
