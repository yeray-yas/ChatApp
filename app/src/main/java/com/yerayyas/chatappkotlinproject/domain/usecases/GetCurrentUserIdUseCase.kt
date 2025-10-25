package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A use case dedicated to retrieving the unique identifier (UID) of the currently authenticated user.
 *
 * This class abstracts the logic of accessing the user session data by querying the repository.
 * It provides a simple, reusable component to get the current user's ID from anywhere in the app
 * where this use case is injected.
 *
 * @property repository The repository responsible for providing user session data.
 */
@Singleton
class GetCurrentUserIdUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Retrieves the unique ID of the currently signed-in user.
     *
     * @return A [String] containing the current user's UID, or an empty string if no user is authenticated.
     */
    operator fun invoke(): String = repository.getCurrentUserId()
}
