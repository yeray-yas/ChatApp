package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class GetCurrentUserIdUseCase @Inject constructor(
    private val auth: FirebaseAuth
) {
    operator fun invoke(): String? {
        return auth.currentUser?.uid
    }
}