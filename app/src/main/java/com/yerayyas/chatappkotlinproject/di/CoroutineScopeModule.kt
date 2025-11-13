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
 * Dagger Hilt module that provides application-scoped CoroutineScope for background operations.
 *
 * This module configures and provides a specialized CoroutineScope designed for long-running
 * background tasks that should survive individual component lifecycles. The scope is installed
 * in SingletonComponent to ensure it lives throughout the entire application lifecycle.
 *
 * Key characteristics of the provided scope:
 * - **Dispatcher**: Uses Dispatchers.IO for I/O-bound operations (network, database, file operations)
 * - **Job**: SupervisorJob ensures child coroutine failures don't cancel siblings
 * - **Lifecycle**: Application-scoped, survives activity/fragment recreation
 * - **Use cases**: Background services, repository operations, periodic tasks
 *
 * The scope is particularly useful for:
 * - Firebase operations that should continue during configuration changes
 * - Background synchronization tasks
 * - Long-running file operations or downloads
 * - Service operations that need to survive UI component destruction
 *
 * Usage pattern: Inject with @ServiceCoroutineScope qualifier to distinguish from
 * other CoroutineScope instances in the dependency graph.
 *
 * Note: Currently not actively used but provided for future service implementations
 * that may require application-scoped background operations.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    /**
     * Provides a singleton CoroutineScope optimized for background I/O operations.
     *
     * This scope is configured with:
     * - **Dispatchers.IO**: Optimized thread pool for I/O-bound operations including:
     *   - Network requests (Firebase API calls)
     *   - Database operations (Room, Firebase Realtime Database)
     *   - File system operations (image caching, preferences)
     *   - Blocking operations that shouldn't block the main thread
     *
     * - **SupervisorJob**: Provides failure isolation where:
     *   - Individual child coroutine failures don't cancel the entire scope
     *   - Other operations can continue even if one operation fails
     *   - Enables robust error handling for background services
     *   - Maintains scope stability for long-running operations
     *
     * The scope is suitable for repository-level operations, background synchronization,
     * and service operations that need to survive UI component lifecycle changes.
     *
     * @return Application-scoped CoroutineScope configured for background I/O operations
     */
    @ServiceCoroutineScope
    @Provides
    @Singleton
    fun provideServiceCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}

/**
 * Qualifier annotation for distinguishing the application-scoped CoroutineScope.
 *
 * This annotation is used to resolve dependency injection ambiguity when multiple
 * CoroutineScope instances exist in the dependency graph. It specifically identifies
 * the application-scoped scope provided by [CoroutineScopeModule].
 *
 * Usage examples:
 * ```kotlin
 * class MyService @Inject constructor(
 *     @ServiceCoroutineScope private val serviceScope: CoroutineScope
 * ) {
 *     fun performBackgroundOperation() {
 *         serviceScope.launch {
 *             // Long-running background operation
 *         }
 *     }
 * }
 * ```
 *
 * The qualifier ensures that the correct scope is injected for services that need
 * application-wide lifecycle management rather than component-specific scopes.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ServiceCoroutineScope

