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
 *
 * **Key Changes:**
 * - Replaced NotificationCanceller with ShowChatNotificationUseCase
 * - Added proper error handling with Result types
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

            val senderId = data["senderId"]
            val senderName = data["senderName"]
            val message = data["message"] ?: data["messagePreview"]
            val chatId = data["chatId"]

            if (senderId.isNullOrBlank() || senderName.isNullOrBlank()) {
                Log.e(TAG, "Invalid data: senderId or senderName is missing")
                return
            }

            Log.d(TAG, "Processing notification for: $senderName")

            serviceScope.launch {
                try {
                    processNotification(senderId, senderName, message, chatId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical error processing FCM message", e)
        }
    }

    /**
     * Process notification using the new Clean Architecture implementation
     */
    private suspend fun processNotification(
        senderId: String,
        senderName: String,
        message: String?,
        chatId: String?
    ) {
        try {
            val shouldShow = shouldShowChatNotification(senderId)
            Log.d(TAG, "Should show notification: $shouldShow")

            if (shouldShow) {
                val result = showChatNotificationUseCase(
                    senderId = senderId,
                    senderName = senderName,
                    messageBody = message ?: "New message",
                    chatId = chatId ?: "unknown"
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Notification sent successfully for: $senderName")
                } else {
                    Log.e(
                        TAG,
                        "Failed to send notification for: $senderName",
                        result.exceptionOrNull()
                    )
                }
            } else {
                Log.d(TAG, "Notification suppressed - chat is currently open")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in processNotification", e)
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
