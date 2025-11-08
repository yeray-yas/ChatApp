package com.yerayyas.chatappkotlinproject.data.model

/**
 * Representa un chat grupal
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
    val isActive: Boolean = true
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this(
        "", "", "", null, emptyList(), emptyList(),
        "", 0L, null, 0L, true
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
     * Obtiene el número total de miembros
     */
    fun getMemberCount(): Int = memberIds.size

    /**
     * Verifica si el usuario puede modificar el grupo
     */
    fun canModify(userId: String): Boolean = isAdmin(userId) || createdBy == userId
}

/**
 * Tipos de actividad en el grupo
 */
enum class GroupActivityType {
    USER_JOINED,
    USER_LEFT,
    USER_ADDED,
    USER_REMOVED,
    ADMIN_ADDED,
    ADMIN_REMOVED,
    GROUP_CREATED,
    GROUP_NAME_CHANGED,
    GROUP_DESCRIPTION_CHANGED,
    GROUP_IMAGE_CHANGED
}

/**
 * Representa una actividad en el grupo
 */
data class GroupActivity(
    val id: String = "",
    val groupId: String = "",
    val type: GroupActivityType = GroupActivityType.GROUP_CREATED,
    val performedBy: String = "",
    val targetUser: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", GroupActivityType.GROUP_CREATED, "", null, null, null, 0L)

    /**
     * Genera el mensaje de actividad para mostrar en el chat
     */
    fun getActivityMessage(performedByName: String, targetUserName: String? = null): String {
        return when (type) {
            GroupActivityType.USER_JOINED -> "$performedByName se unió al grupo"
            GroupActivityType.USER_LEFT -> "$performedByName dejó el grupo"
            GroupActivityType.USER_ADDED -> "$performedByName agregó a ${targetUserName ?: "alguien"}"
            GroupActivityType.USER_REMOVED -> "$performedByName eliminó a ${targetUserName ?: "alguien"}"
            GroupActivityType.ADMIN_ADDED -> "$performedByName nombró administrador a ${targetUserName ?: "alguien"}"
            GroupActivityType.ADMIN_REMOVED -> "$performedByName quitó como administrador a ${targetUserName ?: "alguien"}"
            GroupActivityType.GROUP_CREATED -> "$performedByName creó el grupo"
            GroupActivityType.GROUP_NAME_CHANGED -> "$performedByName cambió el nombre del grupo a \"$newValue\""
            GroupActivityType.GROUP_DESCRIPTION_CHANGED -> "$performedByName cambió la descripción del grupo"
            GroupActivityType.GROUP_IMAGE_CHANGED -> "$performedByName cambió la imagen del grupo"
        }
    }
}