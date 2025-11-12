package com.yerayyas.chatappkotlinproject.data.model

/**
 * Represents a message specific to group chats
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
    // No-argument constructor required by Firebase
    constructor() : this(
        "", "", "", "", null, "", 0L, GroupMessageType.TEXT, null,
        ReadStatus.SENT, emptyMap(), false, null, null,
        emptyMap(), false, null, emptyList(), false, null
    )

    /**
     * Checks if the message has been read by a specific user
     */
    fun isReadBy(userId: String): Boolean = readBy.containsKey(userId)

    /**
     * Gets the timestamp of when it was read by a user
     */
    fun getReadTimestamp(userId: String): Long? = readBy[userId]

    /**
     * Gets the list of users who have read the message
     */
    fun getReadByUsers(): List<String> = readBy.keys.toList()

    /**
     * Gets the count of users who have read the message
     */
    fun getReadCount(): Int = readBy.size

    /**
     * Checks if the message has reactions
     */
    fun hasReactions(): Boolean = reactions.isNotEmpty()

    /**
     * Gets all unique reactions on the message
     */
    fun getUniqueReactions(): List<String> = reactions.keys.toList()

    /**
     * Gets the count of a specific reaction
     */
    fun getReactionCount(emoji: String): Int = reactions[emoji]?.size ?: 0

    /**
     * Checks if a user has reacted with a specific emoji
     */
    fun hasUserReacted(userId: String, emoji: String): Boolean {
        return reactions[emoji]?.containsKey(userId) == true
    }

    /**
     * Gets all reactions from a user
     */
    fun getUserReactions(userId: String): List<String> {
        return reactions.filter { it.value.containsKey(userId) }.keys.toList()
    }

    /**
     * Checks if the message is a reply to another message
     */
    fun isReply(): Boolean = replyToMessageId != null

    /**
     * Checks if the message mentions specific users
     */
    fun hasMentions(): Boolean = mentionedUsers.isNotEmpty()

    /**
     * Checks if it mentions a specific user
     */
    fun mentionsUser(userId: String): Boolean = mentionedUsers.contains(userId)

    /**
     * Gets the message content for search (without mentions)
     */
    fun getSearchableContent(): String {
        return message.replace(Regex("@\\w+"), "").trim()
    }

    /**
     * Converts the group message to ChatMessage for compatibility
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
     * Checks if the message can be edited by a user
     */
    fun canBeEditedBy(userId: String): Boolean {
        val isOwner = senderId == userId
        val isNotTooOld = System.currentTimeMillis() - timestamp < (15 * 60 * 1000) // 15 minutes
        val isTextMessage = messageType == GroupMessageType.TEXT
        return isOwner && isNotTooOld && isTextMessage && !isSystemMessage
    }

    /**
     * Checks if the message can be deleted by a user
     */
    fun canBeDeletedBy(userId: String, isAdmin: Boolean): Boolean {
        val isOwner = senderId == userId
        return (isOwner || isAdmin) && !isSystemMessage
    }
}

/**
 * Represents read confirmations for a group message
 */
data class MessageReadReceipt(
    val messageId: String = "",
    val groupId: String = "",
    val readBy: Map<String, Long> = emptyMap(), // userId -> timestamp
    val deliveredTo: Map<String, Long> = emptyMap() // userId -> timestamp
) {
    constructor() : this("", "", emptyMap(), emptyMap())

    /**
     * Adds a read receipt
     */
    fun addReadReceipt(
        userId: String,
        timestamp: Long = System.currentTimeMillis()
    ): MessageReadReceipt {
        return copy(readBy = readBy + (userId to timestamp))
    }

    /**
     * Adds a delivery receipt
     */
    fun addDeliveryReceipt(
        userId: String,
        timestamp: Long = System.currentTimeMillis()
    ): MessageReadReceipt {
        return copy(deliveredTo = deliveredTo + (userId to timestamp))
    }

    /**
     * Gets the users who have read the message
     */
    fun getReadUsers(): List<String> = readBy.keys.toList()

    /**
     * Gets the users to whom the message has been delivered
     */
    fun getDeliveredUsers(): List<String> = deliveredTo.keys.toList()

    /**
     * Checks if it was read by a specific user
     */
    fun isReadBy(userId: String): Boolean = readBy.containsKey(userId)

    /**
     * Checks if it was delivered to a specific user
     */
    fun isDeliveredTo(userId: String): Boolean = deliveredTo.containsKey(userId)
}

/**
 * Message types specific to groups (extended from base MessageType)
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