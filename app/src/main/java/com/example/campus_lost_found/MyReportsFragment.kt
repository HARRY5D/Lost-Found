package com.example.campus_lost_found

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campus_lost_found.adapter.ItemsAdapter
import com.example.campus_lost_found.model.FoundItem
import com.example.campus_lost_found.model.Item
import com.example.campus_lost_found.model.LostItem
import com.example.campus_lost_found.repository.ItemRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.example.campus_lost_found.utils.SupabaseManager
import kotlinx.coroutines.launch

class MyReportsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var tabLayout: TabLayout
    private var noItemsTextView: TextView? = null  // Make it nullable to prevent crashes
    private val itemRepository = ItemRepository()
    private val currentUserId: String
        get() = SupabaseManager.getInstance().getCurrentUser() ?: ""

    private var myLostItems = listOf<LostItem>()
    private var myFoundItems = listOf<FoundItem>()
    private var displayingLostItems = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_items_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d("MyReportsFragment", "Starting onViewCreated")

            // Safe view initialization with null checks
            recyclerView = view.findViewById(R.id.itemsRecyclerView) ?: throw IllegalStateException("RecyclerView not found")
            searchView = view.findViewById(R.id.searchView) ?: throw IllegalStateException("SearchView not found")

            // TabLayout might not exist in fragment_items_list, so make it optional
            tabLayout = view.findViewById(R.id.myReportsTabLayout) ?: let {
                Log.w("MyReportsFragment", "myReportsTabLayout not found, creating programmatically")
                createTabLayoutProgrammatically(view)
            }

            // Make noItemsTextView optional to prevent crashes
            noItemsTextView = view.findViewById(R.id.empty_view)
            if (noItemsTextView == null) {
                Log.w("MyReportsFragment", "empty_view not found in layout")
            }

            setupRecyclerView()
            setupTabLayout()
            setupSearch()
            loadMyItems()

            Log.d("MyReportsFragment", "onViewCreated completed successfully")

        } catch (e: Exception) {
            Log.e("MyReportsFragment", "Error in onViewCreated: ${e.message}", e)
            // Show error to user instead of crashing
            android.widget.Toast.makeText(requireContext(), "Loading My Reports...", android.widget.Toast.LENGTH_SHORT).show()
            // Try to load without tabs
            setupBasicView()
        }
    }

    private fun createTabLayoutProgrammatically(parentView: View): TabLayout {
        val tabLayout = TabLayout(requireContext())
        tabLayout.id = View.generateViewId()

        // Add to the parent layout if possible
        val parent = parentView as? ViewGroup
        parent?.addView(tabLayout, 0) // Add at the top

        return tabLayout
    }

    private fun setupBasicView() {
        try {
            // Simple setup without tabs
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = ItemsAdapter(
                items = mutableListOf(),
                isLostItemsList = true,
                currentUserId = currentUserId,
                onItemClick = { },
                onClaimButtonClick = { }
            )
            loadMyItems()
        } catch (e: Exception) {
            Log.e("MyReportsFragment", "Even basic setup failed: ${e.message}")
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
            Log.e("MyReportsFragment", "Error setting up RecyclerView: ${e.message}")
        }
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("My Lost Items"))
        tabLayout.addTab(tabLayout.newTab().setText("My Found Items"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        displayingLostItems = true
                        updateRecyclerView(myLostItems)
                    }
                    1 -> {
                        displayingLostItems = false
                        updateRecyclerView(myFoundItems)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
            if (displayingLostItems) {
                updateRecyclerView(myLostItems)
            } else {
                updateRecyclerView(myFoundItems)
            }
            return
        }

        if (displayingLostItems) {
            val filteredList = myLostItems.filter { item ->
                item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.location.contains(query, ignoreCase = true)
            }
            updateRecyclerView(filteredList)
        } else {
            val filteredList = myFoundItems.filter { item ->
                item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.location.contains(query, ignoreCase = true)
            }
            updateRecyclerView(filteredList)
        }
    }

    private fun loadMyItems() {
        if (currentUserId.isEmpty()) {
            showLoginRequiredDialog()
            return
        }

        // Load lost items reported by current user
        lifecycleScope.launch {
            itemRepository.getLostItemsByUser(currentUserId,
                onSuccess = { items ->
                    myLostItems = items
                    if (displayingLostItems) {
                        updateRecyclerView(myLostItems)
                    }
                },
                onFailure = { exception ->
                    showErrorDialog("Failed to load your lost items: ${exception.message}")
                }
            )
        }

        // Load found items reported by current user
        lifecycleScope.launch {
            itemRepository.getFoundItemsByUser(currentUserId,
                onSuccess = { items ->
                    myFoundItems = items
                    if (!displayingLostItems) {
                        updateRecyclerView(myFoundItems)
                    }
                },
                onFailure = { exception ->
                    showErrorDialog("Failed to load your found items: ${exception.message}")
                }
            )
        }
    }

    private fun <T : Item> updateRecyclerView(items: List<T>) {
        if (items.isEmpty()) {
            recyclerView.visibility = View.GONE
            noItemsTextView?.let { textView ->
                textView.visibility = View.VISIBLE
                textView.text = if (displayingLostItems) {
                    "You haven't reported any lost items yet"
                } else {
                    "You haven't reported any found items yet"
                }
            } ?: run {
                // If no noItemsTextView, show toast instead
                android.widget.Toast.makeText(
                    requireContext(),
                    if (displayingLostItems) "No lost items reported" else "No found items reported",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        recyclerView.visibility = View.VISIBLE
        noItemsTextView?.visibility = View.GONE

        val adapter = ItemsAdapter(
            items = items.toMutableList(),
            isLostItemsList = displayingLostItems,
            currentUserId = currentUserId,
            onItemClick = { item ->
                showItemOptionsDialog(item)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun showItemOptionsDialog(item: Item) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Item Options")
            .setItems(arrayOf("View Details", "Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showItemDetailsDialog(item)
                    1 -> editItem(item)
                    2 -> showDeleteConfirmationDialog(item)
                }
            }
            .show()
    }

    private fun showItemDetailsDialog(item: Item) {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val message = when (item) {
            is LostItem -> """
                Name: ${item.name}
                Category: ${item.category}
                Location: ${item.location}
                Description: ${item.description}
                Date Lost: ${dateFormat.format(java.util.Date(item.dateLost))}
            """.trimIndent()

            is FoundItem -> {
                val claimStatus = if (item.claimed) {
                    "Claimed by: ${item.claimedByName}"
                } else {
                    "Not claimed"
                }

                """
                Name: ${item.name}
                Category: ${item.category}
                Location: ${item.location}
                Description: ${item.description}
                Date Found: ${dateFormat.format(java.util.Date(item.dateFound))}
                Kept at: ${item.keptAt}
                Status: $claimStatus
                """.trimIndent()
            }

            else -> "Item details unavailable"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Item Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun editItem(item: Item) {
        val intent = when (item) {
            is LostItem -> {
                ReportItemActivity.createEditIntent(
                    context = requireContext(),
                    isLostItem = true,
                    itemId = item.id
                )
            }
            is FoundItem -> {
                ReportItemActivity.createEditIntent(
                    context = requireContext(),
                    isLostItem = false,
                    itemId = item.id
                )
            }
            else -> null
        }

        intent?.let { startActivity(it) }
    }

    private fun showDeleteConfirmationDialog(item: Item) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this item report? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: Item) {
        lifecycleScope.launch {
            when (item) {
                is LostItem -> {
                    itemRepository.deleteLostItem(item.id,
                        onSuccess = {
                            showSuccessDialog("Item deleted successfully")
                            loadMyItems()
                        },
                        onFailure = { exception ->
                            showErrorDialog("Failed to delete item: ${exception.message}")
                        }
                    )
                }
                is FoundItem -> {
                    itemRepository.deleteFoundItem(item.id,
                        onSuccess = {
                            showSuccessDialog("Item deleted successfully")
                            loadMyItems()
                        },
                        onFailure = { exception ->
                            showErrorDialog("Failed to delete item: ${exception.message}")
                        }
                    )
                }
            }
        }
    }

    private fun showLoginRequiredDialog() {
        // Check if fragment is still attached and activity is not finishing
        if (!isAdded || requireActivity().isFinishing || requireActivity().isDestroyed) {
            Log.w("MyReportsFragment", "Cannot show login dialog - fragment not attached or activity finishing")
            return
        }

        try {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Login Required")
                .setMessage("You must be logged in to view your reports.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e("MyReportsFragment", "Error showing login dialog: ${e.message}")
        }
    }

    private fun showSuccessDialog(message: String) {
        // Check if fragment is still attached and activity is not finishing
        if (!isAdded || requireActivity().isFinishing || requireActivity().isDestroyed) {
            Log.w("MyReportsFragment", "Cannot show success dialog - fragment not attached or activity finishing")
            return
        }

        try {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e("MyReportsFragment", "Error showing success dialog: ${e.message}")
            // Fallback to toast
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorDialog(message: String) {
        // Check if fragment is still attached and activity is not finishing
        if (!isAdded || requireActivity().isFinishing || requireActivity().isDestroyed) {
            Log.w("MyReportsFragment", "Cannot show error dialog - fragment not attached or activity finishing")
            return
        }

        try {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e("MyReportsFragment", "Error showing error dialog: ${e.message}")
            // Fallback to toast
            android.widget.Toast.makeText(requireContext(), "An error occurred", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear any references to prevent memory leaks
        noItemsTextView = null
    }

    override fun onResume() {
        super.onResume()
        loadMyItems() // Refresh data when coming back to this fragment
    }
}
