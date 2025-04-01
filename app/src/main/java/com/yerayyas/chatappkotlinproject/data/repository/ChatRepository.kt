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
 * Repositorio para manejar las operaciones relacionadas con los mensajes de chat.
 * Abstrae las operaciones de base de datos y almacenamiento.
 */
@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val database: DatabaseReference,
    private val storage: FirebaseStorage
) {

    // Scope para lanzar corrutinas desde dentro de los callbacks
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    /**
     * Obtiene el ID del usuario actual.
     * @return ID del usuario actual o cadena vacía si no hay usuario autenticado
     */
    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    /**
     * Obtiene el ID del chat a partir de los IDs de usuarios.
     * @param userId1 ID del primer usuario
     * @param userId2 ID del segundo usuario
     * @return ID del chat formado por la combinación de los IDs de usuario
     */
    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "$userId1-$userId2"
        } else {
            "$userId2-$userId1"
        }
    }

    /**
     * Obtiene los mensajes de un chat como un Flow.
     * @param otherUserId ID del otro usuario en el chat
     * @return Flow de lista de mensajes ordenados por timestamp
     */
    fun getMessages(otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = requireCurrentUserId()

        val chatId = getChatId(currentUserId, otherUserId)
        val messagesRef = database.child("Chats").child("Messages").child(chatId)
            .orderByChild("timestamp")

        val listener = messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val messagesList = snapshot.children.mapNotNull { messageSnapshot ->
                        messageSnapshot.getValue(ChatMessage::class.java)
                    }.sortedBy { it.timestamp }

                    trySend(messagesList)

                    // Si hay mensajes no leídos dirigidos a nosotros, marcarlos como leídos
                    val unreadMessages = messagesList.filter {
                        it.receiverId == currentUserId && it.readStatus != ReadStatus.READ
                    }
                    if (unreadMessages.isNotEmpty()) {
                        // Lanzar en una corrutina separada, ya que markMessagesAsRead es una función suspendida
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
     * Marca los mensajes como leídos en Firebase.
     * @param chatId ID del chat
     */
    suspend fun markMessagesAsRead(chatId: String) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId.isEmpty()) return

            Log.d(TAG, "Marking messages as read for chat: $chatId")

            // Obtener todos los mensajes no leídos
            val messagesRef = database.child("Chats").child("Messages").child(chatId)
            val snapshot = messagesRef.get().await()

            var updatedCount = 0
            snapshot.children.forEach { messageSnapshot ->
                val message = messageSnapshot.getValue(ChatMessage::class.java)
                if (message?.receiverId == currentUserId && message.readStatus != ReadStatus.READ) {
                    Log.d(TAG, "Updating message ${message.id} to READ")
                    // Actualizar el estado de lectura
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
     * Envía un mensaje de texto al otro usuario.
     * @param receiverId ID del usuario receptor
     * @param messageText Texto del mensaje a enviar
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
     * Envía una imagen al otro usuario.
     * @param receiverId ID del usuario receptor
     * @param imageUri URI de la imagen a enviar
     */
    suspend fun sendImageMessage(receiverId: String, imageUri: Uri) {
        try {
            val currentUserId = requireCurrentUserId()

            val chatId = getChatId(currentUserId, receiverId)

            // Subir imagen a Firebase Storage
            val imageFileName = "chat_images/${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child(imageFileName)
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            // Crear mensaje con la imagen
            val messageRef = database.child("Chats").child("Messages").child(chatId).push()
            val message = ChatMessage(
                id = messageRef.key ?: UUID.randomUUID().toString(),
                senderId = currentUserId,
                receiverId = receiverId,
                message = "Imagen",
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
     * Obtiene el ID del usuario actual o lanza una excepción si no está autenticado.
     * @return ID del usuario autenticado
     * @throws IllegalStateException si no hay usuario autenticado
     */
    private fun requireCurrentUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException(context.getString(R.string.no_authenticated_user))
    }
} 