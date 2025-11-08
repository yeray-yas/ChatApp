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
 * Dagger Hilt module that binds repository implementations to their interfaces.
 * Installed in SingletonComponent to provide singletons app-wide.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the UserRepositoryImpl implementation to the UserRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    /**
     * Binds the ChatRepositoryImpl implementation to the ChatRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    /**
     * Binds the ThemeRepositoryImpl implementation to the ThemeRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindGroupChatRepository(
        groupChatRepositoryImpl: GroupChatRepositoryImpl
    ): GroupChatRepository
}
