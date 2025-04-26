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
import com.google.firebase.firestore.FirebaseFirestore

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Proporciona la referencia raíz de Realtime Database.
     * (La dejamos por si se usa en otras partes, pero no para los tokens FCM).
     */
    @Provides
    fun provideFirebaseDatabase(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    /**
     * NUEVO: Proporciona una instancia singleton de [FirebaseFirestore].
     *
     * @return Una instancia de FirebaseFirestore.
     */
    @Provides
    @Singleton // Es bueno que Firestore sea singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
        // Alternativamente, podrías usar la extensión KTX si prefieres:
        // import com.google.firebase.firestore.ktx.firestore
        // import com.google.firebase.ktx.Firebase
        // return Firebase.firestore
    }
}