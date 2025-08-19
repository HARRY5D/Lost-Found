package com.example.campus_lost_found.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Lost item class for Supabase - updated to match database schema
@Serializable
data class SupabaseLostItem(
    var id: String = "",
    var name: String = "",
    var description: String? = null, // Made nullable to match database
    var category: String = "",
    var location: String = "",
    @SerialName("imageurl") var imageUrl: String = "", // Maps to lowercase database column
    @SerialName("reportedby") var reportedBy: String = "", // Maps to lowercase database column
    @SerialName("reportedbyemail") var reportedByEmail: String = "", // Maps to lowercase database column
    @SerialName("reporteddate") var reportedDate: Long = 0L, // Maps to lowercase database column
    @SerialName("datelost") var dateLost: Long = 0L // Maps to lowercase database column
)

// Found item class for Supabase - updated to match database schema
@Serializable
data class SupabaseFoundItem(
    var id: String = "",
    var name: String = "",
    var description: String? = null, // Made nullable to match database
    var category: String = "",
    var location: String = "",
    @SerialName("imageurl") var imageUrl: String = "", // Maps to lowercase database column
    @SerialName("reportedby") var reportedBy: String = "", // Maps to lowercase database column
    @SerialName("reportedbyemail") var reportedByEmail: String = "", // Maps to lowercase database column
    @SerialName("reporteddate") var reportedDate: Long = 0L, // Maps to lowercase database column
    @SerialName("keptat") var keptAt: String = "", // Maps to lowercase database column
    var claimed: Boolean = false,
    @SerialName("claimedby") var claimedBy: String = "", // Maps to lowercase database column
    @SerialName("claimedbyemail") var claimedByEmail: String = "", // Maps to lowercase database column
    @SerialName("datefound") var dateFound: Long = 0L // Maps to lowercase database column
)

// Helper functions for ID generation and date handling
object SupabaseModelUtils {
    fun generateId(): String = java.util.UUID.randomUUID().toString()
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()
}
