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

/**
 * Hilt module that provides a custom CoroutineScope for application-wide background tasks.
 *
 * This module is installed in the SingletonComponent, ensuring that the same CoroutineScope
 * instance is injected throughout the entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    /**
     * Provides a singleton CoroutineScope configured for I/O-bound operations.
     *
     * This scope uses `Dispatchers.IO` to run tasks on a background thread pool, making it suitable
     * for network requests, disk access, or other long-running operations that should not block the main thread.
     *
     * It is also configured with a `SupervisorJob`, which means that if one of the child coroutines
     * fails, it will not cancel the other coroutines in the same scope.
     *
     * @return A CoroutineScope instance.
     */
    @ServiceCoroutineScope
    @Provides
    @Singleton
    fun provideServiceCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}

/**
 * Qualifier annotation to distinguish the application-wide CoroutineScope provided by [CoroutineScopeModule].
 *
 * This annotation is used to avoid ambiguity when injecting a CoroutineScope, ensuring that Hilt provides
 * the specific instance configured for background services.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceCoroutineScope
