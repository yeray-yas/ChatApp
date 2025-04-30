package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves the current authenticated user’s ID.
 */
@Singleton
class GetCurrentUserIdUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * @return the current user ID or empty string if none.
     */
    operator fun invoke(): String = repository.getCurrentUserId()
}
