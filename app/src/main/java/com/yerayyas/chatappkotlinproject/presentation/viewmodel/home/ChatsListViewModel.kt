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

/**
 * ViewModel responsible for managing and loading chat data for the chat list.
 *
 * This ViewModel handles the logic for fetching the list of chats, including
 * messages and the number of unread messages for each chat. It listens for
 * changes in the Firebase Realtime Database and updates the UI accordingly.
 *
 * Dependencies injected via Hilt:
 * - FirebaseAuth: Used to get the current user's authentication status and user ID.
 * - DatabaseReference: Used to access Firebase Realtime Database and load chat data.
 *
 * The ViewModel exposes the following properties:
 * - [chats]: A state flow containing the list of chat items.
 * - [unreadMessagesCount]: A state flow containing the total number of unread messages.
 */
@HiltViewModel
class ChatsListViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : ViewModel() {

    /**
     * A state flow representing the list of chat items.
     */
    private val _chats = MutableStateFlow<List<ChatListItem>>(emptyList())
    val chats: StateFlow<List<ChatListItem>> = _chats.asStateFlow()

    /**
     * A state flow representing the total number of unread messages.
     */
    private val _unreadMessagesCount = MutableStateFlow(0)
    val unreadMessagesCount: StateFlow<Int> = _unreadMessagesCount.asStateFlow()

    /**
     * Initializes the ViewModel by loading chats and unread messages count.
     */
    init {
        loadChats()
        loadUnreadMessagesCount()
    }

    /**
     * Loads the list of chats for the current user from the Firebase Realtime Database.
     * This function listens for changes to the chat data and updates the list accordingly.
     */
    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        val chatsRef = database.child("Chats").child("Messages")

        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val chatList = mutableListOf<ChatListItem>()

                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key ?: return@forEach

                    // Check if the chat belongs to the current user
                    if (chatId.contains(currentUserId)) {
                        val messages = chatSnapshot.children.mapNotNull { messageSnapshot ->
                            messageSnapshot.getValue(ChatMessage::class.java)
                        }

                        if (messages.isNotEmpty()) {
                            val lastMessage = messages.maxByOrNull { it.timestamp }
                            lastMessage?.let { message ->
                                val otherUserId = if (chatId.startsWith(currentUserId)) {
                                    chatId.substring(currentUserId.length + 1)
                                } else {
                                    chatId.substring(0, chatId.length - currentUserId.length - 1)
                                }

                                // Calculate unread message count for this chat
                                val unreadCount = messages.count {
                                    it.receiverId == currentUserId && it.readStatus != ReadStatus.READ
                                }

                                // Fetch the other user's username
                                database.child("Users").child(otherUserId)
                                    .child("public")
                                    .child("username")
                                    .get()
                                    .addOnSuccessListener { usernameSnapshot ->
                                        val username = usernameSnapshot.getValue(String::class.java) ?: "User"

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

    /**
     * Loads the total count of unread messages for the current user from the Firebase Realtime Database.
     * This function listens for changes to the message data and updates the unread message count.
     */
    private fun loadUnreadMessagesCount() {
        val currentUserId = auth.currentUser?.uid ?: return

        val chatsRef = database.child("Chats").child("Messages")

        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                var totalUnread = 0

                snapshot.children.forEach { chatSnapshot ->
                    val chatId = chatSnapshot.key ?: return@forEach

                    // Check if the chat belongs to the current user
                    if (chatId.contains(currentUserId)) {
                        chatSnapshot.children.forEach { messageSnapshot ->
                            val message = messageSnapshot.getValue(ChatMessage::class.java)
                            if (message?.receiverId == currentUserId && message.readStatus != ReadStatus.READ) {
                                totalUnread++
                            }
                        }
                    }
                }

                _unreadMessagesCount.value = totalUnread
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("ChatsListViewModel", "Error loading unread messages count", error.toException())
            }
        })
    }
}
