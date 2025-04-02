package com.yerayyas.chatappkotlinproject.presentation.viewmodel.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.yerayyas.chatappkotlinproject.data.model.ChatListItem
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ChatsListViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : ViewModel() {

    private val _chats = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chats: StateFlow<List<ChatListItem>> = _chats.asStateFlow()

    private val _unreadMessagesCount = MutableStateFlow(0)
    val unreadMessagesCount: StateFlow<Int> = _unreadMessagesCount.asStateFlow()

    init {
        loadChats()
        loadUnreadMessagesCount()
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return
        Log.d("ChatsListViewModel", "Loading chats for user: $currentUserId")
        
        val chatsRef = database.child("Chats").child("Messages")
        Log.d("ChatsListViewModel", "Chats reference: Chats/Messages")

        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                Log.d("ChatsListViewModel", "Data changed. Number of chats: ${snapshot.childrenCount}")
                val chatList = mutableListOf<ChatListItem>()
                
                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key ?: return@forEach
                    Log.d("ChatsListViewModel", "Processing chat: $chatId")
                    
                    // Verificar si el chat pertenece al usuario actual
                    if (chatId.contains(currentUserId)) {
                        Log.d("ChatsListViewModel", "Chat belongs to current user")
                        val messages = chatSnapshot.children.mapNotNull { messageSnapshot ->
                            messageSnapshot.getValue(ChatMessage::class.java)
                        }
                        Log.d("ChatsListViewModel", "Number of messages in chat: ${messages.size}")
                        
                        if (messages.isNotEmpty()) {
                            val lastMessage = messages.maxByOrNull { it.timestamp }
                            lastMessage?.let { message ->
                                // Obtener el ID del otro usuario del chatId
                                val otherUserId = if (chatId.startsWith(currentUserId)) {
                                    chatId.substring(currentUserId.length + 1) // +1 para el guión
                                } else {
                                    chatId.substring(0, chatId.length - currentUserId.length - 1)
                                }
                                Log.d("ChatsListViewModel", "Other user ID: $otherUserId")
                                
                                // Calcular mensajes no leídos para este chat
                                val unreadCount = messages.count { 
                                    it.receiverId == currentUserId && it.readStatus != ReadStatus.READ 
                                }
                                
                                // Obtener el nombre de usuario del otro usuario
                                database.child("Users").child(otherUserId)
                                    .child("public")
                                    .child("username")
                                    .get()
                                    .addOnSuccessListener { usernameSnapshot ->
                                        val username = usernameSnapshot.getValue(String::class.java) ?: "Usuario"
                                        Log.d("ChatsListViewModel", "Other username: $username")
                                        
                                        chatList.add(
                                            ChatListItem(
                                                chatId = chatId,
                                                otherUserId = otherUserId,
                                                otherUsername = username,
                                                lastMessage = message.message,
                                                timestamp = message.timestamp,
                                                unreadCount = unreadCount
                                            )
                                        )
                                        
                                        _chats.value = chatList.sortedByDescending { it.timestamp }
                                        Log.d("ChatsListViewModel", "Updated chats list. Size: ${_chats.value.size}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ChatsListViewModel", "Error getting username", e)
                                    }
                            }
                        }
                    }
                }
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("ChatsListViewModel", "Error loading chats", error.toException())
            }
        })
    }

    private fun loadUnreadMessagesCount() {
        val currentUserId = auth.currentUser?.uid ?: return
        Log.d("ChatsListViewModel", "Loading unread messages count for user: $currentUserId")
        
        val chatsRef = database.child("Chats").child("Messages")
        
        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                var totalUnread = 0
                
                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key ?: return@forEach
                    
                    // Verificar si el chat pertenece al usuario actual
                    if (chatId.contains(currentUserId)) {
                        chatSnapshot.children.forEach { messageSnapshot ->
                            val message = messageSnapshot.getValue(ChatMessage::class.java)
                            if (message?.receiverId == currentUserId && message.readStatus != ReadStatus.READ) {
                                totalUnread++
                            }
                        }
                    }
                }
                
                Log.d("ChatsListViewModel", "Total unread messages: $totalUnread")
                _unreadMessagesCount.value = totalUnread
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("ChatsListViewModel", "Error loading unread messages count", error.toException())
            }
        })
    }
} 