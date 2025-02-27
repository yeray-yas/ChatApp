package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: DatabaseReference,
    private val firebaseStorage: FirebaseStorage
) : ViewModel() {

    private val firebaseUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private var messagesListener: ValueEventListener? = null
    private var currentChatId: String? = null

    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return
        val currentUserId = firebaseUser?.uid ?: return

        val chatId = if (currentUserId < receiverId) "$currentUserId-$receiverId" else "$receiverId-$currentUserId"
        val messageKey = database.child("Chats").child("Messages").child(chatId).push().key ?: return

        // Map con ServerValue.TIMESTAMP para que sea el servidor quien asigne la hora real
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
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("ChatViewModel", "Error sending message", task.exception)
                } else {
                    Log.d("ChatViewModel", "Message sent successfully")
                    updateMessageLists(currentUserId, receiverId, chatId)
                }
            }
    }

    fun sendMediaMessage(receiverId: String, fileUri: Uri, messageType: String) {
        val currentUserId = firebaseUser?.uid ?: return
        val chatId = if (currentUserId < receiverId) "$currentUserId-$receiverId" else "$receiverId-$currentUserId"

        val filename = "${System.currentTimeMillis()}_${fileUri.lastPathSegment}"
        val storageRef = firebaseStorage.reference.child("chatMedia").child(chatId).child(filename)

        storageRef.putFile(fileUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Error uploading file")
                }
                storageRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result.toString()
                    val messageKey = database.child("Chats")
                        .child("Messages")
                        .child(chatId)
                        .push().key ?: return@addOnCompleteListener

                    val chatMessageMap = mapOf(
                        "id" to messageKey,
                        "senderId" to currentUserId,
                        "receiverId" to receiverId,
                        "message" to "",
                        "mediaUrl" to downloadUrl,
                        "messageType" to messageType,
                        "timestamp" to ServerValue.TIMESTAMP
                    )

                    database.child("Chats")
                        .child("Messages")
                        .child(chatId)
                        .child(messageKey)
                        .updateChildren(chatMessageMap)
                        .addOnCompleteListener { msgTask ->
                            if (!msgTask.isSuccessful) {
                                Log.e("ChatViewModel", "Error sending media message", msgTask.exception)
                            } else {
                                Log.d("ChatViewModel", "Media message sent successfully")
                                updateMessageLists(currentUserId, receiverId, chatId)
                            }
                        }
                } else {
                    Log.e("ChatViewModel", "Error uploading media", task.exception)
                }
            }
    }

    private fun updateMessageLists(currentUserId: String, receiverId: String, chatId: String) {
        val senderMessageList = database.child("Chats")
            .child("SenderMessagesList")
            .child(currentUserId)
            .child(receiverId)

        val receiverMessageList = database.child("Chats")
            .child("ReceiverMessagesList")
            .child(receiverId)
            .child(currentUserId)

        senderMessageList.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    senderMessageList.child(chatId).setValue(receiverId)
                }
                receiverMessageList.child(chatId).setValue(currentUserId)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Error updating message lists", error.toException())
            }
        })
    }

    fun loadMessages(receiverId: String) {
        val currentUserId = firebaseUser?.uid ?: return
        val chatId = if (currentUserId < receiverId) "$currentUserId-$receiverId" else "$receiverId-$currentUserId"
        currentChatId = chatId

        messagesListener?.let {
            database.child("Chats").child("Messages").child(chatId).removeEventListener(it)
        }

        messagesListener = database.child("Chats").child("Messages").child(chatId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messagesList = mutableListOf<ChatMessage>()
                    snapshot.children.forEach { child ->
                        val map = child.value as? Map<*, *> ?: return@forEach
                        val time = map["timestamp"] as? Long ?: 0L

                        val chatMessage = ChatMessage(
                            id = map["id"] as? String ?: "",
                            senderId = map["senderId"] as? String ?: "",
                            receiverId = map["receiverId"] as? String ?: "",
                            message = map["message"] as? String ?: "",
                            mediaUrl = map["mediaUrl"] as? String,
                            messageType = map["messageType"] as? String ?: "text",
                            timestamp = time
                        )
                        messagesList.add(chatMessage)
                    }
                    _messages.value = messagesList.sortedBy { it.timestamp }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Error loading messages", error.toException())
                }
            })
    }

    override fun onCleared() {
        super.onCleared()
        currentChatId?.let { chatId ->
            messagesListener?.let { listener ->
                database.child("Chats").child("Messages").child(chatId).removeEventListener(listener)
            }
        }
    }
}

