package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.domain.usecases.CancelChatNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelUserNotificationsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Updated Dagger Hilt module for providing use case dependencies.
 *
 * This module now includes the new Clean Architecture notification use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // Note: Most of the new notification use cases are automatically provided by Hilt
    // since they are annotated with @Singleton and @Inject constructor.
    // Only the legacy compatibility use case needs explicit provision.

    @Provides
    @Singleton
    fun provideCancelChatNotificationsUseCase(
        cancelUserNotificationsUseCase: CancelUserNotificationsUseCase
    ): CancelChatNotificationsUseCase {
        return CancelChatNotificationsUseCase(cancelUserNotificationsUseCase)
    }

    // Other use cases (ShowChatNotificationUseCase, CancelUserNotificationsUseCase, etc.)
    // are automatically provided by Hilt constructor injection.
}
