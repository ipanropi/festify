package com.cs407.festify.di

import com.cs407.festify.data.repository.AuthRepository
import com.cs407.festify.data.repository.ChatRepository
import com.cs407.festify.data.repository.EventRepository
import com.cs407.festify.data.repository.FunctionsRepository
import com.cs407.festify.data.repository.StorageRepository
import com.cs407.festify.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing app-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides Firebase Authentication instance
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Provides Firebase Firestore instance
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    /**
     * Provides Firebase Storage instance
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    /**
     * Provides Firebase Functions instance
     */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
    }

    /**
     * Provides AuthRepository
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepository(auth, firestore)
    }

    /**
     * Provides UserRepository
     */
    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): UserRepository {
        return UserRepository(firestore, auth)
    }

    /**
     * Provides EventRepository
     */
    @Provides
    @Singleton
    fun provideEventRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): EventRepository {
        return EventRepository(firestore, auth)
    }

    /**
     * Provides ChatRepository
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): ChatRepository {
        return ChatRepository(firestore, auth)
    }

    /**
     * Provides StorageRepository
     */
    @Provides
    @Singleton
    fun provideStorageRepository(
        storage: FirebaseStorage,
        auth: FirebaseAuth
    ): StorageRepository {
        return StorageRepository(storage, auth)
    }

    /**
     * Provides FunctionsRepository
     */
    @Provides
    @Singleton
    fun provideFunctionsRepository(
        functions: FirebaseFunctions
    ): FunctionsRepository {
        return FunctionsRepository(functions)
    }
}
