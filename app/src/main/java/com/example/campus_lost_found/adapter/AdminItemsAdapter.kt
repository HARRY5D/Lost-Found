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
import com.example.campus_lost_found.model.SupabaseFoundItem
import com.example.campus_lost_found.model.SupabaseLostItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminItemsAdapter(
    private val itemType: String,
    private val onItemClicked: (Any) -> Unit,
    private val onApproveClicked: (Any) -> Unit,
    private val onRejectClicked: (Any) -> Unit
) : RecyclerView.Adapter<AdminItemsAdapter.AdminItemViewHolder>() {

    private var lostItems: List<SupabaseLostItem> = emptyList()
    private var foundItems: List<SupabaseFoundItem> = emptyList()

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
        val context = holder.itemView.context
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        when (itemType) {
            "lost_items" -> {
                val item = lostItems[position]
                bindLostItem(holder, item, context, dateFormat)
            }
            "found_items" -> {
                val item = foundItems[position]
                bindFoundItem(holder, item, context, dateFormat)
            }
        }
    }

    private fun bindLostItem(
        holder: AdminItemViewHolder,
        item: SupabaseLostItem,
        context: android.content.Context,
        dateFormat: SimpleDateFormat
    ) {
        // Basic item info
        holder.itemName.text = item.name
        holder.itemCategory.text = item.category
        holder.itemLocation.text = item.location
        holder.itemDescription.text = item.description ?: context.getString(R.string.no_description_provided) // Handle nullable description
        holder.itemDate.text = dateFormat.format(Date(item.reportedDate))

        // Load image
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Status
        holder.itemStatus.text = context.getString(R.string.lost_item)
        holder.itemStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)

        // Hide claim section for lost items
        holder.claimSection.visibility = View.GONE

        // Button actions
        holder.detailsButton.setOnClickListener { onItemClicked(item) }
        holder.approveButton.text = context.getString(R.string.mark_found)
        holder.approveButton.setOnClickListener { onApproveClicked(item) }
        holder.rejectButton.text = context.getString(R.string.delete)
        holder.rejectButton.setOnClickListener { onRejectClicked(item) }
    }

    private fun bindFoundItem(
        holder: AdminItemViewHolder,
        item: SupabaseFoundItem,
        context: android.content.Context,
        dateFormat: SimpleDateFormat
    ) {
        // Basic item info
        holder.itemName.text = item.name
        holder.itemCategory.text = item.category
        holder.itemLocation.text = item.location
        holder.itemDescription.text = item.description ?: context.getString(R.string.no_description_provided) // Handle nullable description
        holder.itemDate.text = dateFormat.format(Date(item.reportedDate))

        // Load image
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Status based on claimed state
        if (item.claimed) {
            holder.itemStatus.text = context.getString(R.string.claimed)
            holder.itemStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
        } else {
            holder.itemStatus.text = context.getString(R.string.available)
            holder.itemStatus.setChipBackgroundColorResource(android.R.color.holo_blue_light)
        }

        // Show claim info if claimed
        if (item.claimed && item.claimedByEmail.isNotEmpty()) {
            holder.claimSection.visibility = View.VISIBLE
            holder.claimBy.text = context.getString(R.string.claimed_by_format, item.claimedByEmail)
            holder.claimMessage.text = context.getString(R.string.kept_at_format, item.keptAt)
        } else {
            holder.claimSection.visibility = View.GONE
        }

        // Button actions
        holder.detailsButton.setOnClickListener { onItemClicked(item) }

        if (item.claimed) {
            holder.approveButton.text = context.getString(R.string.mark_unclaimed)
            holder.approveButton.setOnClickListener { onApproveClicked(item) }
        } else {
            holder.approveButton.text = context.getString(R.string.mark_claimed)
            holder.approveButton.setOnClickListener { onApproveClicked(item) }
        }

        holder.rejectButton.text = context.getString(R.string.delete)
        holder.rejectButton.setOnClickListener { onRejectClicked(item) }
    }

    override fun getItemCount(): Int {
        return when (itemType) {
            "lost_items" -> lostItems.size
            "found_items" -> foundItems.size
            else -> 0
        }
    }

    fun updateLostItems(newItems: List<SupabaseLostItem>) {
        lostItems = newItems
        notifyDataSetChanged()
    }

    fun updateFoundItems(newItems: List<SupabaseFoundItem>) {
        foundItems = newItems
        notifyDataSetChanged()
    }
}
