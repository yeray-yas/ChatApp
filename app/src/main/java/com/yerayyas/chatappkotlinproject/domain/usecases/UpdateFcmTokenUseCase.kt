package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateFcmTokenUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Actualiza el token FCM del usuario actual en la base de datos.
     * Lanza excepción si falla.
     */
    suspend operator fun invoke(token: String) {
        userRepository.updateCurrentUserFCMToken(token)
    }
}