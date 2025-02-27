package com.yerayyas.chatappkotlinproject.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.repository.ChatRepositoryImpl
import com.yerayyas.chatappkotlinproject.domain.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        firebaseAuth: FirebaseAuth,
        database: DatabaseReference,
        firebaseStorage: FirebaseStorage
    ): ChatRepository {
        return ChatRepositoryImpl(firebaseAuth, database, firebaseStorage)
    }
}
