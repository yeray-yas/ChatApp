package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case to load the profile of the currently authenticated user.
 * It returns the full User object.
 */
class LoadUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<User?> {
        return try {
            Result.success(userRepository.getCurrentUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
