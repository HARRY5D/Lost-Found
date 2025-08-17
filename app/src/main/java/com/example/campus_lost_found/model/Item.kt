package com.example.campus_lost_found.model

import com.google.firebase.Timestamp
import java.util.Date

// Base class for all items
open class Item(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var category: String = "",
    var location: String = "",
    var imageUrl: String = "",
    var reportedBy: String = "", // User ID
    var reportedByName: String = "",
    var reportedDate: Timestamp = Timestamp.now()
)

// Lost item class extending the base Item class
class LostItem(
    id: String = "",
    name: String = "",
    description: String = "",
    category: String = "",
    location: String = "",
    imageUrl: String = "",
    reportedBy: String = "",
    reportedByName: String = "",
    reportedDate: Timestamp = Timestamp.now(),
    var dateLost: Timestamp = Timestamp.now()
) : Item(id, name, description, category, location, imageUrl, reportedBy, reportedByName, reportedDate)

// Found item class extending the base Item class
class FoundItem(
    id: String = "",
    name: String = "",
    description: String = "",
    category: String = "",
    location: String = "",
    imageUrl: String = "",
    reportedBy: String = "",
    reportedByName: String = "",
    reportedDate: Timestamp = Timestamp.now(),
    var keptAt: String = "",
    var claimed: Boolean = false,
    var claimedBy: String = "",
    var claimedByName: String = "",
    var dateFound: Timestamp = Timestamp.now()
) : Item(id, name, description, category, location, imageUrl, reportedBy, reportedByName, reportedDate)
