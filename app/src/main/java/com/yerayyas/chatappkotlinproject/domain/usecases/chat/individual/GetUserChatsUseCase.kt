package com.yerayyas.chatappkotlinproject.domain.usecases.chat.individual

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yerayyas.chatappkotlinproject.data.model.ChatListItem
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "GetUserChatsUseCase"

/**
 * A use case that provides a real-time stream of the user's chat list.
 *
 * This use case encapsulates the complex logic of fetching all chat conversations
 * for the current user, identifying the last message, calculating the unread count
 * for each chat, and retrieving the other participant's user information.
 *
 * It listens to the root "Chats/Messages" node and processes all relevant conversations,
 * emitting a complete `List<ChatListItem>` whenever the data changes. This provides
 * an efficient, single source of truth for the chat list UI.
 *
 * @property chatRepository The repository to get the current user's ID.
 * @property firebaseDatabase A direct reference to the Firebase Database to perform complex queries.
 */
class GetUserChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    @Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase
) {

    /**
     * Executes the use case.
     *
     * @return A [Flow] that emits a list of [ChatListItem]s.
     */
    operator fun invoke(): Flow<List<ChatListItem>> = callbackFlow {
        val currentUserId = chatRepository.getCurrentUserId()
        if (currentUserId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val chatsRef = firebaseDatabase.reference.child("Chats").child("Messages")

        var currentJob: kotlinx.coroutines.Job? = null

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Cancel the previous work, if exists
                currentJob?.cancel()
                // Use a coroutine to handle async fetching of usernames
                currentJob = launch {
                    val chatListItems = snapshot.children.mapNotNull { chatSnapshot ->
                        val chatId = chatSnapshot.key
                        if (chatId == null || !chatId.contains(currentUserId)) {
                            return@mapNotNull null
                        }

                        // Efficiently get all messages at once
                        val messages =
                            chatSnapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                        val lastMessage = messages.maxByOrNull { it.timestamp }

                        if (lastMessage == null) {
                            return@mapNotNull null
                        }

                        val otherUserId = if (chatId.startsWith(currentUserId)) {
                            chatId.substring(currentUserId.length + 1)
                        } else {
                            chatId.substring(0, chatId.length - currentUserId.length - 1)
                        }

                        val unreadCount = messages.count {
                            it.receiverId == currentUserId && it.readStatus != ReadStatus.READ
                        }

                        // Fetch username asynchronously
                        val username = try {
                            firebaseDatabase.reference.child("Users").child(otherUserId)
                                .child("public").child("username").get().await()
                                .getValue(String::class.java) ?: "User"
                        } catch (e: Exception) {
                            if (this.isActive) {
                                Log.e(TAG, "Failed to fetch username for $otherUserId", e)
                            }
                            // Re-throw if critical, or manage cancellation/skip item
                            return@mapNotNull null
                        }

                        ChatListItem(
                            chatId = chatId,
                            otherUserId = otherUserId,
                            otherUsername = username,
                            lastMessage = lastMessage.message,
                            timestamp = lastMessage.timestamp,
                            unreadCount = unreadCount
                        )
                    }

                    // Sort the final list and send it to the flow
                    val sortedList = chatListItems.sortedByDescending { it.timestamp }
                    trySend(sortedList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Chat list listener was cancelled.", error.toException())
                close(error.toException())
            }
        }

        chatsRef.addValueEventListener(listener)

        // When the flow is cancelled, remove the Firebase listener and also cancel the ongoing job
        awaitClose {
            Log.d(TAG, "Closing chat list listener and current job.")
            chatsRef.removeEventListener(listener)
            currentJob?.cancel() // Ensures that if the flow dies, any pending asynchronous work also dies
        }
    }
}
