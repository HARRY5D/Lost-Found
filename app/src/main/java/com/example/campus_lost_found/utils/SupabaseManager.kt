package com.example.campus_lost_found.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.campus_lost_found.config.SupabaseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import java.io.InputStream
import java.util.UUID

class SupabaseManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: SupabaseManager? = null

        fun getInstance(): SupabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseManager().also { INSTANCE = it }
            }
        }
    }

    private val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Storage)
        }
    }

    private val storage by lazy { supabaseClient.storage }

    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        userId: String
    ): Result<String> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val imageBytes = inputStream?.readBytes()
            inputStream?.close()

            if (imageBytes == null) {
                return Result.failure(Exception("Failed to read image data"))
            }

            // Generate unique filename
            val fileName = "${userId}_${UUID.randomUUID()}.jpg"

            Log.d("SupabaseManager", "Uploading image: $fileName to bucket: ${SupabaseConfig.STORAGE_BUCKET}")

            // Upload to Supabase Storage with proper error handling
            try {
                storage.from(SupabaseConfig.STORAGE_BUCKET).upload(
                    path = fileName,
                    data = imageBytes,
                    upsert = false // Don't overwrite existing files
                )

                // Get public URL
                val publicUrl = storage.from(SupabaseConfig.STORAGE_BUCKET).publicUrl(fileName)

                Log.d("SupabaseManager", "Image uploaded successfully: $publicUrl")
                Result.success(publicUrl)

            } catch (storageException: Exception) {
                Log.e("SupabaseManager", "Storage upload failed: ${storageException.message}")

                // Provide more specific error messages
                val errorMessage = when {
                    storageException.message?.contains("row-level security") == true ->
                        "Storage permissions not configured. Please check Supabase bucket policies."
                    storageException.message?.contains("bucket") == true ->
                        "Storage bucket 'item-images' not found. Please create it in Supabase dashboard."
                    storageException.message?.contains("auth") == true ->
                        "Authentication failed. Please check Supabase credentials."
                    else -> "Upload failed: ${storageException.message}"
                }

                Result.failure(Exception(errorMessage))
            }

        } catch (e: Exception) {
            Log.e("SupabaseManager", "Failed to upload image: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            // Extract filename from URL
            val fileName = imageUrl.substringAfterLast("/")
            storage.from(SupabaseConfig.STORAGE_BUCKET).delete(fileName)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Failed to delete image: ${e.message}")
            Result.failure(e)
        }
    }
}
