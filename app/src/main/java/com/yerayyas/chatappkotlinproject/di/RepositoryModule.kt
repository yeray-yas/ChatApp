package com.yerayyas.chatappkotlinproject.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Dagger Hilt para proporcionar instancias de repositorios.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Proporciona una instancia singleton de ChatRepository.
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        database: DatabaseReference,
        storage: FirebaseStorage
    ): ChatRepository {
        return ChatRepository(context, auth, database, storage)
    }
} 