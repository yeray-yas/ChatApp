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
import com.yerayyas.chatappkotlinproject.data.repository.UserRepositoryImpl
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import dagger.Binds

/**
 * Dagger Hilt module that provides repository dependencies for the application.
 *
 * This module is installed in the SingletonComponent, making the provided instances
 * available throughout the entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
// Cambiado a 'abstract class' para permitir @Binds
abstract class RepositoryModule {

    // --- Usamos @Binds para UserRepository (más eficiente para bindings simples) ---
    /**
     * Binds the UserRepositoryImpl implementation to the UserRepository interface.
     * Hilt will provide UserRepositoryImpl whenever UserRepository is requested.
     */
    @Binds
    @Singleton // Asegura que sea singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl // Hilt sabe cómo construir UserRepositoryImpl porque está anotada con @Inject
    ): UserRepository


    // --- Mantenemos @Provides para ChatRepository si su construcción es más compleja ---
    // O si prefieres ser explícito sobre sus dependencias aquí.
    // Necesitamos un 'companion object' para alojar las funciones @Provides dentro de una clase abstracta.
    companion object {
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
            auth: FirebaseAuth, // Hilt debe saber cómo proporcionar estas dependencias (probablemente desde otro módulo como FirebaseModule)
            database: DatabaseReference,
            storage: FirebaseStorage
        ): ChatRepository {
            // Aquí sí necesitas construirlo manualmente porque ChatRepository no tenía @Inject en su constructor
            return ChatRepository(context, auth, database, storage)
        }
    }
}