package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to fetch a reactive stream of ALL users from the repository.
 * The filtering and sorting logic is handled by the ViewModel.
 */
class FetchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<List<User>> {
        return userRepository.getAllUsers()
    }
}
