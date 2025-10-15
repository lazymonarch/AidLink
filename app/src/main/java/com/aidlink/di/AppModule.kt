package com.aidlink.di

import com.aidlink.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val EMULATOR_HOST = "192.168.1.3"

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        if (BuildConfig.DEBUG) {
            auth.useEmulator(EMULATOR_HOST, 9099)
        }
        return auth
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val storage = FirebaseStorage.getInstance()
        if (BuildConfig.DEBUG) {
            storage.useEmulator(EMULATOR_HOST, 9199)
        }
        return storage
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        if (BuildConfig.DEBUG) {
            db.useEmulator(EMULATOR_HOST, 8080)
        }
        return db
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        db: FirebaseFirestore,
        storage: FirebaseStorage
    ): AuthRepository = AuthRepository(auth, db, storage)
}