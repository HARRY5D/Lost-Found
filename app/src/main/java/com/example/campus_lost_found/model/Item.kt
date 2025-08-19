package com.example.campus_lost_found.model

import java.util.Date
import java.util.UUID

// Base class for all items using Long timestamps instead of Firebase Timestamp
open class Item(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var description: String = "",
    var category: String = "",
    var location: String = "",
    var imageUrl: String = "",
    var reportedBy: String = "", // User ID
    var reportedByName: String = "",
    var reportedDate: Long = System.currentTimeMillis() // Unix timestamp in milliseconds
)

// Lost item class extending the base Item class
class LostItem(
    id: String = UUID.randomUUID().toString(),
    name: String = "",
    description: String = "",
    category: String = "",
    location: String = "",
    imageUrl: String = "",
    reportedBy: String = "",
    reportedByName: String = "",
    reportedDate: Long = System.currentTimeMillis(),
    var dateLost: Long = System.currentTimeMillis()
) : Item(id, name, description, category, location, imageUrl, reportedBy, reportedByName, reportedDate)

// Found item class extending the base Item class
class FoundItem(
    id: String = UUID.randomUUID().toString(),
    name: String = "",
    description: String = "",
    category: String = "",
    location: String = "",
    imageUrl: String = "",
    reportedBy: String = "",
    reportedByName: String = "",
    reportedDate: Long = System.currentTimeMillis(),
    var keptAt: String = "",
    var claimed: Boolean = false,
    var claimedBy: String = "",
    var claimedByName: String = "",
    var dateFound: Long = System.currentTimeMillis()
) : Item(id, name, description, category, location, imageUrl, reportedBy, reportedByName, reportedDate)
