package com.yerayyas.chatappkotlinproject.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yerayyas.chatappkotlinproject.di.ServiceCoroutineScope
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.ShouldShowChatNotificationUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
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

    @Inject @ServiceCoroutineScope
    lateinit var serviceScope: CoroutineScope

    @Inject
    lateinit var updateFcmToken: UpdateFcmTokenUseCase

    @Inject
    lateinit var shouldShowChatNotification: ShouldShowChatNotificationUseCase

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var notifHelper: NotificationHelper

    /**
     * Called when a new FCM registration token is generated.
     *
     * This method is invoked by the Firebase SDK whenever a new token is created or an existing one is
     * refreshed. The new token is then sent to the backend server to keep it up-to-date.
     *
     * @param token The new FCM token as a [String].
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: ${token.take(10)}...")
        serviceScope.launch {
            try {
                updateFcmToken(token)
                Log.i(TAG, "FCM token update successfully sent to server.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send FCM token to server.", e)
            }
        }
    }

    /**
     * Called when a new data message is received from FCM.
     *
     * This method processes incoming data messages. It first checks if the notification should be shown
     * based on the current app state (e.g., if the user is already in the specific chat screen).
     * If the notification is warranted, it delegates the display logic to [NotificationHelper].
     *
     * @param remoteMessage The [RemoteMessage] object containing the message data from FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "New FCM data message received from: ${remoteMessage.from}")
        remoteMessage.data.takeIf { it.isNotEmpty() }?.let { data ->
            val senderId       = data["senderId"]       ?: return
            val chatId         = data["chatId"]         ?: return
            val senderName     = data["senderName"]     ?: "Someone"
            val messagePreview = data["messagePreview"] ?: "New message"

            if (shouldShowChatNotification(senderId)) {
                Log.d(TAG, "Notification condition met. Displaying notification for sender: $senderId")
                notifHelper.sendChatNotification(
                    senderId    = senderId,
                    senderName  = senderName,
                    messageBody = messagePreview,
                    chatId      = chatId
                )
            } else {
                Log.d(TAG, "Notification suppressed for sender: $senderId as chat is likely open.")
            }
        }
    }
}
