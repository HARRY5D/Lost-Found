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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campus_lost_found.adapter.AdminItemsAdapter
import com.example.campus_lost_found.config.SupabaseConfig
import com.example.campus_lost_found.model.SupabaseFoundItem
import com.example.campus_lost_found.model.SupabaseLostItem
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

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

    private val supabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }
    }

    private lateinit var adapter: AdminItemsAdapter
    private var currentItemType = "found_items"

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
        tabLayout.addTab(tabLayout.newTab().setText("Found Items"))
        tabLayout.addTab(tabLayout.newTab().setText("Lost Items"))

        // Handle tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentItemType = when (tab.position) {
                    0 -> "found_items"
                    1 -> "lost_items"
                    else -> "found_items"
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
            onApproveClicked = { item -> markAsFound(item) },
            onRejectClicked = { item -> deleteItem(item) }
        )

        recyclerView.adapter = adapter
    }

    private fun loadStatistics() {
        // Show loading
        statsCard.alpha = 0.5f

        lifecycleScope.launch {
            try {
                // Count lost items
                val lostItems = supabaseClient.postgrest["lost_items"]
                    .select()
                    .decodeList<SupabaseLostItem>()
                totalLostItems.text = lostItems.size.toString()

                // Count found items
                val foundItems = supabaseClient.postgrest["found_items"]
                    .select()
                    .decodeList<SupabaseFoundItem>()
                totalFoundItems.text = foundItems.size.toString()

                // Count claimed items
                val claimedItems = foundItems.count { it.claimed }
                totalClaims.text = claimedItems.toString()

                // Count pending claims (unclaimed found items)
                val pendingItems = foundItems.count { !it.claimed }
                pendingClaims.text = pendingItems.toString()

                // Show stats
                statsCard.alpha = 1.0f
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Failed to load statistics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadReports(type: String) {
        // Show loading
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = "Loading..."

        lifecycleScope.launch {
            try {
                when (type) {
                    "lost_items" -> {
                        val items = supabaseClient.postgrest["lost_items"]
                            .select()
                            .decodeList<SupabaseLostItem>()

                        if (items.isEmpty()) {
                            emptyView.text = "No lost items found"
                            emptyView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            adapter.updateLostItems(items)
                            emptyView.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        }
                    }
                    "found_items" -> {
                        val items = supabaseClient.postgrest["found_items"]
                            .select()
                            .decodeList<SupabaseFoundItem>()

                        if (items.isEmpty()) {
                            emptyView.text = "No found items found"
                            emptyView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            adapter.updateFoundItems(items)
                            emptyView.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                emptyView.text = "Error loading data: ${e.message}"
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
        }
    }

    private fun showItemDetails(item: Any) {
        val (itemId, itemName) = when (item) {
            is SupabaseLostItem -> item.id to item.name
            is SupabaseFoundItem -> item.id to item.name
            else -> "Unknown" to "Unknown Item"
        }

        Toast.makeText(this, "Showing details for $itemName", Toast.LENGTH_SHORT).show()

        MaterialAlertDialogBuilder(this)
            .setTitle("Item Details")
            .setMessage("Item ID: $itemId\nName: $itemName")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun markAsFound(item: Any) {
        when (item) {
            is SupabaseFoundItem -> {
                lifecycleScope.launch {
                    try {
                        supabaseClient.postgrest["found_items"]
                            .update({
                                set("claimed", !item.claimed)
                                set("claimedByEmail", if (!item.claimed) "admin" else "")
                            }) {
                                filter {
                                    eq("id", item.id)
                                }
                            }

                        Toast.makeText(this@AdminDashboardActivity, "Item status updated", Toast.LENGTH_SHORT).show()
                        loadReports(currentItemType)
                        loadStatistics()
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminDashboardActivity, "Failed to update item: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun deleteItem(item: Any) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        when (item) {
                            is SupabaseLostItem -> {
                                supabaseClient.postgrest["lost_items"]
                                    .delete {
                                        filter {
                                            eq("id", item.id)
                                        }
                                    }
                            }
                            is SupabaseFoundItem -> {
                                supabaseClient.postgrest["found_items"]
                                    .delete {
                                        filter {
                                            eq("id", item.id)
                                        }
                                    }
                            }
                        }

                        Toast.makeText(this@AdminDashboardActivity, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                        loadReports(currentItemType)
                        loadStatistics()
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminDashboardActivity, "Failed to delete item: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        recreate()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        // Since we don't have proper auth setup, just navigate to login
        Toast.makeText(this@AdminDashboardActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate to login screen
        val intent = Intent(this@AdminDashboardActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
