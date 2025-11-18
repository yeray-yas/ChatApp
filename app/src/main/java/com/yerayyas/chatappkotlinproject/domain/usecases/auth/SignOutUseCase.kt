package com.yerayyas.chatappkotlinproject.domain.usecases.auth

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case to sign out the current user.
 * It also clears the FCM token from the repository.
 */
class SignOutUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() {
        userRepository.clearCurrentUserFCMToken()
        firebaseAuth.signOut()
    }
}