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
 * Service handling Firebase Cloud Messaging events.
 *
 * Responsibilities:
 * 1. Receive and process new FCM tokens.
 * 2. Persist the token via UpdateFcmTokenUseCase.
 * 3. Receive data messages and delegate notification display
 *    decisions to ShouldShowChatNotificationUseCase.
 * 4. Delegate notification rendering to NotificationHelper.
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
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
        serviceScope.launch {
            try {
                updateFcmToken(token)
                Log.i(TAG, "Token sent to server.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send FCM token.", e)
            }
        }
    }

    /**
     * Called when a data message is received from FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.takeIf { it.isNotEmpty() }?.let { data ->
            val senderId       = data["senderId"]       ?: return
            val chatId         = data["chatId"]         ?: return
            val senderName     = data["senderName"]     ?: "Someone"
            val messagePreview = data["messagePreview"] ?: "New message"

            if (shouldShowChatNotification(senderId)) {
                notifHelper.sendChatNotification(
                    senderId    = senderId,
                    senderName  = senderName,
                    messageBody = messagePreview,
                    chatId      = chatId
                )
            } else {
                Log.d(TAG, "Notification suppressed for chat $senderId.")
            }
        }
    }
}
