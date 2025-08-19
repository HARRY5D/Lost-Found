package com.example.campus_lost_found

import android.app.Application
import android.util.Log
import com.example.campus_lost_found.config.SupabaseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class CampusLostFoundApplication : Application() {

    companion object {
        private const val TAG = "CampusLostFoundApp"
        var isSupabaseInitialized = false
            private set
            
        lateinit var supabaseClient: io.github.jan.supabase.SupabaseClient
            private set
    }

    override fun onCreate() {
        super.onCreate()

        try {
            Log.d(TAG, "Initializing Supabase...")

            // Initialize Supabase
            supabaseClient = createSupabaseClient(
                supabaseUrl = SupabaseConfig.SUPABASE_URL,
                supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
                install(Storage)
            }

            isSupabaseInitialized = true
            Log.d(TAG, "Supabase initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Critical error initializing Supabase: ${e.message}", e)
            // Don't crash the app - let it continue without Supabase
            isSupabaseInitialized = false
        }

        Log.d(TAG, "Application onCreate completed")
    }
}
