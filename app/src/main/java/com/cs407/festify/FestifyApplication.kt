package com.cs407.festify

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Festify app
 * Initializes Firebase and Hilt
 */
@HiltAndroidApp
class FestifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Configure Firestore settings
        configureFirestore()
    }

    private fun configureFirestore() {
        val firestore = FirebaseFirestore.getInstance()

        // Configure Firestore settings for optimal performance
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Unlimited cache
            .build()

        firestore.firestoreSettings = settings
    }
}
