package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import com.yerayyas.chatappkotlinproject.utils.AppState
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShouldShowChatNotif"

/**
 * Decides whether a chat notification should be shown.
 *
 * For individual chats:
 * A notification is shown if:
 *  1) The app is in background, or
 *  2) The app is foreground but the individual chat with senderId is not currently open.
 *
 * For group chats:
 * A notification is shown if:
 *  1) The app is in background, or
 *  2) The app is foreground but the group chat with groupId is not currently open.
 */
@Singleton
class ShouldShowChatNotificationUseCase @Inject constructor(
    private val appState: AppState
) {

    /**
     * Determines if notification should be shown for individual chat
     * @param senderId ID of the user who sent the message.
     * @return true if a system notification should be displayed.
     */
    operator fun invoke(senderId: String): Boolean {
        return shouldShowIndividualChatNotification(senderId)
    }

    /**
     * Determines if notification should be shown for individual chat
     * @param senderId ID of the user who sent the message.
     * @return true if a system notification should be displayed.
     */
    fun shouldShowIndividualChatNotification(senderId: String): Boolean {
        val isAppBackground = !appState.isAppInForeground
        val isChatOpenForSender = appState.isIndividualChatOpen(senderId)

        val shouldShow = isAppBackground || !isChatOpenForSender

        Log.d(
            TAG,
            "Individual chat notification decision for $senderId: $shouldShow (app in background: $isAppBackground, chat open: $isChatOpenForSender)"
        )

        return shouldShow
    }

    /**
     * Determines if notification should be shown for group chat
     * @param groupId ID of the group where the message was sent.
     * @return true if a system notification should be displayed.
     */
    fun shouldShowGroupChatNotification(groupId: String): Boolean {
        val isAppBackground = !appState.isAppInForeground
        val isGroupChatOpen = appState.isGroupChatOpen(groupId)

        val shouldShow = isAppBackground || !isGroupChatOpen

        Log.d(
            TAG,
            "Group chat notification decision for $groupId: $shouldShow (app in background: $isAppBackground, group chat open: $isGroupChatOpen)"
        )

        return shouldShow
    }
}
