package com.yerayyas.chatappkotlinproject.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @ServiceCoroutineScope
    @Provides
    @Singleton
    fun provideServiceCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceCoroutineScope