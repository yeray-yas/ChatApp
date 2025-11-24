package com.yerayyas.chatappkotlinproject.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
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

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val database: DatabaseReference,
    private val storage: FirebaseStorage
) : ChatRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "$userId1-$userId2"
        } else {
            "$userId2-$userId1"
        }
    }

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

                    val hasUnread = messagesList.any { it.receiverId == currentUserId && it.readStatus != ReadStatus.READ }
                    if (hasUnread) {
                        repositoryScope.launch {
                            try {
                                markMessagesAsRead(chatId)
                                resetUnreadCount(chatId, currentUserId, otherUserId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to mark messages as read", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    close(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    override suspend fun markMessagesAsRead(chatId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) return

        val messagesRef = database.child("Chats").child("Messages").child(chatId)
        val snapshot = messagesRef.get().await()

        snapshot.children.forEach { messageSnapshot ->
            val message = messageSnapshot.getValue(ChatMessage::class.java)
            if (message?.receiverId == currentUserId && message.readStatus != ReadStatus.READ) {
                messageSnapshot.ref.child("readStatus").setValue(ReadStatus.READ)
            }
        }

    }
    private suspend fun resetUnreadCount(chatId: String, currentUserId: String, otherUserId: String) {
        try {
            database.child("Chats").child("User-Chats")
                .child(currentUserId)
                .child(otherUserId)
                .child("unreadCount")
                .setValue(0)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting unread count", e)
        }
    }

    override suspend fun markMessageAsRead(messageId: String, userId: String) {
    }

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

        updateUserChatList(currentUserId, receiverId, messageText)
    }

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

        updateUserChatList(currentUserId, receiverId, messageText)
    }

    override suspend fun sendImageMessage(receiverId: String, imageUri: Uri) {
        val currentUserId = requireCurrentUserId()
        val chatId = getChatId(currentUserId, receiverId)

        val messageRef = database.child("Chats").child("Messages").child(chatId).push()
        val messageId = messageRef.key ?: UUID.randomUUID().toString()

        val tempMessage = ChatMessage(
            id = messageId,
            senderId = currentUserId,
            receiverId = receiverId,
            message = "📷 Photo",
            timestamp = System.currentTimeMillis(),
            imageUrl = "PENDING_UPLOAD",
            messageType = MessageType.IMAGE,
            readStatus = ReadStatus.SENT
        )

        messageRef.setValue(tempMessage).await()

        updateUserChatList(currentUserId, receiverId, "📷 Photo")

        try {
            val imageFileName = "chat_images/$messageId.jpg"
            val imageRef = storage.reference.child(imageFileName)

            imageRef.putFile(imageUri).await()
            val realImageUrl = imageRef.downloadUrl.await().toString()

            messageRef.child("imageUrl").setValue(realImageUrl).await()

        } catch (e: Exception) {
            messageRef.removeValue()
            throw e
        }
    }

    override suspend fun sendImageMessageReply(receiverId: String, imageUri: Uri, replyToMessage: ChatMessage) {
        val currentUserId = requireCurrentUserId()
        val chatId = getChatId(currentUserId, receiverId)

        val messageRef = database.child("Chats").child("Messages").child(chatId).push()
        val messageId = messageRef.key ?: UUID.randomUUID().toString()

        val tempMessage = ChatMessage(
            id = messageId,
            senderId = currentUserId,
            receiverId = receiverId,
            message = "📷 Photo",
            timestamp = System.currentTimeMillis(),
            imageUrl = "PENDING_UPLOAD",
            messageType = MessageType.IMAGE,
            readStatus = ReadStatus.SENT,
            replyToMessageId = replyToMessage.id,
            replyToMessage = if (replyToMessage.messageType == MessageType.IMAGE) "Image" else replyToMessage.message,
            replyToSenderId = replyToMessage.senderId,
            replyToMessageType = replyToMessage.messageType,
            replyToImageUrl = if (replyToMessage.messageType == MessageType.IMAGE) replyToMessage.imageUrl else null
        )

        messageRef.setValue(tempMessage).await()
        updateUserChatList(currentUserId, receiverId, "📷 Photo")

        try {
            val imageFileName = "chat_images/$messageId.jpg"
            val imageRef = storage.reference.child(imageFileName)
            imageRef.putFile(imageUri).await()
            val realImageUrl = imageRef.downloadUrl.await().toString()
            messageRef.child("imageUrl").setValue(realImageUrl).await()
        } catch (e: Exception) {
            messageRef.removeValue()
            throw e
        }
    }
    private suspend fun updateUserChatList(currentUserId: String, receiverId: String, lastMessage: String) {
        val timestamp = System.currentTimeMillis()

        val myChatUpdate = mapOf(
            "chatId" to getChatId(currentUserId, receiverId),
            "lastMessage" to lastMessage,
            "timestamp" to timestamp,
            "otherUserId" to receiverId
        )
        database.child("Chats").child("User-Chats")
            .child(currentUserId)
            .child(receiverId)
            .updateChildren(myChatUpdate)
            .await()

        val otherChatUpdate = mapOf(
            "chatId" to getChatId(currentUserId, receiverId),
            "lastMessage" to lastMessage,
            "timestamp" to timestamp,
            "otherUserId" to currentUserId,
            "unreadCount" to ServerValue.increment(1)
        )
        database.child("Chats").child("User-Chats")
            .child(receiverId)
            .child(currentUserId)
            .updateChildren(otherChatUpdate)
            .await()
    }

    private fun requireCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }
}
