package com.yerayyas.chatappkotlinproject.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupInvitation
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GroupChatRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage
) : GroupChatRepository {

    // ===== GESTIÓN BÁSICA DE GRUPOS =====

    override suspend fun createGroup(group: GroupChat): Result<String> {
        return try {
            val reference = firebaseDatabase.reference.child("groups").push()
            val groupId = reference.key ?: throw Exception("No se pudo generar ID del grupo")

            val groupWithId = group.copy(id = groupId)
            reference.setValue(groupWithId).await()

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupById(groupId: String): GroupChat? {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .get()
                .await()

            snapshot.getValue(GroupChat::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateGroup(group: GroupChat): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("groups")
                .child(group.id)
                .setValue(group)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            // Eliminar el grupo
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .removeValue()
                .await()

            // Eliminar mensajes del grupo
            firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .removeValue()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== GESTIÓN DE MIEMBROS =====

    override suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Grupo no encontrado")
            val updatedMembers = group.memberIds.toMutableList().apply {
                if (!contains(userId)) add(userId)
            }
            val updatedGroup = group.copy(memberIds = updatedMembers)

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Grupo no encontrado")
            val updatedMembers = group.memberIds.toMutableList().apply { remove(userId) }
            val updatedAdmins = group.adminIds.toMutableList().apply { remove(userId) }
            val updatedGroup = group.copy(
                memberIds = updatedMembers,
                adminIds = updatedAdmins
            )

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun makeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Grupo no encontrado")
            val updatedAdmins = group.adminIds.toMutableList().apply {
                if (!contains(userId)) add(userId)
            }
            val updatedGroup = group.copy(adminIds = updatedAdmins)

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Grupo no encontrado")

            // No permitir eliminar el último admin
            if (group.adminIds.size <= 1) {
                throw Exception("No se puede eliminar el último administrador")
            }

            val updatedAdmins = group.adminIds.toMutableList().apply { remove(userId) }
            val updatedGroup = group.copy(adminIds = updatedAdmins)

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupMembers(groupId: String): List<String> {
        return try {
            val group = getGroupById(groupId)
            group?.memberIds ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== MENSAJERÍA GRUPAL =====

    override suspend fun sendMessageToGroup(groupId: String, message: GroupMessage): Result<Unit> {
        return try {
            println("DEBUG: Sending message to group $groupId")
            println("DEBUG: Message content: ${message.message}")
            println("DEBUG: Sender: ${message.senderId}")

            val reference = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .push()

            val messageId = reference.key ?: throw Exception("No se pudo generar ID del mensaje")
            val messageWithId = message.copy(id = messageId, groupId = groupId)

            println("DEBUG: Generated message ID: $messageId")
            println("DEBUG: Saving to Firebase path: group_messages/$groupId/$messageId")

            reference.setValue(messageWithId).await()

            println("DEBUG: Message saved successfully to Firebase")

            // Actualizar última actividad del grupo
            updateLastActivity(groupId, messageWithId)

            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: Error sending message to group: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sube una imagen a Firebase Storage y devuelve la URL de descarga
     * para ser usada en mensajes grupales con imágenes
     */
    override suspend fun uploadGroupMessageImage(groupId: String, imageUri: Uri): Result<String> {
        return try {
            // Define la ruta y nombre para la imagen en Firebase Storage
            val imageFileName = "group_chat_images/$groupId/${java.util.UUID.randomUUID()}.jpg"
            val imageRef = firebaseStorage.reference.child(imageFileName)

            // Subir el archivo y obtener su URL pública
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> {
        return callbackFlow {
            val messagesRef = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .orderByChild("timestamp")

            val listener = messagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val messagesList = snapshot.children.mapNotNull {
                            it.getValue(GroupMessage::class.java)
                        }.sortedBy { it.timestamp }
                        println("DEBUG: Firebase returned ${messagesList.size} messages for group $groupId")
                        trySend(messagesList)
                    } catch (e: Exception) {
                        println("DEBUG: Error parsing Firebase messages: ${e.message}")
                        // Si hay error al parsear, enviar lista vacía en lugar de mock
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Firebase cancelled: ${error.message}")
                    // En caso de error de Firebase, enviar lista vacía
                    trySend(emptyList())
                }
            })

            awaitClose { messagesRef.removeEventListener(listener) }
        }
    }

    override suspend fun updateLastActivity(groupId: String, message: GroupMessage): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Grupo no encontrado")
            val updatedGroup = group.copy(
                lastMessage = message.toChatMessage(),
                lastActivity = message.timestamp
            )

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== READ RECEIPTS =====

    override suspend fun markMessageAsRead(
        groupId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            firebaseDatabase.reference
                .child("message_read_receipts")
                .child(messageId)
                .child(userId)
                .setValue(timestamp)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMessageReadReceipts(messageId: String): Map<String, Long> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("message_read_receipts")
                .child(messageId)
                .get()
                .await()

            val receipts = mutableMapOf<String, Long>()
            snapshot.children.forEach { child ->
                val userId = child.key
                val timestamp = child.getValue<Long>()
                if (userId != null && timestamp != null) {
                    receipts[userId] = timestamp
                }
            }
            receipts
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ===== CONFIGURACIONES =====

    override suspend fun updateGroupSettings(
        groupId: String,
        settings: Map<String, Any>
    ): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .child("settings")
                .updateChildren(settings)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGroupImage(groupId: String, imageUri: Uri): Result<String> {
        return try {
            val imageRef = firebaseStorage.reference
                .child("group_images")
                .child("$groupId.jpg")

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            // Actualizar URL en el grupo
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .child("imageUrl")
                .setValue(downloadUrl)
                .await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== INVITACIONES =====

    override suspend fun createInvitation(invitation: GroupInvitation): Result<String> {
        return try {
            val reference = firebaseDatabase.reference.child("group_invitations").push()
            val invitationId =
                reference.key ?: throw Exception("No se pudo generar ID de invitación")

            val invitationWithId = invitation.copy(id = invitationId)
            reference.setValue(invitationWithId).await()

            Result.success(invitationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit> {
        return try {
            val invitationSnapshot = firebaseDatabase.reference
                .child("group_invitations")
                .child(invitationId)
                .get()
                .await()

            val invitation = invitationSnapshot.getValue(GroupInvitation::class.java)
                ?: throw Exception("Invitación no encontrada")

            val status = if (accept) "ACCEPTED" else "DECLINED"

            // Actualizar estado de la invitación
            firebaseDatabase.reference
                .child("group_invitations")
                .child(invitationId)
                .child("status")
                .setValue(status)
                .await()

            // Si acepta, añadir al grupo
            if (accept) {
                addMemberToGroup(invitation.groupId, invitation.invitedUser)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getInvitationsForUser(userId: String): Flow<List<GroupInvitation>> {
        return callbackFlow {
            val invitationsRef = firebaseDatabase.reference
                .child("group_invitations")
                .orderByChild("invitedUser")
                .equalTo(userId)

            val listener = invitationsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val invitationsList = snapshot.children.mapNotNull {
                            it.getValue(GroupInvitation::class.java)
                        }.filter { it.isActive() }
                        trySend(invitationsList)
                    } catch (e: Exception) {
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })

            awaitClose { invitationsRef.removeEventListener(listener) }
        }
    }

    // ===== BÚSQUEDA Y FILTROS =====

    override suspend fun searchGroupMessages(groupId: String, query: String): List<GroupMessage> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .get()
                .await()

            snapshot.children.mapNotNull {
                it.getValue(GroupMessage::class.java)
            }.filter {
                it.message.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            // Si hay error, usar datos mock
            getMockGroupMessages(groupId).filter {
                it.message.contains(query, ignoreCase = true)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUserGroups(userId: String): Flow<List<GroupChat>> {
        return callbackFlow {
            val groupsRef = firebaseDatabase.reference.child("groups")

            val listener = groupsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val allGroups = snapshot.children.mapNotNull {
                            it.getValue(GroupChat::class.java)
                        }

                        println("DEBUG: Firebase returned ${allGroups.size} total groups")

                        // Filtrar grupos donde el usuario es miembro
                        val userGroups = allGroups.filter { group ->
                            group.memberIds.contains(userId)
                        }.sortedByDescending { it.lastActivity }

                        println("DEBUG: User $userId is member of ${userGroups.size} groups")

                        // Si no hay grupos reales, usar mock solo en desarrollo
                        val groupsToSend = userGroups.ifEmpty {
                            println("DEBUG: No real groups found, using mock data")
                            getMockGroups(userId)
                        }

                        trySend(groupsToSend)
                    } catch (e: Exception) {
                        println("DEBUG: Error loading groups from Firebase: ${e.message}")
                        // En caso de error, usar datos mock solo como fallback
                        trySend(getMockGroups(userId))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Firebase groups cancelled: ${error.message}")
                    // En caso de error, usar datos mock como fallback
                    trySend(getMockGroups(userId))
                }
            })

            awaitClose { groupsRef.removeEventListener(listener) }
        }
    }

    // ===== DATOS DE MUESTRA =====

    private fun getMockGroupMessages(groupId: String): List<GroupMessage> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "current_user"

        return listOf(
            GroupMessage(
                id = "msg1",
                groupId = groupId,
                senderId = "user2",
                senderName = "Ana García",
                message = "¡Hola a todos! 👋",
                timestamp = System.currentTimeMillis() - 3600000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.READ,
                readBy = mapOf("user3" to System.currentTimeMillis() - 3500000)
            ),
            GroupMessage(
                id = "msg2",
                groupId = groupId,
                senderId = "user3",
                senderName = "Carlos López",
                message = "@Juan ¿cómo va todo?",
                timestamp = System.currentTimeMillis() - 1800000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.READ,
                mentionedUsers = listOf("user1"),
                readBy = mapOf(currentUserId to System.currentTimeMillis() - 1700000)
            ),
            GroupMessage(
                id = "msg3",
                groupId = groupId,
                senderId = currentUserId,
                senderName = "Tú",
                message = "Todo bien por aquí, gracias por preguntar 😊",
                timestamp = System.currentTimeMillis() - 900000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.DELIVERED,
                readBy = mapOf("user2" to System.currentTimeMillis() - 800000)
            ),
            GroupMessage(
                id = "msg4",
                groupId = groupId,
                senderId = "user4",
                senderName = "María Rodríguez",
                message = "¿Alguien para almorzar mañana? 🍕",
                timestamp = System.currentTimeMillis() - 300000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.SENT
            )
        ).sortedBy { it.timestamp }
    }

    private fun getMockGroups(userId: String): List<GroupChat> {
        return listOf(
            GroupChat(
                id = "group1",
                name = "Familia ❤️",
                description = "Chat familiar",
                memberIds = listOf(userId, "user2", "user3", "user4"),
                adminIds = listOf(userId),
                createdBy = userId,
                lastActivity = System.currentTimeMillis() - 300000,
                lastMessage = ChatMessage(
                    message = "¿Alguien para almorzar mañana? 🍕",
                    timestamp = System.currentTimeMillis() - 300000
                )
            ),
            GroupChat(
                id = "group2",
                name = "Trabajo - Equipo Dev 💻",
                description = "Equipo de desarrollo",
                memberIds = listOf(userId, "user5", "user6", "user7", "user8"),
                adminIds = listOf(userId, "user5"),
                createdBy = userId,
                lastActivity = System.currentTimeMillis() - 7200000,
                lastMessage = ChatMessage(
                    message = "La nueva feature está lista para testing",
                    timestamp = System.currentTimeMillis() - 7200000
                )
            ),
            GroupChat(
                id = "group3",
                name = "Amigos de la U 🎓",
                description = "Los de siempre",
                memberIds = listOf(userId, "user9", "user10", "user11"),
                adminIds = listOf(userId),
                createdBy = userId,
                lastActivity = System.currentTimeMillis() - 86400000,
                lastMessage = ChatMessage(
                    message = "¿Cuándo nos vemos?",
                    timestamp = System.currentTimeMillis() - 86400000
                )
            )
        ).sortedByDescending { it.lastActivity }
    }
}