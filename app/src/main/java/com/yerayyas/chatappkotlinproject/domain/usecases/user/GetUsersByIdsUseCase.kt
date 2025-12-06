package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import javax.inject.Inject

class GetUsersByIdsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userIds: List<String>): List<User> {
        return userRepository.getUsersByIds(userIds)
    }
}