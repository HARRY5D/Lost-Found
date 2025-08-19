package com.example.campus_lost_found

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.campus_lost_found.utils.SupabaseManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Starting MainActivity onCreate")

            setContentView(R.layout.activity_main)

            // Initialize views and setup tabs
            initializeViews()
            setupTabsAndViewPager()

            // Show success message
            Toast.makeText(this, "Welcome to Campus Lost & Found!", Toast.LENGTH_SHORT).show()

            Log.d(TAG, "MainActivity initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in MainActivity: ${e.message}", e)
            // Create fallback simple layout if XML fails
            createSimpleLayout()
        }
    }

    private fun initializeViews() {
        try {
            // No need to set up toolbar since we're using a custom layout
            supportActionBar?.hide() // Hide the default action bar

            // Handle logout button - now directly accessible
            val logoutButton = findViewById<MaterialButton>(R.id.logoutButton)
            logoutButton?.setOnClickListener {
                showLogoutConfirmation()
            }

            // Check Supabase authentication state and hide/show logout button
            val isUserSignedIn = SupabaseManager.getInstance().isUserSignedIn()
            logoutButton?.visibility = if (!isUserSignedIn) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }

            // Handle FAB
            val fabAddItem = findViewById<FloatingActionButton>(R.id.fabAddItem)
            fabAddItem?.setOnClickListener {
                try {
                    val intent = Intent(this, ReportItemActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Report Item feature temporarily unavailable", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}")
            throw e
        }
    }

    private fun setupTabsAndViewPager() {
        try {
            // Get views directly from layout - much simpler approach
            tabLayout = findViewById(R.id.mainTabLayout)
            viewPager = findViewById(R.id.mainViewPager)

            // Set up ViewPager2 with adapter
            viewPager.adapter = MainPagerAdapter(this)

            // Connect TabLayout with ViewPager2
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Lost Items"
                    1 -> "Found Items"
                    2 -> "My Reports"
                    else -> "Tab ${position + 1}"
                }
            }.attach()

            Log.d(TAG, "Tabs and ViewPager setup completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup tabs: ${e.message}", e)
            // Simple fallback if TabLayout setup fails
            setupSimpleFragments()
        }
    }

    private fun setupSimpleFragments() {
        try {
            // Fallback: Create a simple fragment container if ViewPager fails
            val container = android.widget.FrameLayout(this)
            container.id = android.view.View.generateViewId()

            // Replace the ViewPager with a simple container
            val parent = viewPager.parent as android.view.ViewGroup
            val layoutParams = viewPager.layoutParams
            val index = parent.indexOfChild(viewPager)

            parent.removeView(viewPager)
            parent.addView(container, index, layoutParams)

            // Show the first fragment by default
            supportFragmentManager.beginTransaction()
                .replace(container.id, LostItemsFragment())
                .commit()

            // Set up tab click listeners manually
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val fragment = when (tab?.position) {
                        0 -> LostItemsFragment()
                        1 -> FoundItemsFragment()
                        2 -> MyReportsFragment()
                        else -> LostItemsFragment()
                    }

                    supportFragmentManager.beginTransaction()
                        .replace(container.id, fragment)
                        .commit()
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            Log.d(TAG, "Simple fragments setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Even simple fragments setup failed: ${e.message}", e)
        }
    }

    // Adapter for ViewPager2 to manage fragments
    private inner class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LostItemsFragment()
                1 -> FoundItemsFragment()
                2 -> MyReportsFragment()
                else -> LostItemsFragment()
            }
        }
    }

    private fun createSimpleLayout() {
        // Create a simple linear layout programmatically
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)

        // Title
        val titleText = android.widget.TextView(this)
        titleText.text = "Campus Lost & Found"
        titleText.textSize = 24f
        titleText.setPadding(0, 0, 0, 50)
        layout.addView(titleText)

        // Welcome message
        val welcomeText = android.widget.TextView(this)
        welcomeText.text = "Welcome! The app is running successfully."
        welcomeText.textSize = 16f
        welcomeText.setPadding(0, 0, 0, 30)
        layout.addView(welcomeText)

        // Add Report Item button
        val reportButton = android.widget.Button(this)
        reportButton.text = "Report Item"
        reportButton.setOnClickListener {
            try {
                val intent = Intent(this, ReportItemActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Report Item feature temporarily unavailable", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(reportButton)

        // Add logout button if user is authenticated with Supabase
        val isUserSignedIn = SupabaseManager.getInstance().isUserSignedIn()
        if (isUserSignedIn) {
            val logoutButton = android.widget.Button(this)
            logoutButton.text = "Logout"
            logoutButton.setOnClickListener {
                showLogoutConfirmation()
            }
            layout.addView(logoutButton)
        }

        // Set the layout as content view
        setContentView(layout)
    }

    private fun showLogoutConfirmation() {
        try {
            MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    // Use Supabase logout instead of Firebase
                    lifecycleScope.launch {
                        try {
                            val result = SupabaseManager.getInstance().signOut()
                            result.onSuccess {
                                runOnUiThread {
                                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            }.onFailure { exception ->
                                runOnUiThread {
                                    Log.e(TAG, "Logout failed: ${exception.message}")
                                    Toast.makeText(this@MainActivity, "Logout failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Log.e(TAG, "Error during logout: ${e.message}")
                                Toast.makeText(this@MainActivity, "Logout error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show logout confirmation: ${e.message}")
            Toast.makeText(this, "Logout feature temporarily unavailable", Toast.LENGTH_SHORT).show()
        }
    }
}
