package com.example.campus_lost_found

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campus_lost_found.adapter.ItemsAdapter
import com.example.campus_lost_found.model.FoundItem
import com.example.campus_lost_found.repository.ItemRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class FoundItemsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private val itemRepository = ItemRepository()
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val currentUserName: String
        get() = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous User"

    private var foundItems = listOf<FoundItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_items_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Safe view initialization with null checks
            recyclerView = view.findViewById(R.id.itemsRecyclerView) ?: throw IllegalStateException("RecyclerView not found")
            searchView = view.findViewById(R.id.searchView) ?: throw IllegalStateException("SearchView not found")

            setupRecyclerView()
            setupSearch()
            loadFoundItems()

        } catch (e: Exception) {
            Log.e("FoundItemsFragment", "Error in onViewCreated: ${e.message}")
            // Show error to user instead of crashing
            android.widget.Toast.makeText(requireContext(), "Error loading fragment", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        try {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.setHasFixedSize(true)

            // Set empty adapter initially to prevent crashes
            recyclerView.adapter = ItemsAdapter(
                items = mutableListOf(),
                isLostItemsList = false,
                currentUserId = currentUserId,
                onItemClick = { },
                onClaimButtonClick = { }
            )
        } catch (e: Exception) {
            Log.e("FoundItemsFragment", "Error setting up RecyclerView: ${e.message}")
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterItems(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterItems(newText)
                return false
            }
        })
    }

    private fun filterItems(query: String?) {
        if (query.isNullOrBlank()) {
            updateRecyclerView(foundItems)
            return
        }

        val filteredList = foundItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true) ||
            item.category.contains(query, ignoreCase = true) ||
            item.location.contains(query, ignoreCase = true) ||
            item.keptAt.contains(query, ignoreCase = true)
        }

        updateRecyclerView(filteredList)
    }

    private fun loadFoundItems() {
        Log.d("FoundItemsFragment", "Loading found items from all users...")

        itemRepository.getFoundItems().get()
            .addOnSuccessListener { snapshot ->
                Log.d("FoundItemsFragment", "Successfully loaded ${snapshot.size()} found items")
                val items = snapshot.toObjects(FoundItem::class.java)

                // Debug: Log each item to see what's being loaded
                items.forEachIndexed { index, item ->
                    Log.d("FoundItemsFragment", "Item $index: ${item.name} by ${item.reportedByName} (${item.reportedBy})")
                }

                foundItems = items
                updateRecyclerView(items)

                // Show empty state if no items
                if (items.isEmpty()) {
                    showEmptyState("No found items yet. Help by reporting found items!")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FoundItemsFragment", "Failed to load found items: ${exception.message}")
                showErrorDialog("Failed to load found items: ${exception.message}")
                showEmptyState("Failed to load items. Please check your connection.")
            }
    }

    private fun updateRecyclerView(items: List<FoundItem>) {
        val adapter = ItemsAdapter(
            items = items.toMutableList(),
            isLostItemsList = false,
            currentUserId = currentUserId,
            onItemClick = { item ->
                // Show item details
                showItemDetailsDialog(item as FoundItem)
            },
            onClaimButtonClick = { item ->
                // Show claim confirmation dialog
                showClaimConfirmationDialog(item as FoundItem)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun showItemDetailsDialog(item: FoundItem) {
        val claimStatus = if (item.claimed) {
            "Claimed by: ${item.claimedByName}"
        } else {
            "Not claimed"
        }

        val message = """
            Name: ${item.name}
            Category: ${item.category}
            Location: ${item.location}
            Description: ${item.description}
            Date Found: ${item.dateFound.toDate()}
            Kept at: ${item.keptAt}
            Status: $claimStatus
            Reported by: ${item.reportedByName}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Item Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showClaimConfirmationDialog(item: FoundItem) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_claim_item, null)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Claim This Item")
            .setView(dialogView)
            .setMessage("Are you sure you want to claim this item? The finder will be notified of your claim.")
            .setPositiveButton("Claim") { _, _ ->
                processClaimRequest(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processClaimRequest(item: FoundItem) {
        if (currentUserId.isEmpty()) {
            showErrorDialog("You must be logged in to claim items.")
            return
        }

        itemRepository.claimItem(item.id, currentUserId, currentUserName)
            .addOnSuccessListener {
                showSuccessDialog("Claim request sent successfully. The finder will be notified.")
                loadFoundItems() // Refresh the list
            }
            .addOnFailureListener { exception ->
                showErrorDialog("Failed to claim item: ${exception.message}")
            }
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEmptyState(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        loadFoundItems() // Refresh data when coming back to this fragment
    }
}
