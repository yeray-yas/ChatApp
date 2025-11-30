package com.yerayyas.chatappkotlinproject.notifications

import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.domain.usecases.ShouldShowChatNotificationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.ShowChatNotificationUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyFirebaseMsgService"

/**
 * Firebase Cloud Messaging service implementing Clean Architecture principles.
 *
 * This service handles incoming FCM messages and token management using domain layer
 * use cases instead of direct infrastructure calls. It supports both individual and
 * group chat notifications while maintaining proper separation of concerns.
 *
 * Key responsibilities:
 * - Processing incoming FCM messages for chat notifications
 * - Managing FCM token updates through domain layer use cases
 * - Supporting both individual and group chat notification flows
 * - Implementing intelligent notification filtering based on app state
 * - Providing comprehensive error handling and logging
 * - Using Clean Architecture patterns for maintainable code
 *
 * Architecture Pattern: Infrastructure Service with Use Case Integration
 * - Responds to Firebase Cloud Messaging events
 * - Delegates business logic to domain layer use cases
 * - Uses dependency injection for loose coupling with domain layer
 * - Handles asynchronous operations with proper coroutine scope
 * - Maintains separation between infrastructure and business logic
 *
 * Notification Types Supported:
 * - Individual chat messages: Direct user-to-user communication
 * - Group chat messages: Multi-user group communication
 * - Legacy message format: Backward compatibility with older notifications
 *
 * Key improvements in Clean Architecture implementation:
 * - Uses ShowChatNotificationUseCase instead of direct NotificationManager calls
 * - Uses ShouldShowChatNotificationUseCase for intelligent notification filtering
 * - Uses UpdateFcmTokenUseCase for token management
 * - Proper error handling with Result types from use cases
 * - Comprehensive logging for debugging and monitoring
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var showChatNotificationUseCase: ShowChatNotificationUseCase

    @Inject
    lateinit var shouldShowChatNotification: ShouldShowChatNotificationUseCase

    @Inject
    lateinit var updateFcmToken: UpdateFcmTokenUseCase

    @Inject
    lateinit var appState: AppState

    /**
     * Coroutine scope for handling asynchronous use case operations.
     * Uses SupervisorJob to prevent child coroutine failures from affecting the service.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when the service is created and dependency injection is complete.
     */
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Firebase Messaging Service initialized with Clean Architecture implementation")
    }

    /**
     * Called when a new FCM message is received.
     *
     * This method processes incoming messages for both individual and group chats,
     * using domain layer use cases to handle business logic. It includes comprehensive
     * error handling and logging for debugging purposes.
     *
     * Message processing flow:
     * 1. Validate message data payload
     * 2. Extract message type and sender information
     * 3. Route to appropriate processing method based on message type
     * 4. Use domain layer use cases for notification decisions and display
     *
     * @param remoteMessage The FCM message containing notification data
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        try {
            val messageData = remoteMessage.data
            if (messageData.isEmpty()) {
                Log.w(TAG, "Received FCM message with empty data payload - ignoring")
                return
            }

            Log.d(TAG, "Processing FCM message data: $messageData")

            // Extract common message fields
            val messageType = messageData["messageType"]
            val senderId = messageData["senderId"]
            val senderName = messageData["senderName"]
            val messageContent = messageData["message"] ?: messageData["messagePreview"]

            // Validate essential fields
            if (senderId.isNullOrBlank() || senderName.isNullOrBlank()) {
                Log.e(TAG, "Invalid FCM message: missing senderId or senderName")
                return
            }

            Log.i(TAG, "Processing $messageType notification from: $senderName")

            // Process message asynchronously using coroutines
            serviceScope.launch {
                try {
                    when (messageType) {
                        "group_message" -> {
                            processGroupChatMessage(
                                messageData,
                                senderId,
                                senderName,
                                messageContent
                            )
                        }

                        else -> {
                            // Handle individual chat (includes legacy messages without explicit type)
                            processIndividualChatMessage(
                                messageData,
                                senderId,
                                senderName,
                                messageContent
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in asynchronous message processing", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical error processing FCM message", e)
        }
    }

    /**
     * Called when a new FCM token is generated.
     *
     * This method uses the domain layer use case to update the token in the backend,
     * ensuring proper error handling and logging through the Clean Architecture approach.
     *
     * @param token The new FCM registration token
     */
    override fun onNewToken(token: String) {
        Log.i(TAG, "New FCM token received - updating through domain layer")

        try {
            serviceScope.launch {
                try {
                    updateFcmToken(token)
                    Log.i(TAG, "FCM token updated successfully through use case")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update FCM token using domain layer use case", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new FCM token", e)
        }
    }

    /**
     * Called when the service is being destroyed.
     */
    override fun onDestroy() {
        Log.i(TAG, "Firebase Messaging Service destroyed")
        super.onDestroy()
    }

    /**
     * Processes FCM messages for individual chat conversations.
     *
     * This method uses domain layer use cases to determine whether notifications
     * should be shown and to display them appropriately. It handles both modern
     * and legacy message formats for backward compatibility.
     *
     * @param messageData The complete FCM message data
     * @param senderId Unique identifier of the message sender
     * @param senderName Display name of the message sender
     * @param messageContent The message text content
     */
    private suspend fun processIndividualChatMessage(
        messageData: Map<String, String>,
        senderId: String,
        senderName: String,
        messageContent: String?
    ) {
        try {
            Log.d(TAG, "Processing individual chat message from: $senderName")

            // 1. System/Context Check (Screen, Background, Home vs Chat)
            val systemAllowsNotification = shouldDisplayNotification(targetId = senderId, isGroup = false)

            if (systemAllowsNotification) {
                // 2. Business Logic Check (Muted chats, blocked users, etc. handled by UseCase)
                // Note: The UseCase might basically duplicate the AppState check, but that's okay.
                // The important part is that 'systemAllowsNotification' has filtered the Home screen case.
                val shouldShow = shouldShowChatNotification.shouldShowIndividualChatNotification(senderId)

                Log.d(TAG, "Should show individual notification for $senderName: $shouldShow")

                if (shouldShow) {
                    val chatId = messageData["chatId"] ?: senderId

                    val result = showChatNotificationUseCase(
                        senderId = senderId,
                        senderName = senderName,
                        messageBody = messageContent ?: "New message",
                        chatId = chatId
                    )

                    handleNotificationResult(result, "individual", senderName)
                }
            } else {
                Log.d(TAG, "Individual notification suppressed by System Rules (Screen ON + Home/SameChat)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing individual chat message from: $senderName", e)
        }
    }

    /**
     * Processes FCM messages for group chat conversations.
     *
     * This method handles group-specific message data and uses domain layer use cases
     * to manage group notification logic, including sender information display.
     *
     * @param messageData The complete FCM message data
     * @param senderId Unique identifier of the message sender
     * @param senderName Display name of the message sender
     * @param messageContent The message text content
     */
    private suspend fun processGroupChatMessage(
        messageData: Map<String, String>,
        senderId: String,
        senderName: String,
        messageContent: String?
    ) {
        try {
            val groupId = messageData["groupId"]
            val groupName = messageData["groupName"]

            if (groupId.isNullOrBlank()) {
                Log.e(TAG, "Group message missing required groupId - cannot process")
                return
            }

            Log.d(TAG, "Processing group chat message from: $senderName in group: $groupName")

            // 1. System/Context Check
            val systemAllowsNotification = shouldDisplayNotification(targetId = groupId, isGroup = true)

            if (systemAllowsNotification) {
                // 2. Business Logic Check
                val shouldShow = shouldShowChatNotification.shouldShowGroupChatNotification(groupId)

                Log.d(TAG, "Should show group notification for $groupName: $shouldShow")

                if (shouldShow) {
                    val result = showChatNotificationUseCase(
                        senderId = senderId,
                        senderName = senderName,
                        messageBody = messageContent ?: "New message",
                        chatId = groupId,
                        isGroupMessage = true,
                        groupName = groupName
                    )

                    handleNotificationResult(result, "group", "$senderName in $groupName")
                }
            } else {
                Log.d(TAG, "Group notification suppressed by System Rules (Screen ON + Home/SameChat)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing group chat message from: $senderName", e)
        }
    }

    /**
     * Determines whether a notification should be displayed based on System State (Screen On/Off)
     * and Application UI State (Background, Home, Specific Chat).
     *
     * Rules:
     * 1. Screen OFF -> Show
     * 2. App Background -> Show
     * 3. App Foreground + Screen ON:
     *    - If target chat is OPEN -> Suppress (already reading)
     *    - If ANY OTHER chat is open -> Show (Heads-up)
     *    - If NO chat is open (Home, Settings, etc.) -> Suppress
     *
     * @param targetId The ID of the sender (individual) or group (group chat).
     * @param isGroup True if checking a group chat, false otherwise.
     */
    private fun shouldDisplayNotification(targetId: String, isGroup: Boolean): Boolean {
        // 1. Check Screen State
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        // If Screen is OFF, we ALWAYS want to notify (so phone vibrates/sounds)
        if (!isScreenOn) return true

        // 2. Check Background State
        // If App is in Background, we ALWAYS want to notify
        if (!appState.isAppInForeground) return true

        // --- At this point: App is Foreground AND Screen is ON ---

        // Get the ID of the chat currently visible to the user (if any)
        val currentOpenChatId = if (isGroup) {
            appState.currentOpenGroupChatId
        } else {
            appState.currentOpenChatUserId
        }

        // 3. Check if user is inside the SAME chat
        if (currentOpenChatId == targetId) {
            return false // Suppress: User is looking at this chat
        }

        // 4. Check if user is inside ANY OTHER chat
        if (!currentOpenChatId.isNullOrEmpty()) {
            return true // Show: User is in chat A, message from chat B (Heads-up needed)
        }

        // 5. User is in Home, Group List, Settings, etc. (No specific chat open)
        return false
    }

    /**
     * Handles the result of notification use case operations.
     *
     * This method provides centralized logging and error handling for notification
     * results, making it easier to debug notification issues.
     *
     * @param result The Result object from the notification use case
     * @param notificationType Description of the notification type for logging
     * @param context Additional context information for logging
     */
    private fun handleNotificationResult(
        result: Result<Unit>,
        notificationType: String,
        context: String
    ) {
        if (result.isSuccess) {
            Log.i(TAG, "Successfully sent $notificationType notification for: $context")
        } else {
            Log.e(
                TAG,
                "Failed to send $notificationType notification for: $context",
                result.exceptionOrNull()
            )
        }
    }
}
