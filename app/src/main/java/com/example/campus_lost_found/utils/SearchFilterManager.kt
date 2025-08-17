package com.example.campus_lost_found.utils

import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.example.campus_lost_found.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SearchFilterManager(
    private val context: Context,
    private val rootView: View,
    private val onSearchFiltersChanged: (
        query: String,
        categories: List<String>,
        startDate: Date?,
        endDate: Date?,
        location: String?
    ) -> Unit
) {
    private val searchEditText: TextInputEditText = rootView.findViewById(R.id.search_edit_text)
    private val categoryChipGroup: ChipGroup = rootView.findViewById(R.id.category_chip_group)
    private val dateFilterButton: MaterialButton = rootView.findViewById(R.id.date_filter_button)
    private val locationFilterButton: MaterialButton = rootView.findViewById(R.id.location_filter_button)

    private var searchQuery: String = ""
    private val selectedCategories = mutableListOf<String>()
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var selectedLocation: String? = null

    // Pre-defined campus locations
    private val campusLocations = listOf(
        "Library", "Student Center", "Academic Building", "Cafeteria", 
        "Gymnasium", "Dormitory", "Parking Lot", "Auditorium", "Labs"
    )

    init {
        setupSearchListener()
        setupCategoryChips()
        setupDateFilter()
        setupLocationFilter()
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                notifyFiltersChanged()
            }
        })
    }

    private fun setupCategoryChips() {
        // Set up click listeners for all category chips
        for (i in 0 until categoryChipGroup.childCount) {
            val chip = categoryChipGroup.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { _, isChecked ->
                val category = chip.text.toString()
                if (isChecked) {
                    if (!selectedCategories.contains(category)) {
                        selectedCategories.add(category)
                    }
                } else {
                    selectedCategories.remove(category)
                }
                notifyFiltersChanged()
            }
        }
    }

    private fun setupDateFilter() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        dateFilterButton.setOnClickListener {
            // Create a dialog for date range selection
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_date_range, null)
            val startDateButton = dialogView.findViewById<MaterialButton>(R.id.start_date_button)
            val endDateButton = dialogView.findViewById<MaterialButton>(R.id.end_date_button)
            val clearButton = dialogView.findViewById<MaterialButton>(R.id.clear_date_button)

            // Initialize with current values
            startDateButton.text = startDate?.let { dateFormat.format(it) } ?: "Select Start Date"
            endDateButton.text = endDate?.let { dateFormat.format(it) } ?: "Select End Date"

            // Start date button - use MaterialDatePicker for better calendar selection
            startDateButton.setOnClickListener {
                val builder = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Start Date")

                // If there's already a start date, select it in the picker
                if (startDate != null) {
                    builder.setSelection(startDate!!.time)
                }

                val datePicker = builder.build()

                datePicker.addOnPositiveButtonClickListener { selection ->
                    startDate = Date(selection)
                    startDateButton.text = dateFormat.format(startDate!!)
                }

                // Show the calendar picker
                datePicker.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "START_DATE_PICKER")
            }

            // End date button - use MaterialDatePicker for better calendar selection
            endDateButton.setOnClickListener {
                val builder = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select End Date")

                // If there's already an end date, select it in the picker
                if (endDate != null) {
                    builder.setSelection(endDate!!.time)
                }

                val datePicker = builder.build()

                datePicker.addOnPositiveButtonClickListener { selection ->
                    endDate = Date(selection)
                    endDateButton.text = dateFormat.format(endDate!!)
                }

                // Show the calendar picker
                datePicker.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "END_DATE_PICKER")
            }

            clearButton.setOnClickListener {
                startDate = null
                endDate = null
                startDateButton.text = "Select Start Date"
                endDateButton.text = "Select End Date"
            }

            // Show dialog
            MaterialAlertDialogBuilder(context)
                .setTitle("Select Date Range")
                .setView(dialogView)
                .setPositiveButton("Apply") { _, _ ->
                    updateDateFilterButtonText()
                    notifyFiltersChanged()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupLocationFilter() {
        locationFilterButton.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_location_filter, null)
            val locationSpinner = dialogView.findViewById<Spinner>(R.id.location_spinner)
            val locationEditText = dialogView.findViewById<EditText>(R.id.location_edit_text)
            
            // Set up the spinner with campus locations
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, 
                listOf("Select location...") + campusLocations + "Other")
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            locationSpinner.adapter = adapter
            
            // Set initial selection based on current filter
            if (selectedLocation != null) {
                val position = campusLocations.indexOf(selectedLocation)
                if (position >= 0) {
                    locationSpinner.setSelection(position + 1) // +1 for the "Select location..." item
                } else {
                    locationSpinner.setSelection(adapter.count - 1) // "Other" option
                    locationEditText.setText(selectedLocation)
                    locationEditText.visibility = View.VISIBLE
                }
            }
            
            // Show/hide the EditText based on the "Other" selection
            locationSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    locationEditText.visibility = if (position == adapter.count - 1) View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Show dialog
            MaterialAlertDialogBuilder(context)
                .setTitle("Filter by Location")
                .setView(dialogView)
                .setPositiveButton("Apply") { _, _ ->
                    val position = locationSpinner.selectedItemPosition
                    selectedLocation = when {
                        position == 0 -> null // "Select location..."
                        position < adapter.count - 1 -> adapter.getItem(position).toString() // Campus locations
                        else -> locationEditText.text.toString().takeIf { it.isNotBlank() } // "Other" with custom input
                    }
                    updateLocationFilterButtonText()
                    notifyFiltersChanged()
                }
                .setNegativeButton("Clear") { _, _ ->
                    selectedLocation = null
                    updateLocationFilterButtonText()
                    notifyFiltersChanged()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }
    }

    private fun updateDateFilterButtonText() {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        dateFilterButton.text = when {
            startDate != null && endDate != null -> 
                "${dateFormat.format(startDate!!)} - ${dateFormat.format(endDate!!)}"
            startDate != null -> 
                "From ${dateFormat.format(startDate!!)}"
            endDate != null -> 
                "Until ${dateFormat.format(endDate!!)}"
            else -> 
                "Date Filter"
        }
    }

    private fun updateLocationFilterButtonText() {
        locationFilterButton.text = selectedLocation ?: "Location"
    }

    private fun notifyFiltersChanged() {
        onSearchFiltersChanged(searchQuery, selectedCategories, startDate, endDate, selectedLocation)
    }

    // Helper methods for converting between Date and Timestamp
    fun getStartTimestamp(): Timestamp? {
        return startDate?.let { Timestamp(it) }
    }

    fun getEndTimestamp(): Timestamp? {
        // If end date is specified, add 23:59:59 to include the whole day
        return endDate?.let {
            val calendar = Calendar.getInstance()
            calendar.time = it
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            Timestamp(calendar.time)
        }
    }

    fun clearAllFilters() {
        searchEditText.setText("")
        searchQuery = ""

        // Clear category selections
        for (i in 0 until categoryChipGroup.childCount) {
            val chip = categoryChipGroup.getChildAt(i) as Chip
            chip.isChecked = false
        }
        selectedCategories.clear()

        // Clear date filter
        startDate = null
        endDate = null
        dateFilterButton.text = "Date Filter"

        // Clear location filter
        selectedLocation = null
        locationFilterButton.text = "Location"

        // Notify that filters changed
        notifyFiltersChanged()
    }
}
