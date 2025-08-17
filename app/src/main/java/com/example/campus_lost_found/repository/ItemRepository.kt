package com.example.campus_lost_found.repository

import com.example.campus_lost_found.model.FoundItem
import com.example.campus_lost_found.model.LostItem
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class ItemRepository {
    private val db = FirebaseFirestore.getInstance()
    private val lostItemsCollection = db.collection("lostItems")
    private val foundItemsCollection = db.collection("foundItems")
    private val usersCollection = db.collection("users")

    // Lost Items
    fun addLostItem(lostItem: LostItem): Task<Void> {
        val documentRef = lostItemsCollection.document()
        lostItem.id = documentRef.id
        return documentRef.set(lostItem)
            .addOnSuccessListener {
                // Update user's lost reports
                usersCollection.document(lostItem.reportedBy)
                    .update("lostReports", com.google.firebase.firestore.FieldValue.arrayUnion(lostItem.id))
            }
    }

    fun getLostItems(): Query {
        return lostItemsCollection.orderBy("reportedDate", Query.Direction.DESCENDING)
    }

    fun getLostItemsByUser(userId: String): Query {
        return lostItemsCollection.whereEqualTo("reportedBy", userId)
            .orderBy("reportedDate", Query.Direction.DESCENDING)
    }

    fun getLostItem(itemId: String): Task<DocumentSnapshot> {
        return lostItemsCollection.document(itemId).get()
    }

    fun updateLostItem(lostItem: LostItem): Task<Void> {
        return lostItemsCollection.document(lostItem.id).set(lostItem)
    }

    fun deleteLostItem(itemId: String, userId: String): Task<Void> {
        return lostItemsCollection.document(itemId).delete()
            .addOnSuccessListener {
                // Remove from user's lost reports
                usersCollection.document(userId)
                    .update("lostReports", com.google.firebase.firestore.FieldValue.arrayRemove(itemId))
            }
    }

    // Found Items
    fun addFoundItem(foundItem: FoundItem): Task<Void> {
        val documentRef = foundItemsCollection.document()
        foundItem.id = documentRef.id
        return documentRef.set(foundItem)
            .addOnSuccessListener {
                // Update user's found reports
                usersCollection.document(foundItem.reportedBy)
                    .update("foundReports", com.google.firebase.firestore.FieldValue.arrayUnion(foundItem.id))
            }
    }

    fun getFoundItems(): Query {
        return foundItemsCollection.orderBy("reportedDate", Query.Direction.DESCENDING)
    }

    fun getFoundItemsByUser(userId: String): Query {
        return foundItemsCollection.whereEqualTo("reportedBy", userId)
            .orderBy("reportedDate", Query.Direction.DESCENDING)
    }

    fun getFoundItem(itemId: String): Task<DocumentSnapshot> {
        return foundItemsCollection.document(itemId).get()
    }

    fun updateFoundItem(foundItem: FoundItem): Task<Void> {
        return foundItemsCollection.document(foundItem.id).set(foundItem)
    }

    fun deleteFoundItem(itemId: String, userId: String): Task<Void> {
        return foundItemsCollection.document(itemId).delete()
            .addOnSuccessListener {
                // Remove from user's found reports
                usersCollection.document(userId)
                    .update("foundReports", com.google.firebase.firestore.FieldValue.arrayRemove(itemId))
            }
    }

    fun claimItem(foundItemId: String, userId: String, userName: String): Task<Void> {
        return foundItemsCollection.document(foundItemId)
            .update(
                mapOf(
                    "claimed" to true,
                    "claimedBy" to userId,
                    "claimedByName" to userName
                )
            )
    }

    // Search functionality
    fun searchLostItems(query: String): Task<QuerySnapshot> {
        return lostItemsCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .get()
    }

    fun searchFoundItems(query: String): Task<QuerySnapshot> {
        return foundItemsCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .get()
    }
}
