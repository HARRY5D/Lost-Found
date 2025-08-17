package com.example.campus_lost_found

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class CampusLostFoundApplication : Application() {

    companion object {
        private const val TAG = "CampusLostFoundApp"
        var isFirebaseInitialized = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        try {
            Log.d(TAG, "Initializing Firebase...")

            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            isFirebaseInitialized = true
            Log.d(TAG, "Firebase initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Critical error initializing Firebase: ${e.message}", e)
            // Don't crash the app - let it continue without Firebase
            isFirebaseInitialized = false
        }

        Log.d(TAG, "Application onCreate completed")
    }
}
