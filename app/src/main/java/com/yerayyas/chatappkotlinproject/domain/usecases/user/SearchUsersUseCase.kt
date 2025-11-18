package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case to search for users by a query string, excluding the current user from the results.
 *
 * @property userRepository Repository to fetch user data.
 * @property auth Service to identify the current user.
 */
class SearchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(query: String): List<User> {
        val currentUserId = auth.currentUser?.uid
        val results = userRepository.searchUsers(query)
        return if (currentUserId != null) {
            results.filter { it.id != currentUserId }
        } else {
            results
        }
    }
}
