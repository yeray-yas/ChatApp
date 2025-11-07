package com.yerayyas.chatappkotlinproject.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Firebase-related dependencies.
 *
 * This module installs bindings into the SingletonComponent,
 * meaning the provided instances will live as long as the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provides a singleton instance of [FirebaseAuth].
     *
     * @return An instance of FirebaseAuth.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Provides the root [DatabaseReference] of the Firebase Realtime Database.
     *
     * This reference can be used to read/write to the database.
     *
     * @return A DatabaseReference pointing to the root node.
     */
    @Provides
    fun provideFirebaseDatabase(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    /**
     * Provides a singleton instance of [FirebaseStorage].
     *
     * @return An instance of FirebaseStorage for uploading and downloading files.
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}
