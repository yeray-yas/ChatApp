package com.yerayyas.chatappkotlinproject.domain.usecases.auth

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.user.ManageUserPresenceUseCase
import javax.inject.Inject

/**
 * Use case to sign out the current user.
 * Ensures the user is marked as offline BEFORE destroying the session.
 */
class SignOutUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val manageUserPresenceUseCase: ManageUserPresenceUseCase
) {
    suspend operator fun invoke() {
        try {
            manageUserPresenceUseCase.stopPresenceUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            userRepository.clearCurrentUserFCMToken()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        firebaseAuth.signOut()
    }
}
