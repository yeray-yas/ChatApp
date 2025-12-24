package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.ThemeRepository
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.data.repository.ChatRepositoryImpl
import com.yerayyas.chatappkotlinproject.data.repository.ThemeRepositoryImpl
import com.yerayyas.chatappkotlinproject.data.repository.UserRepositoryImpl
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.data.repository.GroupChatRepositoryImpl
import com.yerayyas.chatappkotlinproject.data.service.FirebaseAuthenticationService
import com.yerayyas.chatappkotlinproject.domain.interfaces.AuthenticationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module responsible for binding repository implementations to their domain interfaces.
 *
 * This module is the cornerstone of the Data Layer in Clean Architecture. It instructs Hilt on how to
 * provide concrete implementations (usually utilizing Firebase or DataStore) whenever a Domain Use Case
 * or ViewModel requests a repository interface.
 *
 * Key features:
 * - **Dependency Inversion:** Decouples the Domain layer from Data layer implementation details.
 * - **Singleton Scope:** Ensures consistent data state and resource efficiency across the app.
 * - **Centralized Binding:** Acts as the single source of truth for repository dependency configuration.
 *
 * The module is installed in [SingletonComponent] to make these bindings available throughout the
 * entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [UserRepositoryImpl] to the [UserRepository] domain interface.
     *
     * Provides access to user-related data, handling Firebase Authentication bridging,
     * user profile caching, and presence management logic.
     *
     * @param userRepositoryImpl The concrete implementation.
     * @return UserRepository interface for domain layer consumption.
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    /**
     * Binds [ChatRepositoryImpl] to the [ChatRepository] domain interface.
     *
     * Manages individual messaging operations, including real-time synchronization
     * via Firebase Realtime Database and media handling via Firebase Storage.
     *
     * @param chatRepositoryImpl The concrete implementation.
     * @return ChatRepository interface for domain layer consumption.
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    /**
     * Binds [ThemeRepositoryImpl] to the [ThemeRepository] domain interface.
     *
     * Handles local persistence of UI preferences (Dark/Light mode, Dynamic Colors)
     * using Jetpack DataStore, exposing changes reactively via Flow.
     *
     * @param themeRepositoryImpl The concrete implementation.
     * @return ThemeRepository interface for domain layer consumption.
     */
    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    /**
     * Binds [GroupChatRepositoryImpl] to the [GroupChatRepository] domain interface.
     *
     * Handles complex group operations such as creation, member management, role administration,
     * and multi-user message synchronization.
     *
     * @param groupChatRepositoryImpl The concrete implementation.
     * @return GroupChatRepository interface for domain layer consumption.
     */
    @Binds
    @Singleton
    abstract fun bindGroupChatRepository(
        groupChatRepositoryImpl: GroupChatRepositoryImpl
    ): GroupChatRepository

    /**
     * Binds [FirebaseAuthenticationService] to the [AuthenticationService] domain interface.
     *
     * This binding provides the infrastructure-level implementation for authentication checks,
     * allowing the domain layer to verify session validity without depending directly on the
     * Firebase SDK.
     *
     * @param impl The concrete Firebase-backed implementation.
     * @return AuthenticationService interface for domain layer consumption.
     */
    @Binds
    // Nota: Si AuthenticationService no tiene estado, @Singleton es opcional pero recomendado por consistencia.
    // Si la implementación mantiene caché, descomenta @Singleton.
    // @Singleton
    abstract fun bindAuthenticationService(
        impl: FirebaseAuthenticationService
    ): AuthenticationService
}
