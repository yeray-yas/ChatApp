package com.yerayyas.chatappkotlinproject.data.model

/**
 * Represents a consolidated user model for the application.
 *
 * This data class provides a complete user representation by aggregating information from
 * different nodes in the Firebase Realtime Database structure (e.g., `/Users/{uid}/public`
 * and `/Users/{uid}/private`). It serves as the primary user model throughout the application,
 * providing essential user information to UI components and business logic.
 *
 * Key features:
 * - Aggregated user data from multiple Firebase nodes
 * - Public and private data separation support
 * - Online status tracking and management
 * - Profile information with image support
 * - Timestamp-based activity tracking
 * - Comprehensive user identification
 *
 * Data sources:
 * - Public profile data: username, profileImage
 * - Private data: email, status, lastSeen
 * - Authentication data: id (UID from Firebase Auth)
 *
 * Usage contexts:
 * - User lists and contact displays
 * - Profile screens and user information
 * - Chat participant identification
 * - Online status indicators
 * - Search and filtering operations
 *
 * @property id The unique identifier (UID) from Firebase Authentication
 * @property username The user's display name, sourced from public profile data
 * @property email The user's email address, sourced from private profile data
 * @property profileImage The URL of the user's profile picture, sourced from public data
 * @property status The user's current status text (e.g., "online", "offline"), sourced from private data
 * @property isOnline A boolean flag derived from the [status] field for convenient UI checks
 * @property lastSeen A timestamp (in milliseconds) indicating when the user was last active
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
