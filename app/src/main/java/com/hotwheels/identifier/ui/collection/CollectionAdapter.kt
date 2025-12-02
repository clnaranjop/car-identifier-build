package com.hotwheels.identifier.ui.collection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hotwheels.identifier.databinding.ItemCollectionBinding
import com.hotwheels.identifier.viewmodel.CollectionItemWithModel

class CollectionAdapter(
    private val onItemClick: (CollectionItemWithModel) -> Unit
) : ListAdapter<CollectionItemWithModel, CollectionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CollectionItemWithModel) {
            binding.apply {
                tvModelName.text = item.modelName
                tvSeries.text = item.modelSeries
                tvYear.text = item.modelYear.toString()
                tvQuantity.text = "x${item.collectionItem.quantity}"
                tvCondition.text = item.collectionItem.condition

                // Set favorite icon
                btnFavorite.setImageResource(
                    if (item.collectionItem.isFavorite) {
                        android.R.drawable.btn_star_big_on
                    } else {
                        android.R.drawable.btn_star_big_off
                    }
                )

                // Click listeners
                root.setOnClickListener {
                    onItemClick(item)
                }

                btnFavorite.setOnClickListener {
                    // This would trigger a callback to the ViewModel
                    // For now, just visual feedback
                }

                // Format acquisition date
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                tvAcquiredDate.text = "Added: ${dateFormat.format(java.util.Date(item.collectionItem.acquiredDate))}"
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CollectionItemWithModel>() {
            override fun areItemsTheSame(
                oldItem: CollectionItemWithModel,
                newItem: CollectionItemWithModel
            ): Boolean {
                return oldItem.collectionItem.id == newItem.collectionItem.id
            }

            override fun areContentsTheSame(
                oldItem: CollectionItemWithModel,
                newItem: CollectionItemWithModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}