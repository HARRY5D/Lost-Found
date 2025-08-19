package com.example.campus_lost_found.repository

import com.example.campus_lost_found.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class ItemRepository {

    private val supabaseRepository = SupabaseRepository()

    // Convert old Item models to Supabase models and vice versa
    private fun LostItem.toSupabaseLostItem(): SupabaseLostItem {
        return SupabaseLostItem(
            id = if (this.id.isBlank()) java.util.UUID.randomUUID().toString() else this.id,
            name = this.name,
            description = if (this.description.isBlank()) null else this.description, // Handle nullable description
            category = this.category,
            location = this.location,
            imageUrl = this.imageUrl,
            reportedBy = this.reportedBy,
            reportedByEmail = this.reportedByName, // Using name as email for compatibility
            reportedDate = this.reportedDate,
            dateLost = this.dateLost
        )
    }

    private fun SupabaseLostItem.toLostItem(): LostItem {
        return LostItem(
            id = this.id,
            name = this.name,
            description = this.description ?: "", // Handle nullable description from database
            category = this.category,
            location = this.location,
            imageUrl = this.imageUrl,
            reportedBy = this.reportedBy,
            reportedByName = this.reportedByEmail,
            reportedDate = this.reportedDate,
            dateLost = this.dateLost
        )
    }

    private fun FoundItem.toSupabaseFoundItem(): SupabaseFoundItem {
        return SupabaseFoundItem(
            id = if (this.id.isBlank()) java.util.UUID.randomUUID().toString() else this.id,
            name = this.name,
            description = if (this.description.isBlank()) null else this.description, // Handle nullable description
            category = this.category,
            location = this.location,
            imageUrl = this.imageUrl,
            reportedBy = this.reportedBy,
            reportedByEmail = this.reportedByName,
            reportedDate = this.reportedDate,
            keptAt = this.keptAt,
            claimed = this.claimed,
            claimedBy = this.claimedBy,
            claimedByEmail = this.claimedByName,
            dateFound = this.dateFound
        )
    }

    private fun SupabaseFoundItem.toFoundItem(): FoundItem {
        return FoundItem(
            id = this.id,
            name = this.name,
            description = this.description ?: "", // Handle nullable description from database
            category = this.category,
            location = this.location,
            imageUrl = this.imageUrl,
            reportedBy = this.reportedBy,
            reportedByName = this.reportedByEmail,
            reportedDate = this.reportedDate,
            keptAt = this.keptAt,
            claimed = this.claimed,
            claimedBy = this.claimedBy,
            claimedByName = this.claimedByEmail,
            dateFound = this.dateFound
        )
    }

    // Lost Items operations
    suspend fun addLostItem(item: LostItem, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.addLostItem(item.toSupabaseLostItem())
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun getLostItems(onSuccess: (List<LostItem>) -> Unit, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.getLostItems()
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { items -> onSuccess(items.map { it.toLostItem() }) },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun getLostItemsByUser(userId: String, onSuccess: (List<LostItem>) -> Unit, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.getLostItemsByUser(userId)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { items -> onSuccess(items.map { it.toLostItem() }) },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun updateLostItem(item: LostItem, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.updateLostItem(item.toSupabaseLostItem())
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun deleteLostItem(itemId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.deleteLostItem(itemId)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    // Found Items operations
    suspend fun addFoundItem(item: FoundItem, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.addFoundItem(item.toSupabaseFoundItem())
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun getFoundItems(onSuccess: (List<FoundItem>) -> Unit, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.getFoundItems()
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { items -> onSuccess(items.map { it.toFoundItem() }) },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun getFoundItemsByUser(userId: String, onSuccess: (List<FoundItem>) -> Unit, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.getFoundItemsByUser(userId)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { items -> onSuccess(items.map { it.toFoundItem() }) },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun updateFoundItem(item: FoundItem, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.updateFoundItem(item.toSupabaseFoundItem())
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }

    suspend fun deleteFoundItem(itemId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val result = supabaseRepository.deleteFoundItem(itemId)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onFailure(it as Exception) }
                )
            }
        }
    }
}
