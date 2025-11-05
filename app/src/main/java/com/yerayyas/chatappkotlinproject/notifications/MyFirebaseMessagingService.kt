package com.yerayyas.chatappkotlinproject.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.domain.usecases.ShouldShowChatNotificationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyFirebaseMsgService"

/**
 * A service that extends [FirebaseMessagingService] to handle Firebase Cloud Messaging (FCM) events.
 *
 * This service is responsible for two main tasks:
 * 1.  **Token Management**: It captures newly generated FCM tokens and updates them on the backend
 *     server using the [UpdateFcmTokenUseCase].
 * 2.  **Message Handling**: It intercepts incoming data messages from FCM, determines if a push
 *     notification should be displayed using [ShouldShowChatNotificationUseCase], and then uses
 *     [NotificationHelper] to build and show the notification.
 *
 * This class is annotated with `@AndroidEntryPoint` to enable Hilt dependency injection.
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

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
        Log.d(TAG, "Firebase Messaging Service created")
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
     * Process notification with proper error handling
     */
    private fun processNotification(
        senderId: String,
        senderName: String,
        message: String?,
        chatId: String?
    ) {
        try {
            val shouldShow = shouldShowChatNotification(senderId)
            Log.d(TAG, "Should show notification: $shouldShow")

            if (shouldShow) {
                notificationHelper.sendChatNotification(
                    senderId = senderId,
                    senderName = senderName,
                    messageBody = message ?: "New message",
                    chatId = chatId ?: "unknown"
                )
                Log.d(TAG, "Notification sent for: $senderName")
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
