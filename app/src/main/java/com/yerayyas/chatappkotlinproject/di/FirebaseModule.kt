package com.yerayyas.chatappkotlinproject.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Firebase-related dependencies.
 *
 * This module configures and provides all Firebase service instances required by the application,
 * including authentication, real-time database, and cloud storage. All instances are provided
 * as singletons to ensure consistent state management and optimal resource usage.
 *
 * Firebase services provided:
 * - **Firebase Authentication**: User authentication and session management
 * - **Firebase Realtime Database**: Real-time data synchronization for messages and user data
 * - **Firebase Storage**: Cloud storage for images and file uploads
 *
 * The module follows Clean Architecture principles by providing these services through
 * dependency injection, allowing for easier testing and modularity.
 *
 * Installation: SingletonComponent ensures all Firebase instances live throughout
 * the entire application lifecycle for consistent state management.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provides a singleton instance of [FirebaseAuth] for user authentication.
     *
     * This instance handles all authentication operations including:
     * - User sign-in and sign-up processes
     * - Session management and persistence
     * - User state observation and authentication tokens
     * - Password reset and account management
     *
     * @return Singleton FirebaseAuth instance configured for the application
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Provides a singleton instance of [FirebaseDatabase] with named qualifier.
     *
     * This named instance is specifically used by repositories that need access
     * to the database instance itself for advanced operations like configuration
     * or multiple database scenarios.
     *
     * @return Singleton FirebaseDatabase instance for advanced database operations
     */
    @Provides
    @Singleton
    @Named("firebaseDatabaseInstance")
    fun provideFirebaseDatabaseInstance(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    /**
     * Provides the root [DatabaseReference] of the Firebase Realtime Database.
     *
     * This reference serves as the entry point for all database operations including:
     * - Reading and writing user data, messages, and group information
     * - Setting up real-time listeners for live data updates
     * - Managing database queries and transactions
     * - Handling offline capabilities and data synchronization
     *
     * The root reference allows repositories to navigate to specific database paths
     * while maintaining a centralized configuration point.
     *
     * @return DatabaseReference pointing to the root node of the Firebase Realtime Database
     */
    @Provides
    fun provideFirebaseDatabase(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    /**
     * Provides a singleton instance of [FirebaseStorage] for cloud file operations.
     *
     * This instance manages all cloud storage operations including:
     * - Image upload and download for profile pictures and messages
     * - File management with proper access control
     * - Optimized storage buckets for different file types
     * - Secure download URL generation for file access
     *
     * The storage instance is configured with default settings suitable for
     * chat application requirements including image compression and security rules.
     *
     * @return Singleton FirebaseStorage instance for file upload and management operations
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}
