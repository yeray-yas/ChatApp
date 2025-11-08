package com.yerayyas.chatappkotlinproject.data.model

/**
 * Representa un chat grupal completo con todas las funcionalidades avanzadas
 */
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
    val isActive: Boolean = true,
    val settings: GroupSettings = GroupSettings(),
    val pinnedMessageIds: List<String> = emptyList(),
    val mutedMembers: List<String> = emptyList()
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this(
        "", "", "", null, emptyList(), emptyList(),
        "", 0L, null, 0L, true, GroupSettings(), emptyList(), emptyList()
    )

    /**
     * Verifica si un usuario es administrador del grupo
     */
    fun isAdmin(userId: String): Boolean = adminIds.contains(userId)

    /**
     * Verifica si un usuario es miembro del grupo
     */
    fun isMember(userId: String): Boolean = memberIds.contains(userId)

    /**
     * Verifica si un usuario está silenciado
     */
    fun isMuted(userId: String): Boolean = mutedMembers.contains(userId)

    /**
     * Obtiene el número total de miembros
     */
    fun getMemberCount(): Int = memberIds.size

    /**
     * Verifica si el usuario puede modificar el grupo
     */
    fun canModify(userId: String): Boolean = isAdmin(userId) || createdBy == userId

    /**
     * Verifica si el usuario puede enviar mensajes
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
     * Verifica si el usuario puede agregar miembros
     */
    fun canAddMembers(userId: String): Boolean {
        return when {
            !isMember(userId) -> false
            settings.onlyAdminsCanAddMembers && !isAdmin(userId) -> false
            else -> true
        }
    }

    /**
     * Obtiene la información de display del grupo
     */
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
 * Configuraciones avanzadas del grupo
 */
data class GroupSettings(
    val onlyAdminsCanWrite: Boolean = false,
    val onlyAdminsCanAddMembers: Boolean = false,
    val onlyAdminsCanEditInfo: Boolean = true,
    val disappearingMessages: Boolean = false,
    val disappearingMessageTimer: Long = 0L, // en milisegundos
    val allowMemberInvites: Boolean = true,
    val showMemberAddedNotifications: Boolean = true,
    val showMemberLeftNotifications: Boolean = true,
    val enableReadReceipts: Boolean = true,
    val enableTypingIndicators: Boolean = true
) {
    constructor() : this(false, false, true, false, 0L, true, true, true, true, true)
}

/**
 * Información de display simplificada para listas
 */
data class GroupDisplayInfo(
    val name: String,
    val memberCount: Int,
    val imageUrl: String?,
    val lastMessage: ChatMessage?,
    val lastActivity: Long
)

/**
 * Tipos de actividad en el grupo (expandidos)
 */
enum class GroupActivityType {
    // Actividades de miembros
    USER_JOINED,
    USER_LEFT,
    USER_ADDED,
    USER_REMOVED,
    USER_INVITED,

    // Actividades de administración
    ADMIN_ADDED,
    ADMIN_REMOVED,
    MEMBER_MUTED,
    MEMBER_UNMUTED,

    // Actividades del grupo
    GROUP_CREATED,
    GROUP_NAME_CHANGED,
    GROUP_DESCRIPTION_CHANGED,
    GROUP_IMAGE_CHANGED,
    GROUP_SETTINGS_CHANGED,

    // Actividades de mensajes
    MESSAGE_PINNED,
    MESSAGE_UNPINNED,
    MESSAGES_CLEARED,

    // Actividades de seguridad
    ENCRYPTION_ENABLED,
    ENCRYPTION_DISABLED,
    DISAPPEARING_MESSAGES_ENABLED,
    DISAPPEARING_MESSAGES_DISABLED
}

/**
 * Representa una actividad en el grupo con información extendida
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
     * Genera el mensaje de actividad para mostrar en el chat
     */
    fun getActivityMessage(performedByName: String, targetUserName: String? = null): String {
        return when (type) {
            GroupActivityType.USER_JOINED -> "$performedByName se unió al grupo"
            GroupActivityType.USER_LEFT -> "$performedByName dejó el grupo"
            GroupActivityType.USER_ADDED -> "$performedByName agregó a ${targetUserName ?: "alguien"}"
            GroupActivityType.USER_REMOVED -> "$performedByName eliminó a ${targetUserName ?: "alguien"}"
            GroupActivityType.USER_INVITED -> "$performedByName invitó a ${targetUserName ?: "alguien"}"

            GroupActivityType.ADMIN_ADDED -> "$performedByName nombró administrador a ${targetUserName ?: "alguien"}"
            GroupActivityType.ADMIN_REMOVED -> "$performedByName quitó como administrador a ${targetUserName ?: "alguien"}"
            GroupActivityType.MEMBER_MUTED -> "$performedByName silenció a ${targetUserName ?: "alguien"}"
            GroupActivityType.MEMBER_UNMUTED -> "$performedByName quitó el silencio a ${targetUserName ?: "alguien"}"

            GroupActivityType.GROUP_CREATED -> "$performedByName creó el grupo"
            GroupActivityType.GROUP_NAME_CHANGED -> "$performedByName cambió el nombre del grupo a \"$newValue\""
            GroupActivityType.GROUP_DESCRIPTION_CHANGED -> "$performedByName cambió la descripción del grupo"
            GroupActivityType.GROUP_IMAGE_CHANGED -> "$performedByName cambió la imagen del grupo"
            GroupActivityType.GROUP_SETTINGS_CHANGED -> "$performedByName cambió la configuración del grupo"

            GroupActivityType.MESSAGE_PINNED -> "$performedByName fijó un mensaje"
            GroupActivityType.MESSAGE_UNPINNED -> "$performedByName desfijó un mensaje"
            GroupActivityType.MESSAGES_CLEARED -> "$performedByName eliminó todos los mensajes"

            GroupActivityType.ENCRYPTION_ENABLED -> "$performedByName activó la encriptación"
            GroupActivityType.ENCRYPTION_DISABLED -> "$performedByName desactivó la encriptación"
            GroupActivityType.DISAPPEARING_MESSAGES_ENABLED -> "$performedByName activó los mensajes temporales"
            GroupActivityType.DISAPPEARING_MESSAGES_DISABLED -> "$performedByName desactivó los mensajes temporales"
        }
    }

    /**
     * Obtiene el ícono representativo de la actividad
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
 * Representa una invitación a grupo
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
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 días
) {
    constructor() : this("", "", "", "", "", "", 0L, InvitationStatus.PENDING, 0L)

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun isActive(): Boolean = status == InvitationStatus.PENDING && !isExpired()
}

/**
 * Estados de invitación
 */
enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED
}