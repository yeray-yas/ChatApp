package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: DatabaseReference
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
        // Genera un chatId único basado en ambos IDs (orden alfabético para asegurar consistencia)
        val chatId = if (currentUserId < receiverId) "$currentUserId-$receiverId" else "$receiverId-$currentUserId"
        val messageKey = database.child("Chats").child("Messages").child(chatId).push().key ?: return

        val chatMessage = ChatMessage(
            id = messageKey,
            senderId = currentUserId,
            receiverId = receiverId,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        // Save the message in "Chats/Messages/{chatId}/{messageKey}"
        database.child("Chats").child("Messages").child(chatId).child(messageKey)
            .setValue(chatMessage)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("ChatViewModel", "Error sending message", task.exception)
                }else{
                    Log.d("ChatViewModel", "Message sent successfully")
                    val senderMessageList = database
                        .child("Chats")
                        .child("SenderMessagesList")
                        .child(currentUserId)
                        .child(receiverId)

                    val receiverMessageList = database
                        .child("Chats")
                        .child("ReceiverMessagesList")
                        .child(receiverId)
                        .child(currentUserId)

                    senderMessageList.addListenerForSingleValueEvent(object: ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if(!snapshot.exists()){
                                senderMessageList.child(chatId).setValue(receiverId)
                            }

                            receiverMessageList.child(chatId).setValue(currentUserId)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("ChatViewModel", "Error updating message lists", error.toException())
                        }

                    })
                }
            }
    }

    fun loadMessages(receiverId: String) {
        val currentUserId = firebaseUser?.uid ?: return
        val chatId = if (currentUserId < receiverId) "$currentUserId-$receiverId" else "$receiverId-$currentUserId"
        currentChatId = chatId  // Guardamos el chatId actual para poder remover el listener en onCleared

        // Remueve el listener previo para evitar duplicados
        messagesListener?.let {
            database.child("Chats").child(chatId).removeEventListener(it)
        }

        messagesListener = database.child("Chats").child(chatId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messagesList = mutableListOf<ChatMessage>()
                    snapshot.children.forEach { child ->
                        child.getValue(ChatMessage::class.java)?.let { messagesList.add(it) }
                    }
                    // Ordena de forma ascendente para que el primer mensaje aparezca en la parte superior
                    _messages.value = messagesList.sortedBy { it.timestamp }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Error loading messages", error.toException())
                }
            })
    }

    override fun onCleared() {
        currentChatId?.let { chatId ->
            messagesListener?.let { listener ->
                database.child("Chats").child(chatId).removeEventListener(listener)
            }
        }
        super.onCleared()
    }
}

