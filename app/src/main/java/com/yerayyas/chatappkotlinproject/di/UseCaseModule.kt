package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelChatNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelUserNotificationsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing use case dependencies and compatibility bridges.
 *
 * This module handles the dependency injection of use cases that require manual configuration
 * or serve as compatibility layers between different architectural approaches. Most use cases
 * in the application are automatically provided by Hilt through constructor injection,
 * but some require explicit configuration due to legacy compatibility or complex dependency requirements.
 *
 * Architecture patterns supported:
 * - **Clean Architecture Use Cases**: Domain layer business logic operations
 * - **Legacy Compatibility**: Bridge pattern for older notification system integration
 * - **Dependency Composition**: Manual wiring of complex use case dependencies
 *
 * The module follows these principles:
 * - Automatic provision through @Inject constructor when possible
 * - Manual provision only when required for compatibility or complex scenarios
 * - Singleton scope for stateless use cases to optimize memory usage
 * - Clear separation between domain logic and infrastructure concerns
 *
 * Most notification use cases (ShowChatNotificationUseCase, CancelUserNotificationsUseCase,
 * CancelAllNotificationsUseCase, etc.) are automatically provided by Hilt since they use
 * @Singleton and @Inject constructor annotations.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    /**
     * Provides [CancelChatNotificationsUseCase] with legacy compatibility support.
     *
     * This use case serves as a compatibility bridge between the legacy notification system
     * and the new Clean Architecture notification implementation. It wraps the new
     * [CancelUserNotificationsUseCase] to maintain backward compatibility with existing
     * ViewModels and UI components that depend on the legacy interface.
     *
     * The use case provides:
     * - Individual chat notification cancellation through legacy interface
     * - Group chat notification cancellation with modern implementation
     * - Error handling and logging for notification operations
     * - Safe operation that doesn't disrupt UI flow on failures
     *
     * @param cancelUserNotificationsUseCase The modern notification cancellation use case
     * @return CancelChatNotificationsUseCase configured for legacy compatibility
     */
    @Provides
    @Singleton
    fun provideCancelChatNotificationsUseCase(
        cancelUserNotificationsUseCase: CancelUserNotificationsUseCase
    ): CancelChatNotificationsUseCase {
        return CancelChatNotificationsUseCase(cancelUserNotificationsUseCase)
    }

    // Note: The following use cases are automatically provided by Hilt constructor injection
    // and do not require explicit provision in this module:
    //
    // Notification Use Cases (automatically provided):
    // - ShowChatNotificationUseCase: Handles notification display operations
    // - CancelUserNotificationsUseCase: Cancels notifications for specific users/groups
    // - CancelAllNotificationsUseCase: Clears all application notifications
    // - UpdateFcmTokenUseCase: Manages FCM token updates for push notifications
    // - ProcessNotificationIntentUseCase: Processes notification tap intents for navigation
    //
    // Group Management Use Cases (automatically provided):
    // - CreateGroupUseCase: Group creation with validation and persistence
    // - ManageGroupMembersUseCase: Member addition, removal, and permission management
    // - All other domain use cases with @Inject constructor and @Singleton annotations
}
