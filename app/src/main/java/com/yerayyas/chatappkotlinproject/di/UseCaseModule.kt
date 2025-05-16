package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.domain.usecases.CancelChatNotificationsUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideCancelChatNotificationsUseCase(
        notificationHelper: NotificationHelper
    ): CancelChatNotificationsUseCase {
        return CancelChatNotificationsUseCase(notificationHelper)
    }
}
