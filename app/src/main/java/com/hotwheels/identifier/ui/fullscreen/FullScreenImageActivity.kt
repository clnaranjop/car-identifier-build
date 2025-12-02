package com.hotwheels.identifier.ui.fullscreen

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.hotwheels.identifier.databinding.ActivityFullscreenImageBinding
import com.github.chrisbanes.photoview.PhotoView

/**
 * Activity para ver una imagen en pantalla completa con zoom
 */
class FullScreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenImageBinding

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_MODEL_NAME = "extra_model_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable fullscreen mode
        enableFullscreen()

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val modelName = intent.getStringExtra(EXTRA_MODEL_NAME)

        // Set model name if provided
        modelName?.let {
            binding.tvModelName.text = it
        }

        // Load image from assets
        imagePath?.let {
            try {
                val inputStream = assets.open(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap != null) {
                    binding.photoView.setImageBitmap(bitmap)
                } else {
                    // Fallback to placeholder
                    binding.photoView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to placeholder
                binding.photoView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // Close on click
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Toggle UI visibility on image tap
        binding.photoView.setOnClickListener {
            toggleUIVisibility()
        }
    }

    private fun enableFullscreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun toggleUIVisibility() {
        val isVisible = binding.overlayContainer.visibility == View.VISIBLE
        binding.overlayContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
    }
}
