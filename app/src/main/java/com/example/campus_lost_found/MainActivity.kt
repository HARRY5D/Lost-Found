package com.example.campus_lost_found

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

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
            // Set up toolbar
            val toolbar: Toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = getString(R.string.app_name)

            // Handle logout button in toolbar
            val logoutButton = findViewById<MaterialButton>(R.id.logoutButton)
            logoutButton?.setOnClickListener {
                showLogoutConfirmation()
            }

            // Check authentication state and hide/show logout button
            val currentUser = FirebaseAuth.getInstance().currentUser
            logoutButton?.visibility = if (currentUser == null) {
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
            // Get the TabLayout from the new design
            tabLayout = findViewById(R.id.mainTabLayout)

            // Create ViewPager2 and replace the fragment container
            viewPager = ViewPager2(this)
            viewPager.id = android.view.View.generateViewId()

            // Find the fragment container and replace it with ViewPager2
            val fragmentContainer = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.nav_host_fragment)
            if (fragmentContainer != null) {
                val parent = fragmentContainer.parent as android.view.ViewGroup
                val layoutParams = fragmentContainer.layoutParams
                parent.removeView(fragmentContainer)
                parent.addView(viewPager, layoutParams)

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
            } else {
                Log.e(TAG, "Fragment container not found")
                throw Exception("Fragment container not found")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup tabs: ${e.message}")
            // If ViewPager setup fails, create a simple fallback
            createSimpleTabLayout()
        }
    }

    private fun createSimpleTabLayout() {
        try {
            // Hide the problematic ViewPager and create simple button navigation
            tabLayout.visibility = android.view.View.GONE

            // Create simple fragment container
            val fragmentContainer = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (fragmentContainer == null) {
                // Manually add the first fragment
                supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, LostItemsFragment())
                    .commit()
            }

            Log.d(TAG, "Simple tab layout created")
        } catch (e: Exception) {
            Log.e(TAG, "Even simple layout failed: ${e.message}")
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

        // Add logout button if user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
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
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show logout confirmation: ${e.message}")
            Toast.makeText(this, "Logout feature temporarily unavailable", Toast.LENGTH_SHORT).show()
        }
    }
}
