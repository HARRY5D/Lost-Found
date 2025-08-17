package com.example.campus_lost_found.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.campus_lost_found.R
import com.example.campus_lost_found.model.FoundItem
import com.example.campus_lost_found.model.Item
import com.example.campus_lost_found.model.LostItem
import java.text.SimpleDateFormat
import java.util.Locale

class ItemsAdapter(
    private var items: MutableList<Item> = mutableListOf(),
    private val isLostItemsList: Boolean,
    private val currentUserId: String,
    private val onClaimButtonClick: ((Item) -> Unit)? = null,
    private val onItemClick: ((Item) -> Unit)? = null
) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, isLostItemsList, currentUserId, onClaimButtonClick, onItemClick)
    }

    override fun getItemCount() = items.size

    // Method to update the items list efficiently
    fun updateItems(newItems: List<Item>) {
        val diffCallback = ItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    // DiffUtil callback for efficient list updates
    private class ItemDiffCallback(
        private val oldList: List<Item>,
        private val newList: List<Item>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemImage: ImageView = itemView.findViewById(R.id.itemImage)
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemDescription: TextView = itemView.findViewById(R.id.itemDescription)
        private val itemCategory: TextView = itemView.findViewById(R.id.itemCategory)
        private val itemLocation: TextView = itemView.findViewById(R.id.itemLocation)
        private val itemDate: TextView = itemView.findViewById(R.id.itemDate)
        private val claimButton: Button = itemView.findViewById(R.id.claimButton)

        fun bind(
            item: Item,
            isLostItemsList: Boolean,
            currentUserId: String,
            onClaimButtonClick: ((Item) -> Unit)?,
            onItemClick: ((Item) -> Unit)?
        ) {
            itemName.text = item.name
            itemDescription.text = item.description
            itemCategory.text = item.category

            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            when (item) {
                is LostItem -> {
                    itemLocation.text = itemView.context.getString(R.string.lost_at, item.location)
                    itemDate.text = itemView.context.getString(R.string.lost_on, dateFormat.format(item.dateLost.toDate()))
                }
                is FoundItem -> {
                    itemLocation.text = itemView.context.getString(R.string.found_at, item.location)
                    itemDate.text = itemView.context.getString(R.string.found_on, dateFormat.format(item.dateFound.toDate()))
                }
            }

            // Load image with Glide
            if (item.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(itemImage)
            } else {
                itemImage.setImageResource(R.drawable.ic_placeholder)
            }

            // Set up claim button
            if (item.reportedBy == currentUserId) {
                claimButton.visibility = View.GONE
            } else {
                claimButton.visibility = View.VISIBLE
                claimButton.text = if (isLostItemsList) {
                    itemView.context.getString(R.string.i_found_this)
                } else {
                    itemView.context.getString(R.string.this_is_mine)
                }
                claimButton.setOnClickListener {
                    onClaimButtonClick?.invoke(item)
                }
            }

            // Set up item click
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}
