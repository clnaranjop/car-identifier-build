package com.hotwheels.identifier.ui.exploration

import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hotwheels.identifier.R
import com.hotwheels.identifier.data.ReferenceImage
import com.hotwheels.identifier.databinding.ItemReferenceImageBinding
import com.hotwheels.identifier.ui.fullscreen.FullScreenImageActivity

/**
 * Adapter para mostrar imágenes de referencia en modo Exploración.
 */
class ExplorationAdapter(
    private val assets: AssetManager,
    private val onRotateClicked: (ReferenceImage) -> Unit,
    private val onAddToCollectionClicked: (ReferenceImage) -> Unit,
    private val onReplacementFlagChanged: (ReferenceImage, Boolean) -> Unit
) : RecyclerView.Adapter<ExplorationAdapter.ImageViewHolder>() {

    private var images = listOf<ReferenceImage>()
    private val flaggedImages = mutableSetOf<String>()
    var developerModeEnabled = false // Developer mode flag

    fun submitList(newImages: List<ReferenceImage>) {
        images = newImages
        notifyDataSetChanged()
    }

    fun updateFlagStatus(fileName: String, isFlagged: Boolean) {
        if (isFlagged) {
            flaggedImages.add(fileName)
        } else {
            flaggedImages.remove(fileName)
        }
    }

    fun isFlagged(fileName: String): Boolean {
        return flaggedImages.contains(fileName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemReferenceImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    // Disable ViewHolder recycling to prevent checkbox state issues
    override fun getItemViewType(position: Int): Int = position

    inner class ImageViewHolder(
        private val binding: ItemReferenceImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: ReferenceImage) {
            // Set text info
            binding.tvModelName.text = image.displayName
            binding.tvYear.text = image.year

            // Show rotation badge if image is rotated
            if (image.currentRotation > 0) {
                binding.tvRotationBadge.visibility = View.VISIBLE
                binding.tvRotationBadge.text = "${image.currentRotation}°"
            } else {
                binding.tvRotationBadge.visibility = View.GONE
            }

            // Load and display image with current rotation
            loadImage(image)

            // Set checkbox state (remove listener first to avoid triggering during recycling)
            binding.checkboxNeedsReplacement.setOnCheckedChangeListener(null)
            val isFlagged = isFlagged(image.fileName)
            binding.checkboxNeedsReplacement.isChecked = isFlagged

            // Set checkbox listener
            binding.checkboxNeedsReplacement.setOnCheckedChangeListener { _, isChecked ->
                updateFlagStatus(image.fileName, isChecked)
                onReplacementFlagChanged(image, isChecked)
            }

            // Hide checkbox and rotate button if developer mode is disabled
            binding.checkboxNeedsReplacement.visibility = if (developerModeEnabled) View.VISIBLE else View.GONE
            binding.btnRotate.visibility = if (developerModeEnabled) View.VISIBLE else View.GONE

            // Set button listeners
            binding.btnRotate.setOnClickListener {
                onRotateClicked(image)
            }

            binding.btnAddToCollection.setOnClickListener {
                onAddToCollectionClicked(image)
            }

            // Add click listener to image to open fullscreen viewer
            binding.imageView.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, FullScreenImageActivity::class.java)
                intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_PATH, image.assetPath)
                intent.putExtra(FullScreenImageActivity.EXTRA_MODEL_NAME, image.displayName)
                context.startActivity(intent)
            }
        }

        private fun loadImage(image: ReferenceImage) {
            try {
                // Load bitmap from assets
                val inputStream = assets.open(image.assetPath)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                // Apply rotation if needed
                val rotatedBitmap = if (image.currentRotation > 0) {
                    rotateBitmap(originalBitmap, image.currentRotation.toFloat())
                } else {
                    originalBitmap
                }

                // Display in ImageView
                binding.imageView.setImageBitmap(rotatedBitmap)

            } catch (e: Exception) {
                e.printStackTrace()
                // Set placeholder on error
                binding.imageView.setImageResource(R.drawable.icon_logo)
            }
        }

        private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
        }
    }
}
