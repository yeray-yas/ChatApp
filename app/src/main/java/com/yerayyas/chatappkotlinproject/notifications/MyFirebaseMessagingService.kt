package com.yerayyas.chatappkotlinproject.notifications

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.domain.usecases.ShouldShowChatNotificationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import com.yerayyas.chatappkotlinproject.utils.XiaomiPermissionHelper
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
     * Called when the service is created. Verify that all dependencies are properly injected.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== SERVICE CREATED ===")

        // Log device information - important for troubleshooting Xiaomi issues
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")

        if (XiaomiPermissionHelper.isXiaomiDevice()) {
            Log.w(TAG, "XIAOMI DEVICE DETECTED in Firebase Messaging Service")
            Log.w(TAG, "Service may be killed by MIUI battery optimization")

            // Check Xiaomi-specific settings
            val status = XiaomiPermissionHelper.checkXiaomiNotificationSettings(this)
            Log.d(TAG, "Xiaomi notification status in service: $status")

            if (!status.isFullyConfigured()) {
                Log.w(TAG, "Service may not work properly due to missing Xiaomi configurations")
                Log.w(TAG, "Missing: ${status.getMissingConfigurations()}")
            }
        }

        // Verify dependencies are injected
        try {
            Log.d(TAG, "notificationHelper initialized: ${this::notificationHelper.isInitialized}")
            Log.d(
                TAG,
                "shouldShowChatNotification initialized: ${this::shouldShowChatNotification.isInitialized}"
            )
            Log.d(TAG, "updateFcmToken initialized: ${this::updateFcmToken.isInitialized}")
            Log.d(TAG, "appState initialized: ${this::appState.isInitialized}")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking dependency initialization", e)
        }
    }

    /**
     * Called when a new data message is received from FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.w(TAG, "==========================================")
        Log.w(TAG, "=== FCM MESSAGE RECEIVED ON XIAOMI DEVICE ===")
        Log.w(TAG, "==========================================")
        Log.w(TAG, "From: ${remoteMessage.from}")
        Log.w(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.w(TAG, "Data payload: ${remoteMessage.data}")
        Log.w(TAG, "Priority: ${remoteMessage.priority}")
        Log.w(TAG, "Original priority: ${remoteMessage.originalPriority}")
        Log.w(TAG, "Sent time: ${remoteMessage.sentTime}")
        Log.w(TAG, "TTL: ${remoteMessage.ttl}")

        if (XiaomiPermissionHelper.isXiaomiDevice()) {
            Log.w(TAG, "🔥 PROCESSING FCM MESSAGE ON XIAOMI DEVICE! 🔥")
            Log.w(TAG, "Message priority should be 'high' for reliable delivery")
            Log.w(TAG, "Current time: ${System.currentTimeMillis()}")
            Log.w(TAG, "Message age: ${System.currentTimeMillis() - remoteMessage.sentTime}ms")
        }

        try {
            val data = remoteMessage.data
            if (data.isEmpty()) {
                Log.w(TAG, "❌ Empty data payload received - this is unusual!")
                return
            }

            val senderId = data["senderId"]
            val senderName = data["senderName"]
            val message = data["message"] ?: data["messagePreview"]
            val messageType = data["messageType"]
            val chatId = data["chatId"]

            Log.w(TAG, "📧 Parsed FCM data:")
            Log.w(TAG, "  SenderId: $senderId")
            Log.w(TAG, "  SenderName: $senderName")
            Log.w(TAG, "  Message: $message")
            Log.w(TAG, "  MessageType: $messageType")
            Log.w(TAG, "  ChatId: $chatId")

            if (senderId.isNullOrBlank()) {
                Log.e(TAG, "❌ SenderId is null or blank - cannot process notification")
                return
            }

            if (senderName.isNullOrBlank()) {
                Log.e(TAG, "❌ SenderName is null or blank - cannot process notification")
                return
            }

            Log.w(TAG, "✅ FCM data validation passed - processing notification...")

            // Process notification in coroutine
            serviceScope.launch {
                try {
                    processNotification(senderId, senderName, message, messageType, chatId)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing notification in coroutine", e)
                    // Fallback: try to show basic notification
                    tryFallbackNotification(
                        senderName,
                        message ?: "New message",
                        chatId ?: "unknown"
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error processing FCM message", e)
            // Last resort fallback  
            tryFallbackNotification("New Message", "You have received a new message", "unknown")
        }

        Log.w(TAG, "==========================================")
        Log.w(TAG, "=== END FCM MESSAGE PROCESSING ===")
        Log.w(TAG, "==========================================")
    }

    /**
     * Process notification with proper error handling
     */
    private suspend fun processNotification(
        senderId: String,
        senderName: String,
        message: String?,
        messageType: String?,
        chatId: String?
    ) {
        try {
            Log.d(TAG, "Processing notification for senderId: $senderId")

            // Check if we should show the notification
            val shouldShow = shouldShowChatNotification(senderId)
            Log.d(TAG, "Final decision - shouldShowNotification: $shouldShow")

            if (shouldShow) {
                Log.d(TAG, "=== SHOWING NOTIFICATION ===")

                notificationHelper.sendChatNotification(
                    senderId = senderId,
                    senderName = senderName,
                    messageBody = message ?: "New message",
                    chatId = chatId ?: "unknown"
                )
            } else {
                Log.d(TAG, "Notification suppressed - chat is currently open or app in foreground")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in processNotification", e)
            throw e // Re-throw to trigger fallback
        }
    }

    /**
     * Fallback notification method
     */
    private fun tryFallbackNotification(title: String, message: String, chatId: String) {
        try {
            Log.w(TAG, "Using fallback notification method")
            serviceScope.launch {
                notificationHelper.sendChatNotification(
                    senderId = "unknown",
                    senderName = title,
                    messageBody = message,
                    chatId = chatId
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Even fallback notification failed", e)
        }
    }

    /**
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "=== NEW FCM TOKEN RECEIVED ===")
        Log.d(TAG, "New token: ${token.take(20)}...")

        if (XiaomiPermissionHelper.isXiaomiDevice()) {
            Log.w(TAG, "New FCM token received on Xiaomi device")
            Log.w(
                TAG,
                "Xiaomi devices may regenerate tokens more frequently due to MIUI optimizations"
            )
        }

        try {
            // Here you would typically send the token to your server
            serviceScope.launch {
                updateFcmToken(token)
            }
            Log.i(TAG, "FCM token updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new FCM token", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")

        if (XiaomiPermissionHelper.isXiaomiDevice()) {
            Log.w(TAG, "Firebase Messaging Service destroyed on Xiaomi device")
            Log.w(
                TAG,
                "Service may be killed by MIUI - ensure autostart and battery optimization are configured"
            )
        }

        super.onDestroy()
    }
}
