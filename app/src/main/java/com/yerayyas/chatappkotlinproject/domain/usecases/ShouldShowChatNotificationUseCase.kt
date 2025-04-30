package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.utils.AppState
import javax.inject.Inject
import javax.inject.Singleton

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
        return isAppBackground || !isChatOpenForSender
    }
}
