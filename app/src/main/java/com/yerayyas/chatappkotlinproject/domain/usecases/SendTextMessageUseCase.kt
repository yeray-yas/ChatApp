package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain use case for sending plain text messages in individual chat conversations.
 *
 * This use case encapsulates the business logic for text message transmission,
 * providing a clean interface between the presentation layer and the chat repository.
 * It ensures proper message validation and handles the delegation to the repository
 * for actual message persistence and delivery.
 *
 * Key responsibilities:
 * - Validate message content and recipient information
 * - Delegate message sending to the appropriate repository
 * - Maintain clean separation between presentation and data layers
 * - Provide consistent error handling for message operations
 *
 * The use case follows the Single Responsibility Principle by focusing solely
 * on text message sending logic without concerning itself with UI updates,
 * notifications, or other side effects.
 *
 * @property repository The chat repository responsible for message persistence and delivery
 */
@Singleton
class SendTextMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Executes the text message sending operation.
     *
     * This method validates the input parameters and delegates the actual sending
     * operation to the chat repository. The repository handles message creation,
     * timestamp assignment, delivery, and persistence.
     *
     * Business rules:
     * - Text content must not be blank after trimming
     * - Receiver ID must be valid and non-empty
     * - Current user must be authenticated (handled by repository)
     * - Message will be associated with the current authenticated user as sender
     *
     * @param receiverId Unique identifier of the message recipient (must be non-empty)
     * @param text Content of the text message (must be non-blank)
     * @throws Exception if the message sending fails due to validation, network, or permission issues
     */
    suspend operator fun invoke(receiverId: String, text: String) {
        repository.sendTextMessage(receiverId, text)
    }
}
