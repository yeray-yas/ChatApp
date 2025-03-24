package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference,
    private val storage: FirebaseStorage
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private var messagesListener: ValueEventListener? = null

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    fun loadMessages(receiverId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatId = if (currentUserId < receiverId) {
            "$currentUserId-$receiverId"
        } else {
            "$receiverId-$currentUserId"
        }

        messagesListener = database.child("Chats").child("Messages").child(chatId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messagesList = snapshot.children.mapNotNull { messageSnapshot ->
                        messageSnapshot.getValue(ChatMessage::class.java)
                    }.sortedBy { it.timestamp }
                    _messages.value = messagesList
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Error loading messages", error.toException())
                }
            })
    }

    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return

        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val chatId = if (currentUserId < receiverId) {
                    "$currentUserId-$receiverId"
                } else {
                    "$receiverId-$currentUserId"
                }

                val messageRef = database.child("Chats").child("Messages").child(chatId).push()
                val message = ChatMessage(
                    id = messageRef.key ?: "",
                    senderId = currentUserId,
                    receiverId = receiverId,
                    message = messageText,
                    timestamp = System.currentTimeMillis(),
                    messageType = MessageType.TEXT
                )

                messageRef.setValue(message)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
            }
        }
    }

    fun sendImage(receiverId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val chatId = if (currentUserId < receiverId) {
                    "$currentUserId-$receiverId"
                } else {
                    "$receiverId-$currentUserId"
                }

                // Subir imagen a Firebase Storage
                val imageFileName = "chat_images/${UUID.randomUUID()}.jpg"
                val imageRef = storage.reference.child(imageFileName)
                imageRef.putFile(imageUri).await()
                val imageUrl = imageRef.downloadUrl.await().toString()

                // Crear mensaje con la imagen
                val messageRef = database.child("Chats").child("Messages").child(chatId).push()
                val message = ChatMessage(
                    id = messageRef.key ?: "",
                    senderId = currentUserId,
                    receiverId = receiverId,
                    message = "Imagen",
                    timestamp = System.currentTimeMillis(),
                    imageUrl = imageUrl,
                    messageType = MessageType.IMAGE
                )

                messageRef.setValue(message)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending image", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.let { database.removeEventListener(it) }
    }
}
