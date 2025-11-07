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
 * Dagger Hilt module for notification-related dependencies.
 *
 * This module binds all the notification-related interfaces to their
 * concrete implementations, following Clean Architecture principles.
 */
@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused") // This module is used by Hilt's dependency injection system
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationDataSource(
        androidNotificationDataSource: AndroidNotificationDataSource
    ): NotificationDataSource

    @Binds
    @Singleton
    abstract fun bindNotificationChannelManager(
        androidNotificationChannelManager: AndroidNotificationChannelManager
    ): NotificationChannelManager

    @Binds
    @Singleton
    abstract fun bindNotificationBuilder(
        androidNotificationBuilder: AndroidNotificationBuilder
    ): NotificationBuilder

    @Binds
    @Singleton
    abstract fun bindPendingIntentFactory(
        androidPendingIntentFactory: AndroidPendingIntentFactory
    ): PendingIntentFactory

    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(
        androidDeviceInfoProvider: AndroidDeviceInfoProvider
    ): DeviceInfoProvider
}
