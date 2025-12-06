package com.yerayyas.chatappkotlinproject.domain.usecases.notification

import android.util.Log
import com.yerayyas.chatappkotlinproject.domain.model.NotificationData
import com.yerayyas.chatappkotlinproject.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShowChatNotification"

/**
 * Use case for displaying chat notifications.
 *
 * This use case encapsulates the business logic for showing notifications,
 * including permission validation and error handling.
 *
 * Supports both individual and group chat notifications.
 */
@Singleton
class ShowChatNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Executes the use case to show a chat notification.
     *
     * @param senderId Unique identifier for the message sender
     * @param senderName Display name of the sender
     * @param messageBody Content of the received message
     * @param chatId Unique identifier for the chat conversation (user ID for individual, group ID for group)
     * @param isGroupMessage Whether this is a group message (default: false)
     * @param groupName Name of the group (required if isGroupMessage is true)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        senderId: String,
        senderName: String,
        messageBody: String,
        chatId: String,
        isGroupMessage: Boolean = false,
        groupName: String? = null
    ): Result<Unit> {
        return try {
            val chatType = if (isGroupMessage) "group" else "individual"
            val displayName = if (isGroupMessage) groupName else senderName
            Log.d(
                TAG,
                "Attempting to show $chatType notification for: $displayName (Sender: $senderName)"
            )

            // Check permissions first
            val permissionState = notificationRepository.getNotificationPermissionState()
            if (!permissionState.isAllowed) {
                val error = "Notification permission not granted"
                Log.w(TAG, error)
                return Result.failure(SecurityException(error))
            }

            // Create notification data
            val notificationData = NotificationData(
                senderId = senderId,
                senderName = senderName,
                messageBody = messageBody,
                chatId = chatId,
                isGroupMessage = isGroupMessage,
                groupName = groupName
            )

            // Show the notification
            val result = notificationRepository.showNotification(notificationData)

            if (result.isSuccess) {
                Log.d(TAG, "$chatType notification shown successfully for: $displayName")
            } else {
                Log.e(
                    TAG,
                    "Failed to show $chatType notification for: $displayName",
                    result.exceptionOrNull()
                )
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error showing notification", e)
            Result.failure(e)
        }
    }
}
