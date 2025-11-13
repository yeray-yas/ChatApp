package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.data.datasource.AndroidNotificationDataSource
import com.yerayyas.chatappkotlinproject.data.datasource.NotificationDataSource
import com.yerayyas.chatappkotlinproject.data.repository.NotificationRepositoryImpl
import com.yerayyas.chatappkotlinproject.domain.repository.NotificationRepository
import com.yerayyas.chatappkotlinproject.domain.service.DeviceInfoProvider
import com.yerayyas.chatappkotlinproject.domain.service.NotificationBuilder
import com.yerayyas.chatappkotlinproject.domain.service.NotificationChannelManager
import com.yerayyas.chatappkotlinproject.domain.service.PendingIntentFactory
import com.yerayyas.chatappkotlinproject.infrastructure.service.AndroidDeviceInfoProvider
import com.yerayyas.chatappkotlinproject.infrastructure.service.AndroidNotificationBuilder
import com.yerayyas.chatappkotlinproject.infrastructure.service.AndroidNotificationChannelManager
import com.yerayyas.chatappkotlinproject.infrastructure.service.AndroidPendingIntentFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for notification-related dependencies and service bindings.
 *
 * This module implements the Dependency Inversion Principle by binding abstract domain interfaces
 * to their concrete Android platform implementations. It ensures a clean separation between
 * the notification business logic (domain layer) and platform-specific implementations
 * (infrastructure layer).
 *
 * Architecture layers bound:
 * - **Domain Layer**: Interfaces defining notification contracts and business rules
 * - **Infrastructure Layer**: Android-specific implementations of notification services
 * - **Data Layer**: Repository implementations for notification data management
 *
 * Key notification services provided:
 * - **NotificationRepository**: Orchestrates notification operations across the app
 * - **NotificationDataSource**: Platform-specific notification display and management
 * - **NotificationChannelManager**: Android notification channel configuration
 * - **NotificationBuilder**: Creates platform-optimized notification objects
 * - **PendingIntentFactory**: Generates secure intents for notification actions
 * - **DeviceInfoProvider**: Supplies device compatibility information for optimization
 *
 * All bindings use singleton scope to ensure consistent behavior and optimal resource usage
 * throughout the application lifecycle.
 *
 * The module follows Clean Architecture principles by:
 * - Abstracting platform dependencies through interfaces
 * - Enabling easy testing through dependency injection
 * - Supporting multiple platform implementations if needed
 * - Maintaining clear separation of concerns
 */
@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused") // This module is used by Hilt's dependency injection system
abstract class NotificationModule {

    /**
     * Binds the [NotificationRepositoryImpl] to the [NotificationRepository] interface.
     *
     * The repository coordinates between different notification services and use cases,
     * providing a unified interface for all notification operations in the domain layer.
     * It handles permission checking, notification display orchestration, and cancellation.
     *
     * @param notificationRepositoryImpl The concrete repository implementation
     * @return NotificationRepository interface for domain layer consumption
     */
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    /**
     * Binds the [AndroidNotificationDataSource] to the [NotificationDataSource] interface.
     *
     * The data source provides platform-specific notification operations including
     * display, cancellation, and state management. The Android implementation handles
     * notification channels, grouping, and system integration.
     *
     * @param androidNotificationDataSource The Android-specific data source implementation
     * @return NotificationDataSource interface for repository layer consumption
     */
    @Binds
    @Singleton
    abstract fun bindNotificationDataSource(
        androidNotificationDataSource: AndroidNotificationDataSource
    ): NotificationDataSource

    /**
     * Binds the [AndroidNotificationChannelManager] to the [NotificationChannelManager] interface.
     *
     * The channel manager handles Android notification channel creation and configuration,
     * ensuring proper categorization and user control over different notification types.
     * It manages importance levels, sound settings, and vibration patterns.
     *
     * @param androidNotificationChannelManager The Android-specific channel manager
     * @return NotificationChannelManager interface for notification system configuration
     */
    @Binds
    @Singleton
    abstract fun bindNotificationChannelManager(
        androidNotificationChannelManager: AndroidNotificationChannelManager
    ): NotificationChannelManager

    /**
     * Binds the [AndroidNotificationBuilder] to the [NotificationBuilder] interface.
     *
     * The notification builder creates platform-optimized notification objects with
     * proper styling, actions, and content. The Android implementation handles
     * Material Design guidelines and system-specific notification features.
     *
     * @param androidNotificationBuilder The Android-specific notification builder
     * @return NotificationBuilder interface for creating notification objects
     */
    @Binds
    @Singleton
    abstract fun bindNotificationBuilder(
        androidNotificationBuilder: AndroidNotificationBuilder
    ): NotificationBuilder

    /**
     * Binds the [AndroidPendingIntentFactory] to the [PendingIntentFactory] interface.
     *
     * The pending intent factory creates secure intents for notification actions,
     * handling deep-link navigation and action processing. The Android implementation
     * manages intent flags and security considerations for different Android versions.
     *
     * @param androidPendingIntentFactory The Android-specific pending intent factory
     * @return PendingIntentFactory interface for creating notification action intents
     */
    @Binds
    @Singleton
    abstract fun bindPendingIntentFactory(
        androidPendingIntentFactory: AndroidPendingIntentFactory
    ): PendingIntentFactory

    /**
     * Binds the [AndroidDeviceInfoProvider] to the [DeviceInfoProvider] interface.
     *
     * The device info provider supplies device-specific information for notification
     * optimization and compatibility. This includes Android version, device capabilities,
     * and manufacturer-specific notification behaviors.
     *
     * @param androidDeviceInfoProvider The Android-specific device info provider
     * @return DeviceInfoProvider interface for accessing device compatibility information
     */
    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(
        androidDeviceInfoProvider: AndroidDeviceInfoProvider
    ): DeviceInfoProvider
}
