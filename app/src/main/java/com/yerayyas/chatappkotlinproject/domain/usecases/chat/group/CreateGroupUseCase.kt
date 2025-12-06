package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case for creating a new group chat.
 *
 * This use case handles the complete process of creating a group chat including:
 * - User authentication validation
 * - Input validation (name, description, member limits)
 * - Optional group image upload
 * - Group creation with proper member and admin setup
 * - Error handling with descriptive messages
 *
 * The creator of the group is automatically added as both a member and an administrator.
 */
class CreateGroupUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    private val TAG = "CreateGroupUseCase"
    /**
     * Creates a new group chat with the specified parameters.
     *
     * @param name Group name (required, max 100 characters)
     * @param description Group description (optional, max 500 characters)
     * @param memberIds List of initial member IDs (at least 1 required, max 256)
     * @param imageUri Group image URI (optional)
     * @return Result containing the created group ID on success, or an exception on failure
     */
    suspend operator fun invoke(
        name: String,
        description: String,
        memberIds: List<String>,
        imageUri: Uri? = null
    ): Result<String> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Input validations
            if (name.isBlank()) {
                return Result.failure(IllegalArgumentException("Group name cannot be empty"))
            }

            if (name.length > 100) {
                return Result.failure(IllegalArgumentException("Group name cannot exceed 100 characters"))
            }

            if (description.length > 500) {
                return Result.failure(IllegalArgumentException("Description cannot exceed 500 characters"))
            }

            if (memberIds.isEmpty()) {
                return Result.failure(IllegalArgumentException("Must add at least one member to the group"))
            }

            if (memberIds.size > 256) {
                return Result.failure(IllegalArgumentException("A group cannot have more than 256 members"))
            }

            // Create GroupChat object
            val group = GroupChat(
                name = name.trim(),
                description = description.trim(),
                memberIds = (memberIds + currentUserId).distinct(), // Add creator and remove duplicates
                adminIds = listOf(currentUserId), // Creator becomes admin
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis(),
                isActive = true
            )

            // Create the group first
            val createResult = groupChatRepository.createGroup(group)
            if (createResult.isFailure) {
                return createResult
            }

            val groupId = createResult.getOrThrow()

            // Upload group image if provided
            imageUri?.let { uri ->
                val imageUploadResult = groupChatRepository.updateGroupImage(groupId, uri)
                if (imageUploadResult.isFailure) {
                    // Log the image upload failure but don't fail the entire group creation
                    // The group was created successfully, just without the image
                    Log.w(
                        TAG,
                        "Group created successfully but image upload failed: ${imageUploadResult.exceptionOrNull()?.message}"
                    )
                }
            }

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}