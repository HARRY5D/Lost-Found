package com.example.campus_lost_found.repository

import android.util.Log
import com.example.campus_lost_found.model.SupabaseFoundItem
import com.example.campus_lost_found.model.SupabaseLostItem
import com.example.campus_lost_found.utils.SupabaseManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import com.example.campus_lost_found.config.SupabaseConfig

class SupabaseRepository {

    private val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }
    }

    private val postgrest by lazy { supabaseClient.postgrest }

    // Lost Items Operations
    suspend fun addLostItem(lostItem: SupabaseLostItem): Result<SupabaseLostItem> {
        return try {
            val result = postgrest["lost_items"].insert(lostItem)
            Log.d("SupabaseRepo", "Lost item added successfully: ${lostItem.id}")
            Result.success(lostItem)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to add lost item: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getLostItems(): Result<List<SupabaseLostItem>> {
        return try {
            val items = postgrest["lost_items"]
                .select()
                .decodeList<SupabaseLostItem>()
            Log.d("SupabaseRepo", "Retrieved ${items.size} lost items")
            Result.success(items)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to get lost items: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getLostItemsByUser(userId: String): Result<List<SupabaseLostItem>> {
        return try {
            val items = postgrest["lost_items"]
                .select() {
                    filter {
                        eq("reportedby", userId) // Changed to lowercase to match database
                    }
                }
                .decodeList<SupabaseLostItem>()
            Log.d("SupabaseRepo", "Retrieved ${items.size} lost items for user: $userId")
            Result.success(items)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to get user's lost items: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateLostItem(lostItem: SupabaseLostItem): Result<SupabaseLostItem> {
        return try {
            postgrest["lost_items"].update(lostItem) {
                filter {
                    eq("id", lostItem.id)
                }
            }
            Log.d("SupabaseRepo", "Lost item updated successfully: ${lostItem.id}")
            Result.success(lostItem)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to update lost item: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteLostItem(itemId: String): Result<Unit> {
        return try {
            postgrest["lost_items"].delete {
                filter {
                    eq("id", itemId)
                }
            }
            Log.d("SupabaseRepo", "Lost item deleted successfully: $itemId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to delete lost item: ${e.message}")
            Result.failure(e)
        }
    }

    // Found Items Operations
    suspend fun addFoundItem(foundItem: SupabaseFoundItem): Result<SupabaseFoundItem> {
        return try {
            postgrest["found_items"].insert(foundItem)
            Log.d("SupabaseRepo", "Found item added successfully: ${foundItem.id}")
            Result.success(foundItem)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to add found item: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getFoundItems(): Result<List<SupabaseFoundItem>> {
        return try {
            val items = postgrest["found_items"]
                .select()
                .decodeList<SupabaseFoundItem>()
            Log.d("SupabaseRepo", "Retrieved ${items.size} found items")
            Result.success(items)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to get found items: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getFoundItemsByUser(userId: String): Result<List<SupabaseFoundItem>> {
        return try {
            val items = postgrest["found_items"]
                .select() {
                    filter {
                        eq("reportedby", userId) // Changed to lowercase to match database
                    }
                }
                .decodeList<SupabaseFoundItem>()
            Log.d("SupabaseRepo", "Retrieved ${items.size} found items for user: $userId")
            Result.success(items)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to get user's found items: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateFoundItem(foundItem: SupabaseFoundItem): Result<SupabaseFoundItem> {
        return try {
            postgrest["found_items"].update(foundItem) {
                filter {
                    eq("id", foundItem.id)
                }
            }
            Log.d("SupabaseRepo", "Found item updated successfully: ${foundItem.id}")
            Result.success(foundItem)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to update found item: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteFoundItem(itemId: String): Result<Unit> {
        return try {
            postgrest["found_items"].delete {
                filter {
                    eq("id", itemId)
                }
            }
            Log.d("SupabaseRepo", "Found item deleted successfully: $itemId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to delete found item: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun claimFoundItem(itemId: String, claimedBy: String, claimedByEmail: String): Result<Unit> {
        return try {
            postgrest["found_items"].update(
                mapOf(
                    "claimed" to true,
                    "claimedby" to claimedBy, // Changed to lowercase to match database
                    "claimedbyemail" to claimedByEmail // Changed to lowercase to match database
                )
            ) {
                filter {
                    eq("id", itemId)
                }
            }
            Log.d("SupabaseRepo", "Item claimed successfully: $itemId by $claimedByEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupabaseRepo", "Failed to claim item: ${e.message}")
            Result.failure(e)
        }
    }
}
