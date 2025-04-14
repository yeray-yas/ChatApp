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
 * Dagger Hilt module that provides repository dependencies for the application.
 *
 * This module is installed in the SingletonComponent, making the provided instances
 * available throughout the entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides a singleton instance of [ChatRepository].
     *
     * @param context The application context.
     * @param auth An instance of [FirebaseAuth] used for authentication.
     * @param database A [DatabaseReference] to the Firebase Realtime Database.
     * @param storage An instance of [FirebaseStorage] for storing media files.
     * @return A configured instance of [ChatRepository].
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