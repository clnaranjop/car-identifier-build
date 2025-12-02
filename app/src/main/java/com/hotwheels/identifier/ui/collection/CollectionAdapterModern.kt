package com.hotwheels.identifier.ui.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hotwheels.identifier.R
import com.hotwheels.identifier.databinding.ItemCollectionModernBinding
import com.hotwheels.identifier.viewmodel.CollectionItemWithModel
import java.text.SimpleDateFormat
import java.util.*

class CollectionAdapterModern(
    private val onItemClick: (CollectionItemWithModel) -> Unit,
    private val onFavoriteClick: (CollectionItemWithModel) -> Unit
    // Removed onViewPricesClick - shopping not available from collection
) : ListAdapter<CollectionItemWithModel, CollectionAdapterModern.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionModernBinding.inflate(
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
        private val binding: ItemCollectionModernBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CollectionItemWithModel) {
            binding.apply {
                // Model name
                tvModelName.text = item.modelName

                // Year
                tvYear.text = item.modelYear.toString()

                // Series
                tvSeries.text = item.modelSeries ?: "Unknown Series"

                // Load thumbnail with Glide
                loadThumbnail(item)

                // Show/hide TH badge
                badgeTH.visibility = if (item.collectionItem.isTreasureHunt && !item.collectionItem.isSuperTreasureHunt) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Show/hide STH badge (priority over TH)
                badgeSTH.visibility = if (item.collectionItem.isSuperTreasureHunt) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Rarity stars
                displayRarityStars(item.collectionItem.rarityLevel)

                // Quantity badge (only if > 1)
                if (item.collectionItem.quantity > 1) {
                    tvQuantity.visibility = View.VISIBLE
                    tvQuantity.text = "x${item.collectionItem.quantity}"
                } else {
                    tvQuantity.visibility = View.GONE
                }

                // Favorite icon
                btnFavorite.setImageResource(
                    if (item.collectionItem.isFavorite) {
                        R.drawable.ic_favorite_filled
                    } else {
                        R.drawable.ic_favorite_border
                    }
                )

                // Acquisition date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvAcquiredDate.text = "Added: ${dateFormat.format(Date(item.collectionItem.acquiredDate))}"

                // Click listeners
                root.setOnClickListener {
                    onItemClick(item)
                }

                btnFavorite.setOnClickListener {
                    onFavoriteClick(item)
                }

                // Removed btnViewPrices click listener - shopping not available from collection
            }
        }

        private fun loadThumbnail(item: CollectionItemWithModel) {
            val context = binding.root.context

            // Load from user's saved image path if available
            val imagePath = item.collectionItem.imagesPaths

            android.util.Log.d("CollectionAdapter", "Loading thumbnail for ${item.collectionItem.hotWheelModelId}, path: $imagePath")

            if (imagePath != null) {
                // Load from File for local paths
                val imageFile = java.io.File(imagePath)
                android.util.Log.d("CollectionAdapter", "Image file exists: ${imageFile.exists()}, canRead: ${imageFile.canRead()}")

                Glide.with(context)
                    .load(imageFile)
                    .centerCrop()
                    .placeholder(R.drawable.icon_car_orange)
                    .error(R.drawable.icon_car_orange)
                    .into(binding.imgThumbnail)
            } else {
                // No image saved, show placeholder
                binding.imgThumbnail.setImageResource(R.drawable.icon_car_orange)
            }
        }

        private fun displayRarityStars(rarityLevel: Int) {
            val starContainer = binding.layoutRarity
            starContainer.removeAllViews()

            // Ensure rarityLevel is between 1 and 5
            val stars = rarityLevel.coerceIn(1, 5)

            // Add filled stars
            repeat(stars) {
                val starView = createStarImageView(filled = true)
                starContainer.addView(starView)
            }

            // Add empty stars to complete 5 stars
            repeat(5 - stars) {
                val starView = createStarImageView(filled = false)
                starContainer.addView(starView)
            }
        }

        private fun createStarImageView(filled: Boolean): ImageView {
            val context = binding.root.context
            return ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.star_size),
                    context.resources.getDimensionPixelSize(R.dimen.star_size)
                ).apply {
                    marginEnd = context.resources.getDimensionPixelSize(R.dimen.star_margin)
                }
                setImageResource(
                    if (filled) R.drawable.ic_star_filled else R.drawable.ic_star_empty
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
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
