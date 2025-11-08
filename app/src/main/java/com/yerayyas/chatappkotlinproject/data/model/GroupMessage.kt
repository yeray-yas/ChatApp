package com.yerayyas.chatappkotlinproject.data.model

/**
 * Representa un mensaje específico para chats grupales
 */
data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderImageUrl: String? = null,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: GroupMessageType = GroupMessageType.TEXT,
    val imageUrl: String? = null,
    val readStatus: ReadStatus = ReadStatus.SENT,
    val readBy: Map<String, Long> = emptyMap(), // userId -> timestamp when read
    val isPinned: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToMessage: GroupMessage? = null,
    val reactions: Map<String, Map<String, Long>> = emptyMap(), // emoji -> (userId -> timestamp)
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val mentionedUsers: List<String> = emptyList(),
    val isSystemMessage: Boolean = false,
    val systemMessageType: GroupActivityType? = null
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this(
        "", "", "", "", null, "", 0L, GroupMessageType.TEXT, null,
        ReadStatus.SENT, emptyMap(), false, null, null,
        emptyMap(), false, null, emptyList(), false, null
    )

    /**
     * Verifica si el mensaje ha sido leído por un usuario específico
     */
    fun isReadBy(userId: String): Boolean = readBy.containsKey(userId)

    /**
     * Obtiene el timestamp de cuándo fue leído por un usuario
     */
    fun getReadTimestamp(userId: String): Long? = readBy[userId]

    /**
     * Obtiene la lista de usuarios que han leído el mensaje
     */
    fun getReadByUsers(): List<String> = readBy.keys.toList()

    /**
     * Obtiene el conteo de usuarios que han leído el mensaje
     */
    fun getReadCount(): Int = readBy.size

    /**
     * Verifica si el mensaje tiene reacciones
     */
    fun hasReactions(): Boolean = reactions.isNotEmpty()

    /**
     * Obtiene todas las reacciones únicas del mensaje
     */
    fun getUniqueReactions(): List<String> = reactions.keys.toList()

    /**
     * Obtiene el conteo de una reacción específica
     */
    fun getReactionCount(emoji: String): Int = reactions[emoji]?.size ?: 0

    /**
     * Verifica si un usuario ha reaccionado con un emoji específico
     */
    fun hasUserReacted(userId: String, emoji: String): Boolean {
        return reactions[emoji]?.containsKey(userId) == true
    }

    /**
     * Obtiene todas las reacciones de un usuario
     */
    fun getUserReactions(userId: String): List<String> {
        return reactions.filter { it.value.containsKey(userId) }.keys.toList()
    }

    /**
     * Verifica si el mensaje es una respuesta a otro mensaje
     */
    fun isReply(): Boolean = replyToMessageId != null

    /**
     * Verifica si el mensaje menciona a usuarios específicos
     */
    fun hasMentions(): Boolean = mentionedUsers.isNotEmpty()

    /**
     * Verifica si menciona a un usuario específico
     */
    fun mentionsUser(userId: String): Boolean = mentionedUsers.contains(userId)

    /**
     * Obtiene el contenido del mensaje para búsqueda (sin menciones)
     */
    fun getSearchableContent(): String {
        return message.replace(Regex("@\\w+"), "").trim()
    }

    /**
     * Convierte el mensaje grupal a ChatMessage para compatibilidad
     */
    fun toChatMessage(): ChatMessage {
        val compatibleMessageType = when (messageType) {
            GroupMessageType.TEXT, GroupMessageType.SYSTEM_MESSAGE,
            GroupMessageType.REPLY, GroupMessageType.PINNED_MESSAGE,
            GroupMessageType.MEMBER_JOINED, GroupMessageType.MEMBER_LEFT,
            GroupMessageType.ADMIN_PROMOTED, GroupMessageType.GROUP_CREATED,
            GroupMessageType.GROUP_NAME_CHANGED, GroupMessageType.GROUP_IMAGE_CHANGED -> MessageType.TEXT

            GroupMessageType.IMAGE -> MessageType.IMAGE
        }

        return ChatMessage(
            id = id,
            senderId = senderId,
            receiverId = groupId,
            message = message,
            timestamp = timestamp,
            messageType = compatibleMessageType,
            imageUrl = imageUrl,
            readStatus = readStatus
        )
    }

    /**
     * Verifica si el mensaje puede ser editado por un usuario
     */
    fun canBeEditedBy(userId: String): Boolean {
        val isOwner = senderId == userId
        val isNotTooOld = System.currentTimeMillis() - timestamp < (15 * 60 * 1000) // 15 minutos
        val isTextMessage = messageType == GroupMessageType.TEXT
        return isOwner && isNotTooOld && isTextMessage && !isSystemMessage
    }

    /**
     * Verifica si el mensaje puede ser eliminado por un usuario
     */
    fun canBeDeletedBy(userId: String, isAdmin: Boolean): Boolean {
        val isOwner = senderId == userId
        return (isOwner || isAdmin) && !isSystemMessage
    }
}

/**
 * Representa las confirmaciones de lectura de un mensaje grupal
 */
data class MessageReadReceipt(
    val messageId: String = "",
    val groupId: String = "",
    val readBy: Map<String, Long> = emptyMap(), // userId -> timestamp
    val deliveredTo: Map<String, Long> = emptyMap() // userId -> timestamp
) {
    constructor() : this("", "", emptyMap(), emptyMap())

    /**
     * Añade una confirmación de lectura
     */
    fun addReadReceipt(
        userId: String,
        timestamp: Long = System.currentTimeMillis()
    ): MessageReadReceipt {
        return copy(readBy = readBy + (userId to timestamp))
    }

    /**
     * Añade una confirmación de entrega
     */
    fun addDeliveryReceipt(
        userId: String,
        timestamp: Long = System.currentTimeMillis()
    ): MessageReadReceipt {
        return copy(deliveredTo = deliveredTo + (userId to timestamp))
    }

    /**
     * Obtiene los usuarios que han leído el mensaje
     */
    fun getReadUsers(): List<String> = readBy.keys.toList()

    /**
     * Obtiene los usuarios a los que se les ha entregado el mensaje
     */
    fun getDeliveredUsers(): List<String> = deliveredTo.keys.toList()

    /**
     * Verifica si fue leído por un usuario específico
     */
    fun isReadBy(userId: String): Boolean = readBy.containsKey(userId)

    /**
     * Verifica si fue entregado a un usuario específico
     */
    fun isDeliveredTo(userId: String): Boolean = deliveredTo.containsKey(userId)
}

/**
 * Tipos de mensaje específicos para grupos (extendidos del MessageType base)
 */
enum class GroupMessageType {
    TEXT,
    IMAGE,
    SYSTEM_MESSAGE,
    REPLY,
    PINNED_MESSAGE,
    MEMBER_JOINED,
    MEMBER_LEFT,
    ADMIN_PROMOTED,
    GROUP_CREATED,
    GROUP_NAME_CHANGED,
    GROUP_IMAGE_CHANGED
}