package com.yerayyas.chatappkotlinproject.data.model

/**
 * Represents a consolidated user model for the UI layer.
 *
 * This data class is a representation of a user, aggregating information from different
 * nodes in the Firebase Realtime Database (e.g., `/Users/{uid}/public`
 * and `/Users/{uid}/private`).
 * It is designed to provide a complete user object to the UI components,
 * such as user lists and profile screens.
 *
 * @property id The unique identifier (UID) of the user, typically from Firebase Authentication.
 * @property username The user's display name. Sourced from the user's public profile data.
 * @property email The user's email address. Sourced from the user's private data.
 * @property profileImage The URL of the user's profile picture. Sourced from
 * the public profile data.
 * @property status The user's online status, e.g., "online" or "offline". Sourced from
 * the private data.
 * @property isOnline A boolean flag derived from the `status` field for easy
 * conditional checks in the UI.
 * @property lastSeen A timestamp (in milliseconds) indicating when the user was
 * last active. Sourced from the private data.
 */
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val profileImage: String = "",
    val status: String = "offline",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0
)
