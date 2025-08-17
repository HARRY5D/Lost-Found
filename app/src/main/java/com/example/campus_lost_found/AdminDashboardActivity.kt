package com.example.campus_lost_found

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campus_lost_found.adapter.AdminItemsAdapter
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var statsCard: MaterialCardView
    private lateinit var totalLostItems: TextView
    private lateinit var totalFoundItems: TextView
    private lateinit var totalClaims: TextView
    private lateinit var pendingClaims: TextView
    private lateinit var refreshFab: FloatingActionButton

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: AdminItemsAdapter
    private var currentItemType = "claims"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize views
        toolbar = findViewById(R.id.admin_toolbar)
        tabLayout = findViewById(R.id.admin_tab_layout)
        recyclerView = findViewById(R.id.admin_recycler_view)
        emptyView = findViewById(R.id.admin_empty_view)
        statsCard = findViewById(R.id.stats_card)
        totalLostItems = findViewById(R.id.total_lost_items)
        totalFoundItems = findViewById(R.id.total_found_items)
        totalClaims = findViewById(R.id.total_claims)
        pendingClaims = findViewById(R.id.pending_claims)
        refreshFab = findViewById(R.id.admin_refresh_fab)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Admin Dashboard"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup tabs
        setupTabs()

        // Setup RecyclerView with adapter
        setupRecyclerView()

        // Load data
        loadStatistics()
        loadReports(currentItemType)

        // Setup refresh button
        refreshFab.setOnClickListener {
            refreshData()
        }
    }

    private fun setupTabs() {
        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Claims"))
        tabLayout.addTab(tabLayout.newTab().setText("Lost Items"))
        tabLayout.addTab(tabLayout.newTab().setText("Found Items"))

        // Handle tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentItemType = when (tab.position) {
                    0 -> "claims"
                    1 -> "lost"
                    2 -> "found"
                    else -> "claims"
                }
                loadReports(currentItemType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty list
        adapter = AdminItemsAdapter(
            itemType = currentItemType,
            onItemClicked = { item -> showItemDetails(item) },
            onApproveClicked = { item -> approveClaim(item) },
            onRejectClicked = { item -> rejectClaim(item) }
        )

        recyclerView.adapter = adapter
    }

    private fun loadStatistics() {
        // Show loading
        statsCard.alpha = 0.5f

        // Count lost items
        firestore.collection("lostItems")
            .get()
            .addOnSuccessListener { documents ->
                totalLostItems.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load lost items: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Count found items
        firestore.collection("foundItems")
            .get()
            .addOnSuccessListener { documents ->
                totalFoundItems.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load found items: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Count all claims
        firestore.collection("claims")
            .get()
            .addOnSuccessListener { documents ->
                totalClaims.text = documents.size().toString()

                // Count pending claims
                val pending = documents.count { doc ->
                    doc.getBoolean("approved") == false && doc.getBoolean("rejected") == false
                }
                pendingClaims.text = pending.toString()

                // Show stats
                statsCard.alpha = 1.0f
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load claims: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadReports(type: String) {
        // Show loading
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = "Loading..."

        // Determine which collection to query
        val collection = when (type) {
            "claims" -> "claims"
            "lost" -> "lostItems"
            "found" -> "foundItems"
            else -> "claims"
        }

        // Query Firestore
        firestore.collection(collection)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    emptyView.text = "No ${type} items found"
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    // Update adapter with new data
                    adapter.updateItems(documents.documents)
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                emptyView.text = "Error loading data: ${e.message}"
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }

    private fun showItemDetails(item: DocumentSnapshot) {
        val itemId = item.id
        val itemName = item.getString("name") ?: "Unknown Item"
        Toast.makeText(this, "Showing details for $itemName", Toast.LENGTH_SHORT).show()

        // This would typically open a detailed view of the item
        // For now just show a simple dialog with the item ID
        MaterialAlertDialogBuilder(this)
            .setTitle("Item Details")
            .setMessage("Item ID: $itemId\nName: $itemName")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun approveClaim(item: DocumentSnapshot) {
        val itemId = item.id

        // Update the claim status in Firestore
        firestore.collection("claims")
            .document(itemId)
            .update(
                mapOf(
                    "approved" to true,
                    "rejected" to false,
                    "approvedDate" to com.google.firebase.Timestamp.now(),
                    "approvedBy" to auth.currentUser?.email
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Claim approved successfully", Toast.LENGTH_SHORT).show()
                loadReports(currentItemType)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to approve claim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectClaim(item: DocumentSnapshot) {
        val itemId = item.id

        // Update the claim status in Firestore
        firestore.collection("claims")
            .document(itemId)
            .update(
                mapOf(
                    "approved" to false,
                    "rejected" to true,
                    "rejectedDate" to com.google.firebase.Timestamp.now(),
                    "rejectedBy" to auth.currentUser?.email
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Claim rejected", Toast.LENGTH_SHORT).show()
                loadReports(currentItemType)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to reject claim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshData() {
        loadStatistics()
        loadReports(currentItemType)
        Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)

        // Update the theme toggle icon based on current night mode
        val nightModeOn = isNightModeEnabled()
        menu.findItem(R.id.menu_toggle_theme).setIcon(
            if (nightModeOn) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
        )

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_logout -> {
                showLogoutConfirmation()
                true
            }
            R.id.menu_toggle_theme -> {
                toggleDarkTheme()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isNightModeEnabled(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun toggleDarkTheme() {
        val newMode = if (isNightModeEnabled()) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }

        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Logout
                auth.signOut()
                // Return to login screen
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
