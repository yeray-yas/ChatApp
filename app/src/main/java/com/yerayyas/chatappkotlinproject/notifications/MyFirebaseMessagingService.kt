package com.yerayyas.chatappkotlinproject.notifications

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
 * **UPDATED** - Firebase Cloud Messaging service using Clean Architecture
 *
 * This service now uses the new Clean Architecture notification system
 * while maintaining the same functionality. It handles:
 * 1. **Token Management**: Updates FCM tokens using UpdateFcmTokenUseCase
 * 2. **Message Handling**: Shows notifications using ShowChatNotificationUseCase
 * 3. **Chat Types**: Supports both individual and group chat notifications
 *
 * **Key Changes:**
 * - Replaced NotificationCanceller with ShowChatNotificationUseCase
 * - Added proper error handling with Result types
 * - Added support for group chat notifications
 * - Maintains backward compatibility
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when the service is created.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Firebase Messaging Service created with Clean Architecture implementation")
    }

    /**
     * Called when a new data message is received from FCM.
     * Now handles both individual and group chat messages.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")
        Log.d(TAG, "Message data: ${remoteMessage.data}")

        try {
            val data = remoteMessage.data
            if (data.isEmpty()) {
                Log.w(TAG, "Empty data payload received")
                return
            }

            val messageType = data["messageType"]
            val senderId = data["senderId"]
            val senderName = data["senderName"]
            val message = data["message"] ?: data["messagePreview"]

            if (senderId.isNullOrBlank() || senderName.isNullOrBlank()) {
                Log.e(TAG, "Invalid data: senderId or senderName is missing")
                return
            }

            Log.d(TAG, "Processing notification for: $senderName, messageType: $messageType")

            serviceScope.launch {
                try {
                    when (messageType) {
                        "group_message" -> {
                            val groupId = data["groupId"]
                            val groupName = data["groupName"]
                            processGroupNotification(
                                senderId,
                                senderName,
                                message,
                                groupId,
                                groupName
                            )
                        }

                        else -> {
                            // Individual chat (legacy and new messages)
                            val chatId = data["chatId"]
                            processIndividualNotification(senderId, senderName, message, chatId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical error processing FCM message", e)
        }
    }

    /**
     * Process notification for individual chat using the new Clean Architecture implementation
     */
    private suspend fun processIndividualNotification(
        senderId: String,
        senderName: String,
        message: String?,
        chatId: String?
    ) {
        try {
            val shouldShow =
                shouldShowChatNotification.shouldShowIndividualChatNotification(senderId)
            Log.d(TAG, "Should show individual notification: $shouldShow")

            if (shouldShow) {
                val result = showChatNotificationUseCase(
                    senderId = senderId,
                    senderName = senderName,
                    messageBody = message ?: "New message",
                    chatId = chatId ?: "unknown"
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Individual notification sent successfully for: $senderName")
                } else {
                    Log.e(
                        TAG,
                        "Failed to send individual notification for: $senderName",
                        result.exceptionOrNull()
                    )
                }
            } else {
                Log.d(TAG, "Individual notification suppressed - chat is currently open")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in processIndividualNotification", e)
        }
    }

    /**
     * Process notification for group chat
     */
    private suspend fun processGroupNotification(
        senderId: String,
        senderName: String,
        message: String?,
        groupId: String?,
        groupName: String?
    ) {
        try {
            if (groupId.isNullOrBlank()) {
                Log.e(TAG, "Group notification missing groupId")
                return
            }

            val shouldShow = shouldShowChatNotification.shouldShowGroupChatNotification(groupId)
            Log.d(TAG, "Should show group notification: $shouldShow for group: $groupName")

            if (shouldShow) {
                val result = showChatNotificationUseCase(
                    senderId = senderId,
                    senderName = senderName,
                    messageBody = message ?: "New message",
                    chatId = groupId,
                    isGroupMessage = true,
                    groupName = groupName
                )

                if (result.isSuccess) {
                    Log.d(
                        TAG,
                        "Group notification sent successfully for: $senderName in $groupName"
                    )
                } else {
                    Log.e(
                        TAG,
                        "Failed to send group notification for: $senderName in $groupName",
                        result.exceptionOrNull()
                    )
                }
            } else {
                Log.d(TAG, "Group notification suppressed - group chat is currently open")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in processGroupNotification", e)
        }
    }

    /**
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token received")

        try {
            serviceScope.launch {
                updateFcmToken(token)
            }
            Log.d(TAG, "FCM token updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new FCM token", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Firebase Messaging Service destroyed")
        super.onDestroy()
    }
}
