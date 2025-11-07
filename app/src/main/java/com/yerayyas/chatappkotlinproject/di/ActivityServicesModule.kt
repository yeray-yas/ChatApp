package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.domain.usecases.ProcessNotificationIntentUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.UpdateFcmTokenUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationCanceller
import com.yerayyas.chatappkotlinproject.presentation.activity.services.ActivityInitializationService
import com.yerayyas.chatappkotlinproject.presentation.activity.services.NotificationIntentService
import com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions.NotificationPermissionManager
import com.yerayyas.chatappkotlinproject.presentation.activity.services.permissions.PlayServicesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ActivityServicesModule {

    @Provides
    @Singleton
    fun providePlayServicesManager(): PlayServicesManager {
        return PlayServicesManager()
    }

    @Provides
    @Singleton
    fun provideNotificationPermissionManager(): NotificationPermissionManager {
        return NotificationPermissionManager()
    }

    @Provides
    @Singleton
    fun provideActivityInitializationService(
        playServicesManager: PlayServicesManager,
        notificationPermissionManager: NotificationPermissionManager,
        notificationCanceller: NotificationCanceller,
        updateFcmTokenUseCase: UpdateFcmTokenUseCase
    ): ActivityInitializationService {
        return ActivityInitializationService(
            playServicesManager,
            notificationPermissionManager,
            notificationCanceller,
            updateFcmTokenUseCase
        )
    }

    @Provides
    @Singleton
    fun provideNotificationIntentService(
        processNotificationIntent: ProcessNotificationIntentUseCase
    ): NotificationIntentService {
        return NotificationIntentService(processNotificationIntent)
    }
}