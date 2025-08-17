package com.example.campus_lost_found.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.campus_lost_found.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminItemsAdapter(
    private var items: List<DocumentSnapshot> = emptyList(),
    private val itemType: String,
    private val onItemClicked: (DocumentSnapshot) -> Unit,
    private val onApproveClicked: (DocumentSnapshot) -> Unit,
    private val onRejectClicked: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdminItemsAdapter.AdminItemViewHolder>() {

    class AdminItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.admin_item_image)
        val itemName: TextView = itemView.findViewById(R.id.admin_item_name)
        val itemCategory: TextView = itemView.findViewById(R.id.admin_item_category)
        val itemDate: TextView = itemView.findViewById(R.id.admin_item_date)
        val itemLocation: TextView = itemView.findViewById(R.id.admin_item_location)
        val itemStatus: Chip = itemView.findViewById(R.id.admin_item_status)
        val itemDescription: TextView = itemView.findViewById(R.id.admin_item_description)
        val claimSection: LinearLayout = itemView.findViewById(R.id.admin_claim_section)
        val claimBy: TextView = itemView.findViewById(R.id.admin_claim_by)
        val claimMessage: TextView = itemView.findViewById(R.id.admin_claim_message)
        val detailsButton: MaterialButton = itemView.findViewById(R.id.admin_btn_details)
        val approveButton: MaterialButton = itemView.findViewById(R.id.admin_btn_approve)
        val rejectButton: MaterialButton = itemView.findViewById(R.id.admin_btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_report, parent, false)
        return AdminItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminItemViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        // Set up common fields for all item types
        val itemName = item.getString("name") ?: "Unnamed Item"
        val category = item.getString("category") ?: "Uncategorized"
        val description = item.getString("description") ?: "No description provided"
        val location = item.getString("location") ?: "Unknown location"
        val imageUrl = item.getString("imageURL")

        holder.itemName.text = itemName
        holder.itemCategory.text = "Category: $category"
        holder.itemLocation.text = "Location: $location"
        holder.itemDescription.text = description

        // Handle item image
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_admin)
                .error(R.drawable.ic_admin)
                .centerCrop()
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(R.drawable.ic_admin)
        }

        // Set item specific data based on type
        when (itemType) {
            "lost" -> {
                val dateLost = item.getTimestamp("dateLost")?.toDate() ?: Date()
                val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dateLost)
                holder.itemDate.text = "Lost on: $dateFormatted"
                holder.itemStatus.text = "Lost"
                holder.itemStatus.setChipBackgroundColorResource(R.color.error_container)

                // Hide claim section for lost items
                holder.claimSection.visibility = View.GONE
                holder.approveButton.visibility = View.GONE
                holder.rejectButton.visibility = View.GONE
            }
            "found" -> {
                val dateFound = item.getTimestamp("dateFound")?.toDate() ?: Date()
                val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dateFound)
                holder.itemDate.text = "Found on: $dateFormatted"

                // Show where the item is kept
                val keptAt = item.getString("keptAt") ?: "Not specified"
                holder.itemDescription.text = "$description\n\nKept at: $keptAt"

                val isClaimed = item.getBoolean("claimed") ?: false
                if (isClaimed) {
                    holder.itemStatus.text = "Claimed"
                    holder.itemStatus.setChipBackgroundColorResource(R.color.secondary_container)
                } else {
                    holder.itemStatus.text = "Found"
                    holder.itemStatus.setChipBackgroundColorResource(R.color.primary_container)
                }

                // Hide claim section for found items
                holder.claimSection.visibility = View.GONE
                holder.approveButton.visibility = View.GONE
                holder.rejectButton.visibility = View.GONE
            }
            "claims" -> {
                val dateClaimed = item.getTimestamp("dateClaimed")?.toDate() ?: Date()
                val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dateClaimed)
                holder.itemDate.text = "Claimed on: $dateFormatted"

                // Show claim details
                val claimedBy = item.getString("claimedBy") ?: "Unknown user"
                val claimMessage = item.getString("message") ?: "No message provided"

                holder.claimSection.visibility = View.VISIBLE
                holder.claimBy.text = "Claimed by: $claimedBy"
                holder.claimMessage.text = "Message: $claimMessage"

                // Set status and action buttons based on approval state
                val isApproved = item.getBoolean("approved") ?: false
                val isRejected = item.getBoolean("rejected") ?: false

                when {
                    isApproved -> {
                        holder.itemStatus.text = "Approved"
                        holder.itemStatus.setChipBackgroundColorResource(R.color.tertiary_container)
                        holder.approveButton.visibility = View.GONE
                        holder.rejectButton.visibility = View.GONE
                    }
                    isRejected -> {
                        holder.itemStatus.text = "Rejected"
                        holder.itemStatus.setChipBackgroundColorResource(R.color.error_container)
                        holder.approveButton.visibility = View.GONE
                        holder.rejectButton.visibility = View.GONE
                    }
                    else -> {
                        holder.itemStatus.text = "Pending"
                        holder.itemStatus.setChipBackgroundColorResource(R.color.secondary_container)
                        holder.approveButton.visibility = View.VISIBLE
                        holder.rejectButton.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Set up click listeners
        holder.detailsButton.setOnClickListener {
            onItemClicked(item)
        }

        holder.approveButton.setOnClickListener {
            onApproveClicked(item)
        }

        holder.rejectButton.setOnClickListener {
            onRejectClicked(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<DocumentSnapshot>) {
        items = newItems
        notifyDataSetChanged()
    }
}
