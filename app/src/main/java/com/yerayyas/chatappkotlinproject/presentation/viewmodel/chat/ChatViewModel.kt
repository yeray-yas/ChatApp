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
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
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
    private var currentChatId: String? = null
    private var otherUserId: String? = null

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    fun loadMessages(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        this.otherUserId = otherUserId
        currentChatId = if (currentUserId < otherUserId) {
            "$currentUserId-$otherUserId"
        } else {
            "$otherUserId-$currentUserId"
        }

        Log.d("ChatViewModel", "Loading messages for chat: $currentChatId")
        Log.d("ChatViewModel", "Current user: $currentUserId, Other user: $otherUserId")

        messagesListener = database.child("Chats").child("Messages").child(currentChatId!!)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messagesList = snapshot.children.mapNotNull { messageSnapshot ->
                        messageSnapshot.getValue(ChatMessage::class.java)
                    }.sortedBy { it.timestamp }
                    _messages.value = messagesList
                    
                    // Log del último mensaje
                    if (messagesList.isNotEmpty()) {
                        val lastMessage = messagesList.last()
                        Log.d("ChatViewModel", "Last message status: ${lastMessage.readStatus}")
                        Log.d("ChatViewModel", "Last message sender: ${lastMessage.senderId}")
                        Log.d("ChatViewModel", "Last message receiver: ${lastMessage.receiverId}")

                        // Si hay mensajes no leídos dirigidos a nosotros, marcarlos como leídos
                        val unreadMessages = messagesList.filter { 
                            it.receiverId == currentUserId && it.readStatus != ReadStatus.READ 
                        }
                        if (unreadMessages.isNotEmpty()) {
                            markMessagesAsRead()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Error loading messages", error.toException())
                }
            })
    }

    private fun markMessagesAsRead() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val chatId = currentChatId ?: return@launch

                Log.d("ChatViewModel", "Marking messages as read for chat: $chatId")

                // Obtener todos los mensajes no leídos
                val messagesRef = database.child("Chats").child("Messages").child(chatId)
                val snapshot = messagesRef.get().await()
                
                var updatedCount = 0
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(ChatMessage::class.java)
                    // Solo marcar como leídos los mensajes dirigidos a nosotros
                    if (message?.receiverId == currentUserId && message.readStatus != ReadStatus.READ) {
                        Log.d("ChatViewModel", "Updating message ${message.id} to READ")
                        // Actualizar el estado de lectura
                        messageSnapshot.ref.child("readStatus").setValue(ReadStatus.READ)
                        updatedCount++
                    }
                }
                Log.d("ChatViewModel", "Updated $updatedCount messages to READ")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking messages as read", e)
            }
        }
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
                    messageType = MessageType.TEXT,
                    readStatus = ReadStatus.SENT
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
                    messageType = MessageType.IMAGE,
                    readStatus = ReadStatus.SENT
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
