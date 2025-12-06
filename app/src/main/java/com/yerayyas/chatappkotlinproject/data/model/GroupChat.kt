package com.yerayyas.chatappkotlinproject.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

/**
 * Represents a complete group chat with all advanced features.
 *
 * This data class encapsulates all information and functionality related to group chat management,
 * including member administration, message handling, permissions, and group settings.
 * It provides a comprehensive model for group chat operations in the application.
 *
 * Key features:
 * - Complete member and admin management
 * - Advanced permission system
 * - Group settings and customization
 * - Message tracking and activity monitoring
 * - Firebase compatibility with no-argument constructor
 * - Utility methods for common operations
 * - Support for pinned messages and member muting
 *
 * The class includes multiple utility methods for checking permissions, managing members,
 * and determining user capabilities within the group context.
 */
@IgnoreExtraProperties
data class GroupChat(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessage: ChatMessage? = null,
    val lastActivity: Long = System.currentTimeMillis(),
    @get:PropertyName("active")
    val isActive: Boolean = true,
    val settings: GroupSettings = GroupSettings(),
    val pinnedMessageIds: List<String> = emptyList(),
    val mutedMembers: List<String> = emptyList()
) {
    /**
     * No-argument constructor required by Firebase for deserialization.
     * Initializes all properties with default values.
     */
    constructor() : this(
        "", "", "", null, emptyList(), emptyList(),
        "", 0L, null, 0L, true, GroupSettings(), emptyList(), emptyList()
    )

    /**
     * Checks if a user is an administrator of the group.
     *
     * @param userId The user ID to check for admin privileges
     * @return true if the user is an admin, false otherwise
     */
    fun isAdmin(userId: String): Boolean = adminIds.contains(userId)

    /**
     * Checks if a user is a member of the group.
     *
     * @param userId The user ID to check for membership
     * @return true if the user is a member, false otherwise
     */
    fun isMember(userId: String): Boolean = memberIds.contains(userId)

    /**
     * Checks if a user is muted in the group.
     *
     * @param userId The user ID to check for muted status
     * @return true if the user is muted, false otherwise
     */
    fun isMuted(userId: String): Boolean = mutedMembers.contains(userId)

    /**
     * Gets the total number of members in the group.
     *
     * @return The count of group members
     */
    @Exclude
    fun getMemberCount(): Int = memberIds.size

    /**
     * Checks if the user can modify group settings and information.
     *
     * @param userId The user ID to check for modification privileges
     * @return true if the user can modify the group, false otherwise
     */
    @Exclude
    fun canModify(userId: String): Boolean = isAdmin(userId) || createdBy == userId

    /**
     * Checks if the user can send messages to the group.
     *
     * This method considers multiple factors:
     * - Group membership requirement
     * - User mute status
     * - Group settings for admin-only messaging
     *
     * @param userId The user ID to check for messaging privileges
     * @return true if the user can send messages, false otherwise
     */
    fun canSendMessages(userId: String): Boolean {
        return when {
            !isMember(userId) -> false
            isMuted(userId) -> false
            settings.onlyAdminsCanWrite && !isAdmin(userId) -> false
            else -> true
        }
    }

    /**
     * Checks if the user can add new members to the group.
     *
     * This method considers group membership and admin-only settings.
     *
     * @param userId The user ID to check for member addition privileges
     * @return true if the user can add members, false otherwise
     */
    fun canAddMembers(userId: String): Boolean {
        return when {
            !isMember(userId) -> false
            settings.onlyAdminsCanAddMembers && !isAdmin(userId) -> false
            else -> true
        }
    }

    /**
     * Gets the group's display information for UI components.
     *
     * This method creates a simplified representation of the group
     * suitable for list displays and summary views.
     *
     * @return [GroupDisplayInfo] containing essential display data
     */
    @Exclude
    fun getDisplayInfo(): GroupDisplayInfo {
        return GroupDisplayInfo(
            name = name,
            memberCount = getMemberCount(),
            imageUrl = imageUrl,
            lastMessage = lastMessage,
            lastActivity = lastActivity
        )
    }
}

/**
 * Advanced group settings.
 */
data class GroupSettings(
    val onlyAdminsCanWrite: Boolean = false,
    val onlyAdminsCanAddMembers: Boolean = false,
    val onlyAdminsCanEditInfo: Boolean = true,
    val disappearingMessages: Boolean = false,
    val disappearingMessageTimer: Long = 0L, // in milliseconds
    val allowMemberInvites: Boolean = true,
    val showMemberAddedNotifications: Boolean = true,
    val showMemberLeftNotifications: Boolean = true,
    val enableReadReceipts: Boolean = true,
    val enableTypingIndicators: Boolean = true
) {
    constructor() : this(false, false, true, false, 0L, true, true, true, true, true)
}

/**
 * Simplified display information for lists.
 */
data class GroupDisplayInfo(
    val name: String,
    val memberCount: Int,
    val imageUrl: String?,
    val lastMessage: ChatMessage?,
    val lastActivity: Long
)

/**
 * Types of group activities (expanded).
 */
enum class GroupActivityType {
    // Member activities
    USER_JOINED,
    USER_LEFT,
    USER_ADDED,
    USER_REMOVED,
    USER_INVITED,

    // Administration activities
    ADMIN_ADDED,
    ADMIN_REMOVED,
    MEMBER_MUTED,
    MEMBER_UNMUTED,

    // Group activities
    GROUP_CREATED,
    GROUP_NAME_CHANGED,
    GROUP_DESCRIPTION_CHANGED,
    GROUP_IMAGE_CHANGED,
    GROUP_SETTINGS_CHANGED,

    // Message activities
    MESSAGE_PINNED,
    MESSAGE_UNPINNED,
    MESSAGES_CLEARED,

    // Security activities
    ENCRYPTION_ENABLED,
    ENCRYPTION_DISABLED,
    DISAPPEARING_MESSAGES_ENABLED,
    DISAPPEARING_MESSAGES_DISABLED
}

/**
 * Represents a group activity with extended information.
 */
data class GroupActivity(
    val id: String = "",
    val groupId: String = "",
    val type: GroupActivityType = GroupActivityType.GROUP_CREATED,
    val performedBy: String = "",
    val targetUser: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    constructor() : this(
        "",
        "",
        GroupActivityType.GROUP_CREATED,
        "",
        null,
        null,
        null,
        0L,
        emptyMap()
    )

    /**
     * Generates the activity message to display in the chat.
     */
    fun getActivityMessage(performedByName: String, targetUserName: String? = null): String {
        return when (type) {
            GroupActivityType.USER_JOINED -> "$performedByName joined the group"
            GroupActivityType.USER_LEFT -> "$performedByName left the group"
            GroupActivityType.USER_ADDED -> "$performedByName added ${targetUserName ?: "someone"}"
            GroupActivityType.USER_REMOVED -> "$performedByName removed ${targetUserName ?: "someone"}"
            GroupActivityType.USER_INVITED -> "$performedByName invited ${targetUserName ?: "someone"}"

            GroupActivityType.ADMIN_ADDED -> "$performedByName made ${targetUserName ?: "someone"} an administrator"
            GroupActivityType.ADMIN_REMOVED -> "$performedByName removed ${targetUserName ?: "someone"} as an administrator"
            GroupActivityType.MEMBER_MUTED -> "$performedByName muted ${targetUserName ?: "someone"}"
            GroupActivityType.MEMBER_UNMUTED -> "$performedByName unmuted ${targetUserName ?: "someone"}"

            GroupActivityType.GROUP_CREATED -> "$performedByName created the group"
            GroupActivityType.GROUP_NAME_CHANGED -> "$performedByName changed the group name to \"$newValue\""
            GroupActivityType.GROUP_DESCRIPTION_CHANGED -> "$performedByName changed the group description"
            GroupActivityType.GROUP_IMAGE_CHANGED -> "$performedByName changed the group image"
            GroupActivityType.GROUP_SETTINGS_CHANGED -> "$performedByName changed the group settings"

            GroupActivityType.MESSAGE_PINNED -> "$performedByName pinned a message"
            GroupActivityType.MESSAGE_UNPINNED -> "$performedByName unpinned a message"
            GroupActivityType.MESSAGES_CLEARED -> "$performedByName cleared all messages"

            GroupActivityType.ENCRYPTION_ENABLED -> "$performedByName enabled encryption"
            GroupActivityType.ENCRYPTION_DISABLED -> "$performedByName disabled encryption"
            GroupActivityType.DISAPPEARING_MESSAGES_ENABLED -> "$performedByName enabled disappearing messages"
            GroupActivityType.DISAPPEARING_MESSAGES_DISABLED -> "$performedByName disabled disappearing messages"
        }
    }

    /**
     * Gets the representative icon of the activity.
     */
    fun getActivityIcon(): String {
        return when (type) {
            GroupActivityType.USER_JOINED, GroupActivityType.USER_ADDED -> "👤➕"
            GroupActivityType.USER_LEFT, GroupActivityType.USER_REMOVED -> "👤➖"
            GroupActivityType.USER_INVITED -> "📧"
            GroupActivityType.ADMIN_ADDED -> "👑➕"
            GroupActivityType.ADMIN_REMOVED -> "👑➖"
            GroupActivityType.MEMBER_MUTED -> "🔇"
            GroupActivityType.MEMBER_UNMUTED -> "🔊"
            GroupActivityType.GROUP_CREATED -> "🎉"
            GroupActivityType.GROUP_NAME_CHANGED -> "✏️"
            GroupActivityType.GROUP_DESCRIPTION_CHANGED -> "📝"
            GroupActivityType.GROUP_IMAGE_CHANGED -> "🖼️"
            GroupActivityType.GROUP_SETTINGS_CHANGED -> "⚙️"
            GroupActivityType.MESSAGE_PINNED -> "📌"
            GroupActivityType.MESSAGE_UNPINNED -> "📌❌"
            GroupActivityType.MESSAGES_CLEARED -> "🗑️"
            GroupActivityType.ENCRYPTION_ENABLED -> "🔐"
            GroupActivityType.ENCRYPTION_DISABLED -> "🔓"
            GroupActivityType.DISAPPEARING_MESSAGES_ENABLED -> "⏰"
            GroupActivityType.DISAPPEARING_MESSAGES_DISABLED -> "⏰❌"
        }
    }
}

/**
 * Represents a group invitation.
 */
data class GroupInvitation(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val invitedBy: String = "",
    val invitedByName: String = "",
    val invitedUser: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: InvitationStatus = InvitationStatus.PENDING,
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
) {
    constructor() : this("", "", "", "", "", "", 0L, InvitationStatus.PENDING, 0L)

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun isActive(): Boolean = status == InvitationStatus.PENDING && !isExpired()
}

/**
 * Invitation statuses.
 */
enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED
}