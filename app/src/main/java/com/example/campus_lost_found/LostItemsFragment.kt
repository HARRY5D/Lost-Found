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
import com.example.campus_lost_found.model.LostItem
import com.example.campus_lost_found.repository.ItemRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class LostItemsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private val itemRepository = ItemRepository()
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var lostItems = listOf<LostItem>()

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
            loadLostItems()

        } catch (e: Exception) {
            Log.e("LostItemsFragment", "Error in onViewCreated: ${e.message}")
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
                isLostItemsList = true,
                currentUserId = currentUserId,
                onItemClick = { },
                onClaimButtonClick = { }
            )
        } catch (e: Exception) {
            Log.e("LostItemsFragment", "Error setting up RecyclerView: ${e.message}")
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
            updateRecyclerView(lostItems)
            return
        }

        val filteredList = lostItems.filter { item ->
            item.name.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true) ||
            item.category.contains(query, ignoreCase = true) ||
            item.location.contains(query, ignoreCase = true)
        }

        updateRecyclerView(filteredList)
    }

    private fun loadLostItems() {
        Log.d("LostItemsFragment", "Loading lost items from all users...")

        itemRepository.getLostItems().get()
            .addOnSuccessListener { snapshot ->
                Log.d("LostItemsFragment", "Successfully loaded ${snapshot.size()} lost items")
                val items = snapshot.toObjects(LostItem::class.java)

                // Debug: Log each item to see what's being loaded
                items.forEachIndexed { index, item ->
                    Log.d("LostItemsFragment", "Item $index: ${item.name} by ${item.reportedByName} (${item.reportedBy})")
                }

                lostItems = items
                updateRecyclerView(items)

                // Show empty state if no items
                if (items.isEmpty()) {
                    showEmptyState("No lost items found. Be the first to report!")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LostItemsFragment", "Failed to load lost items: ${exception.message}")
                showErrorDialog("Failed to load lost items: ${exception.message}")
                showEmptyState("Failed to load items. Please check your connection.")
            }
    }

    private fun updateRecyclerView(items: List<LostItem>) {
        val adapter = ItemsAdapter(
            items = items.toMutableList(),
            isLostItemsList = true,
            currentUserId = currentUserId,
            onItemClick = { item ->
                showItemDetailsDialog(item as LostItem)
            },
            onClaimButtonClick = { item ->
                handleClaimRequest(item as LostItem)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun showItemDetailsDialog(item: LostItem) {
        val message = """
            Name: ${item.name}
            Category: ${item.category}
            Location: ${item.location}
            Description: ${item.description}
            Date Lost: ${item.dateLost.toDate()}
            Reported by: ${item.reportedByName}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Item Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun handleClaimRequest(item: LostItem) {
        if (currentUserId.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Authentication Required")
                .setMessage("Please sign in to contact the reporter.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Contact Reporter")
            .setMessage("Do you think this item belongs to you? Contact the person who reported it lost.")
            .setPositiveButton("Contact") { _, _ ->
                showContactInfoDialog(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showContactInfoDialog(item: LostItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Contact Information")
            .setMessage("Reporter: ${item.reportedByName}\n\nNote: In a full implementation, this would show contact details or open an in-app messaging system.")
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
        // You can add an empty state view here if needed
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        loadLostItems() // Refresh data when coming back to this fragment
    }
}
