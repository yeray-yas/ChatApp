package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import com.yerayyas.chatappkotlinproject.utils.AppState
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShouldShowChatNotif"

/**
 * Decides whether a chat notification should be shown.
 *
 * A notification is shown if:
 *  1) The app is in background, or
 *  2) The app is foreground but the chat with senderId is not currently open.
 */
@Singleton
class ShouldShowChatNotificationUseCase @Inject constructor(
    private val appState: AppState
) {

    /**
     * @param senderId ID of the user who sent the message.
     * @return true if a system notification should be displayed.
     */
    operator fun invoke(senderId: String): Boolean {
        val isAppBackground = !appState.isAppInForeground
        val isChatOpenForSender = appState.currentOpenChatUserId == senderId

        Log.d(TAG, "=== NOTIFICATION DECISION ===")
        Log.d(TAG, "SenderId: $senderId")
        Log.d(TAG, "isAppInForeground: ${appState.isAppInForeground}")
        Log.d(TAG, "isAppBackground: $isAppBackground")
        Log.d(TAG, "currentOpenChatUserId: ${appState.currentOpenChatUserId}")
        Log.d(TAG, "isChatOpenForSender: $isChatOpenForSender")

        val shouldShow = isAppBackground || !isChatOpenForSender

        // TEMPORARY: Force notifications for debugging
        val forcedDecision = true
        Log.d(TAG, "Original decision - shouldShowNotification: $shouldShow")
        Log.d(TAG, "TEMPORARY OVERRIDE - forcing notification: $forcedDecision")
        Log.d(
            TAG,
            "Reason: ${if (isAppBackground) "App is in background" else if (!isChatOpenForSender) "Chat is not open for this sender" else "Chat is currently open for this sender"}"
        )
        Log.d(TAG, "=========================")

        return forcedDecision // TEMPORARY: Always show notifications
    }
}
