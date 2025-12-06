package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.domain.usecases.ProcessNotificationIntentUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelAllNotificationsUseCase
import com.yerayyas.chatappkotlinproject.presentation.activity.services.ActivityInitializationService
import com.yerayyas.chatappkotlinproject.presentation.activity.services.NotificationIntentService
import com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions.NotificationPermissionManager
import com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions.PlayServicesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Activity-related services and their dependencies.
 *
 * This module follows Clean Architecture principles by providing services that coordinate
 * between different layers of the application. It ensures proper dependency injection
 * for activity initialization, notification processing, and permission management.
 *
 * Key responsibilities:
 * - Providing activity initialization services with proper dependencies
 * - Configuring notification intent processing services
 * - Managing permission-related services (Play Services, Notifications)
 * - Ensuring singleton lifecycle for service instances
 * - Following dependency inversion principle through use case injection
 */
@Module
@InstallIn(SingletonComponent::class)
object ActivityServicesModule {

    /**
     * Provides a singleton instance of [PlayServicesManager] for Google Play Services verification.
     *
     * @return PlayServicesManager instance for managing Google Play Services availability
     */
    @Provides
    @Singleton
    fun providePlayServicesManager(): PlayServicesManager {
        return PlayServicesManager()
    }

    /**
     * Provides a singleton instance of [NotificationPermissionManager] for notification permissions.
     *
     * This manager handles notification permission requests and status checking,
     * following Android's modern permission request patterns.
     *
     * @return NotificationPermissionManager instance for managing notification permissions
     */
    @Provides
    @Singleton
    fun provideNotificationPermissionManager(): NotificationPermissionManager {
        return NotificationPermissionManager()
    }

    /**
     * Provides a singleton instance of [ActivityInitializationService] with all required dependencies.
     *
     * This service coordinates the initialization process for MainActivity, including:
     * - Google Play Services verification through [PlayServicesManager]
     * - Notification permission management through [NotificationPermissionManager]
     * - Notification clearing using domain layer [CancelAllNotificationsUseCase]
     * - FCM token management through [UpdateFcmTokenUseCase]
     *
     * @param playServicesManager Manager for Google Play Services operations
     * @param notificationPermissionManager Manager for notification permission operations
     * @param cancelAllNotificationsUseCase Use case for clearing notifications (domain layer)
     * @param updateFcmTokenUseCase Use case for FCM token management (domain layer)
     * @return ActivityInitializationService configured with all dependencies
     */
    @Provides
    @Singleton
    fun provideActivityInitializationService(
        playServicesManager: PlayServicesManager,
        notificationPermissionManager: NotificationPermissionManager,
        cancelAllNotificationsUseCase: CancelAllNotificationsUseCase,
        updateFcmTokenUseCase: UpdateFcmTokenUseCase
    ): ActivityInitializationService {
        return ActivityInitializationService(
            playServicesManager = playServicesManager,
            notificationPermissionManager = notificationPermissionManager,
            cancelAllNotificationsUseCase = cancelAllNotificationsUseCase,
            updateFcmTokenUseCase = updateFcmTokenUseCase
        )
    }

    /**
     * Provides a singleton instance of [NotificationIntentService] for processing notification intents.
     *
     * This service handles deep-link navigation from notifications by processing intents
     * and coordinating with ViewModels. It uses domain layer use cases for business logic.
     *
     * @param processNotificationIntent Use case for processing notification intents (domain layer)
     * @return NotificationIntentService configured with intent processing capability
     */
    @Provides
    @Singleton
    fun provideNotificationIntentService(
        processNotificationIntent: ProcessNotificationIntentUseCase
    ): NotificationIntentService {
        return NotificationIntentService(processNotificationIntent)
    }
}
