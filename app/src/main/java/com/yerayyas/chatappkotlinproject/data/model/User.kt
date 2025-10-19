package com.yerayyas.chatappkotlinproject.data.model

/**
 * Represents a user in the chat application.
 *
 * This data class models user information, including identifiers, contact details,
 * profile image, status, and online activity. It is used to display user-related
 * information throughout the app, such as in chat lists and profile views.
 *
 * Note:
 * - The `id` field maps to "userId" in Firebase.
 * - The `profileImage` field maps to "image" in Firebase.
 * - The `isOnline` field is derived from the `status`.
 * - The `lastSeen` field is not stored in Firebase.
 *
 * @property id Unique identifier of the user.
 * @property username Display name of the user.
 * @property email Email address of the user.
 * @property profileImage URL or path to the user's profile picture.
 * @property status String representation of the user status ("online", "offline").
 * @property isOnline Boolean flag indicating if the user is currently online.
 * @property lastSeen Timestamp representing the user's last online activity.
 */
data class User(
    val id: String,
    val username: String,
    val email: String,
    val profileImage: String,
    val status: String,
    val isOnline: Boolean,
    val lastSeen: Long = 0
)

