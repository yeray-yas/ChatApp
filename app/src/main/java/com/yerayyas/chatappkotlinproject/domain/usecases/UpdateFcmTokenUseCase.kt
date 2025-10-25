package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A use case responsible for updating the FCM (Firebase Cloud Messaging) token for the current user.
 *
 * This class follows the single-responsibility principle, where its only job is to delegate the token
 * update operation to the user repository. By using the `invoke` operator, this use case can be called
 * as if it were a function.
 *
 * @property userRepository The repository that handles user data operations.
 */
@Singleton
class UpdateFcmTokenUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Updates the current user's FCM token in the backend database.
     *
     * @param token The new FCM token to be saved.
     * @throws Exception if the update operation in the repository fails.
     */
    suspend operator fun invoke(token: String) {
        userRepository.updateCurrentUserFCMToken(token)
    }
}
