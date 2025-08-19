package com.example.campus_lost_found

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.campus_lost_found.model.FoundItem
import com.example.campus_lost_found.model.LostItem
import com.example.campus_lost_found.repository.ItemRepository
import com.example.campus_lost_found.utils.SupabaseManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportItemActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var locationEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var keptAtLayout: View
    private lateinit var keptAtEditText: EditText
    private lateinit var itemImageView: ImageView
    private lateinit var uploadImageButton: Button
    private lateinit var submitButton: Button

    private val itemRepository = ItemRepository()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private var isLostItem = true
    private var selectedDate: Date = Date()
    private var imageUri: Uri? = null
    private var imageUrl: String = ""
    private var editItemId: String? = null
    private var editingExistingItem = false

    private val currentUserId: String
        get() = SupabaseManager.getInstance().getCurrentUser() ?: ""
    private val currentUserEmail: String
        get() = SupabaseManager.getInstance().getCurrentUserEmail() ?: "Anonymous User"

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(itemImageView)
            itemImageView.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_item)

        // Get intent data
        isLostItem = intent.getBooleanExtra("isLostItem", true)
        editItemId = intent.getStringExtra("itemId")
        editingExistingItem = editItemId != null

        // Initialize views
        initViews()
        setupUI()

        // Load item data if editing
        if (editingExistingItem) {
            loadItemData()
        }
    }

    private fun initViews() {
        titleTextView = findViewById(R.id.reportTitleTextView)
        nameEditText = findViewById(R.id.itemNameEditText)
        descriptionEditText = findViewById(R.id.itemDescriptionEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        locationEditText = findViewById(R.id.locationEditText)
        dateButton = findViewById(R.id.dateButton)
        keptAtLayout = findViewById(R.id.keptAtLayout)
        keptAtEditText = findViewById(R.id.keptAtEditText)
        itemImageView = findViewById(R.id.itemImageView)
        uploadImageButton = findViewById(R.id.uploadImageButton)
        submitButton = findViewById(R.id.submitButton)
    }

    private fun setupUI() {
        // Set title based on mode
        val titleText = if (editingExistingItem) {
            if (isLostItem) "Edit Lost Item" else "Edit Found Item"
        } else {
            if (isLostItem) "Report Lost Item" else "Report Found Item"
        }
        titleTextView.text = titleText
        submitButton.text = if (editingExistingItem) "Update" else "Submit"

        // Setup category spinner
        val categories = arrayOf(
            "Electronics", "Books & Stationery", "ID Cards & Documents",
            "Keys", "Clothing", "Accessories", "Wallet/Purse", "Other"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Show/hide kept at field based on item type
        keptAtLayout.visibility = if (isLostItem) View.GONE else View.VISIBLE

        // Date button setup
        updateDateButtonText()
        dateButton.setOnClickListener {
            showDatePicker()
        }

        // Image upload button
        uploadImageButton.setOnClickListener {
            getContent.launch("image/*")
        }

        // Submit button
        submitButton.setOnClickListener {
            if (validateInputs()) {
                if (imageUri != null && imageUrl.isEmpty()) {
                    uploadImageAndSaveItem()
                } else {
                    saveItem()
                }
            }
        }
    }

    private fun loadItemData() {
        if (editItemId.isNullOrEmpty()) return

        // For editing, we'll disable the functionality for now since we don't have
        // individual item retrieval methods in our repository
        // In a full implementation, you would add getSingleLostItem/getSingleFoundItem methods
        showErrorDialog("Edit functionality is not yet implemented for individual items")
    }

    private fun setCategorySpinnerSelection(category: String) {
        val adapter = categorySpinner.adapter as? ArrayAdapter<String>
        adapter?.let {
            for (i in 0 until it.count) {
                if (it.getItem(i) == category) {
                    categorySpinner.setSelection(i)
                    break
                }
            }
        }
    }

    private fun updateDateButtonText() {
        val dateText = if (isLostItem) {
            "Date Lost: ${dateFormat.format(selectedDate)}"
        } else {
            "Date Found: ${dateFormat.format(selectedDate)}"
        }
        dateButton.text = dateText
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isLostItem) "Select Lost Date" else "Select Found Date")
            .setSelection(selectedDate.time)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = Date(selection)
            updateDateButtonText()
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun validateInputs(): Boolean {
        if (nameEditText.text.isBlank()) {
            showErrorDialog("Please enter the item name")
            return false
        }

        if (locationEditText.text.isBlank()) {
            showErrorDialog("Please enter the location")
            return false
        }

        if (!isLostItem && keptAtEditText.text.isBlank()) {
            showErrorDialog("Please enter where the item is kept")
            return false
        }

        if (currentUserId.isEmpty()) {
            showErrorDialog("You must be signed in to report items")
            return false
        }

        return true
    }

    // Supabase image upload (replaces Firebase Storage)
    private fun uploadImageAndSaveItem() {
        imageUri?.let { uri ->
            // Show loading state
            submitButton.isEnabled = false
            submitButton.text = "Uploading..."

            // Show initial upload toast
            showUploadStartNotification()

            lifecycleScope.launch {
                try {
                    val result = SupabaseManager.getInstance().uploadImage(
                        context = this@ReportItemActivity,
                        imageUri = uri,
                        userId = currentUserId
                    )

                    result.onSuccess { url ->
                        imageUrl = url
                        runOnUiThread {
                            // Show success notification
                            showUploadSuccessNotification()

                            // Reset button to saving state
                            submitButton.text = "Saving item..."

                            // Save the item after successful upload
                            saveItem()
                        }
                    }.onFailure { exception ->
                        runOnUiThread {
                            Log.e("ReportItem", "Supabase upload failed: ${exception.message}")
                            showUploadErrorNotification(exception.message ?: "Unknown error")
                            // Reset button state
                            submitButton.isEnabled = true
                            submitButton.text = if (editingExistingItem) "Update" else "Submit"
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Log.e("ReportItem", "Error during Supabase upload: ${e.message}")
                        showUploadErrorNotification(e.message ?: "Unknown error")
                        // Reset button state
                        submitButton.isEnabled = true
                        submitButton.text = if (editingExistingItem) "Update" else "Submit"
                    }
                }
            }
        } ?: run {
            // No image selected, just save the item
            saveItem()
        }
    }

    private fun showUploadStartNotification() {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            "📤 Uploading image to cloud storage...",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        )
        snackbar.setBackgroundTint(getColor(R.color.primary_container))
        snackbar.setTextColor(getColor(R.color.on_primary_container))
        snackbar.show()
    }

    private fun showUploadSuccessNotification() {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            "✅ Image uploaded successfully!",
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(getColor(R.color.tertiary_container))
        snackbar.setTextColor(getColor(R.color.on_tertiary_container))
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()

        // Also show a toast for immediate feedback
        Toast.makeText(this, "✅ Photo uploaded to cloud storage!", Toast.LENGTH_SHORT).show()
    }

    private fun showUploadErrorNotification(errorMessage: String) {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            "❌ Upload failed: $errorMessage",
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(getColor(R.color.error_container))
        snackbar.setTextColor(getColor(R.color.on_error_container))
        snackbar.setAction("RETRY") {
            snackbar.dismiss()
            uploadImageAndSaveItem() // Retry upload
        }
        snackbar.show()
    }

    private fun saveItem() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val location = locationEditText.text.toString().trim()

        if (isLostItem) {
            val lostItem = LostItem(
                id = editItemId ?: java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                category = category,
                location = location,
                imageUrl = imageUrl,
                reportedBy = currentUserId,
                reportedByName = currentUserEmail,
                reportedDate = System.currentTimeMillis(),
                dateLost = selectedDate.time
            )

            lifecycleScope.launch {
                if (editingExistingItem) {
                    itemRepository.updateLostItem(lostItem,
                        onSuccess = {
                            showSuccessDialog("Item updated successfully")
                        },
                        onFailure = { e ->
                            showErrorDialog("Failed to update item: ${e.message}")
                        }
                    )
                } else {
                    itemRepository.addLostItem(lostItem,
                        onSuccess = {
                            showSuccessDialog("Item reported successfully")
                        },
                        onFailure = { e ->
                            showErrorDialog("Failed to save item: ${e.message}")
                        }
                    )
                }
            }
        } else {
            val keptAt = keptAtEditText.text.toString().trim()

            val foundItem = FoundItem(
                id = editItemId ?: java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                category = category,
                location = location,
                imageUrl = imageUrl,
                reportedBy = currentUserId,
                reportedByName = currentUserEmail,
                reportedDate = System.currentTimeMillis(),
                keptAt = keptAt,
                claimed = false,
                claimedBy = "",
                claimedByName = "",
                dateFound = selectedDate.time
            )

            lifecycleScope.launch {
                if (editingExistingItem) {
                    itemRepository.updateFoundItem(foundItem,
                        onSuccess = {
                            showSuccessDialog("Item updated successfully")
                        },
                        onFailure = { e ->
                            showErrorDialog("Failed to update item: ${e.message}")
                        }
                    )
                } else {
                    itemRepository.addFoundItem(foundItem,
                        onSuccess = {
                            showSuccessDialog("Item reported successfully")
                        },
                        onFailure = { e ->
                            showErrorDialog("Failed to save item: ${e.message}")
                        }
                    )
                }
            }
        }
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                setResult(Activity.RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        fun createEditIntent(context: Context, isLostItem: Boolean, itemId: String): Intent {
            return Intent(context, ReportItemActivity::class.java).apply {
                putExtra("isLostItem", isLostItem)
                putExtra("itemId", itemId)
            }
        }
    }
}
