package com.yerayyas.chatappkotlinproject.di

import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.ThemeRepository
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.data.repository.ChatRepositoryImpl
import com.yerayyas.chatappkotlinproject.data.repository.ThemeRepositoryImpl
import com.yerayyas.chatappkotlinproject.data.repository.UserRepositoryImpl
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.data.repository.GroupChatRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module that binds repository implementations to their domain interfaces.
 *
 * This module follows the Repository Pattern and Clean Architecture principles by providing
 * the concrete data layer implementations for all domain repository contracts. It ensures
 * proper dependency injection throughout the application while maintaining separation
 * between domain and data layers.
 *
 * Repository responsibilities:
 * - **UserRepository**: User management, authentication, and FCM token handling
 * - **ChatRepository**: Individual chat messaging, image uploads, and message tracking
 * - **GroupChatRepository**: Group chat management, member administration, and group messaging
 * - **ThemeRepository**: Application theme preferences and dynamic color management
 *
 * All repositories are provided as singletons to ensure:
 * - Consistent data state across the application
 * - Optimal memory usage and performance
 * - Single point of truth for data operations
 * - Proper lifecycle management throughout app execution
 *
 * The module is installed in SingletonComponent to provide application-wide availability
 * and ensure all ViewModels and Use Cases receive the same repository instances.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [UserRepositoryImpl] to the [UserRepository] domain interface.
     *
     * This repository handles all user-related operations including:
     * - Firebase Authentication integration and user session management
     * - FCM token management for push notifications
     * - User profile data retrieval and caching
     * - User search and contact management
     * - Online status tracking and user presence
     *
     * @param userRepositoryImpl The concrete implementation with Firebase integration
     * @return UserRepository interface for domain layer consumption
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    /**
     * Binds [ChatRepositoryImpl] to the [ChatRepository] domain interface.
     *
     * This repository manages individual chat operations including:
     * - Text and image message sending with Firebase Storage integration
     * - Real-time message streaming through Firebase Realtime Database
     * - Message read status tracking and delivery confirmations
     * - Reply functionality with context preservation
     * - Chat history management and message persistence
     *
     * @param chatRepositoryImpl The concrete implementation with Firebase integration
     * @return ChatRepository interface for domain layer consumption
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    /**
     * Binds [ThemeRepositoryImpl] to the [ThemeRepository] domain interface.
     *
     * This repository manages application theming and user preferences including:
     * - Theme mode persistence (Light, Dark, System) using DataStore
     * - Dynamic color (Material You) preference management
     * - Reactive theme updates through Flow for immediate UI response
     * - Type-safe preference storage with automatic migration
     *
     * @param themeRepositoryImpl The concrete implementation with DataStore integration
     * @return ThemeRepository interface for domain layer consumption
     */
    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    /**
     * Binds [GroupChatRepositoryImpl] to the [GroupChatRepository] domain interface.
     *
     * This repository handles comprehensive group chat functionality including:
     * - Group creation, management, and configuration
     * - Member administration with role-based permissions (admin/member)
     * - Group messaging with advanced features (reactions, mentions, replies)
     * - Read receipt tracking across multiple group members
     * - Group invitation system with status management
     * - File upload support for group images and message attachments
     * - Real-time group data synchronization
     * - Mock data support for development and testing scenarios
     *
     * @param groupChatRepositoryImpl The concrete implementation with Firebase integration
     * @return GroupChatRepository interface for domain layer consumption
     */
    @Binds
    @Singleton
    abstract fun bindGroupChatRepository(
        groupChatRepositoryImpl: GroupChatRepositoryImpl
    ): GroupChatRepository
}
