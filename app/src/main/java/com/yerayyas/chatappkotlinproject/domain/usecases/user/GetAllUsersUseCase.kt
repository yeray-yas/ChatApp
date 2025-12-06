package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case to get a real-time stream of all users, excluding the current authenticated user.
 *
 * This is used, for example, when selecting members to add to a new group, ensuring
 * the user cannot select themselves.
 *
 * @property userRepository Repository to fetch user data.
 * @property auth Service to identify the current user.
 */
class GetAllUsersUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) {
    // The 'suspend' keyword is not needed here as we are returning a Flow.
    operator fun invoke(): Flow<List<User>> {
        val currentUserId = auth.currentUser?.uid
        // The getAllUsers() method in the repository is already suspend, but it returns a Flow.
        // We use .map to transform the emitted list from the flow.
        return userRepository.getAllUsers().map { users ->
            if (currentUserId != null) {
                users.filter { it.id != currentUserId }
            } else {
                users
            }
        }
    }
}
