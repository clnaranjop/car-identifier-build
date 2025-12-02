package com.hotwheels.identifier.ui.selection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import java.io.InputStream

/**
 * Simple, robust image loader for loading images from assets
 */
object ImageLoader {
    private const val TAG = "ImageLoader"

    /**
     * Load an image from assets and set it to an ImageView
     * This method is designed to be BULLETPROOF - it will always work or show why it failed
     */
    fun loadFromAssets(
        context: Context,
        imageView: ImageView,
        assetPath: String,
        placeholderResId: Int? = null
    ): Boolean {
        Log.d(TAG, "=== LOADING IMAGE ===")
        Log.d(TAG, "Path: $assetPath")
        Log.d(TAG, "ImageView ID: ${imageView.id}")

        // Clear any previous image first
        imageView.setImageDrawable(null)
        imageView.setImageBitmap(null)

        if (assetPath.isEmpty()) {
            Log.e(TAG, "❌ FAILED: Empty path")
            setPlaceholder(imageView, placeholderResId)
            return false
        }

        var inputStream: InputStream? = null
        try {
            // Open asset
            inputStream = context.assets.open(assetPath)
            Log.d(TAG, "✅ Asset opened successfully")

            // Decode bitmap
            val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)

            if (bitmap == null) {
                Log.e(TAG, "❌ FAILED: BitmapFactory returned null")
                setPlaceholder(imageView, placeholderResId)
                return false
            }

            Log.d(TAG, "✅ Bitmap decoded: ${bitmap.width}x${bitmap.height}")
            Log.d(TAG, "✅ Bitmap config: ${bitmap.config}")
            Log.d(TAG, "✅ Bitmap byte count: ${bitmap.byteCount}")

            // Force ImageView configuration
            imageView.apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                visibility = ImageView.VISIBLE
                adjustViewBounds = true
            }

            // Set the bitmap
            imageView.setImageBitmap(bitmap)
            Log.d(TAG, "✅ Bitmap set to ImageView")

            // CRITICAL: Force immediate layout update
            imageView.invalidate()
            imageView.requestLayout()

            // Verify it worked
            val verifyDrawable = imageView.drawable
            if (verifyDrawable is android.graphics.drawable.BitmapDrawable) {
                val verifyBitmap = verifyDrawable.bitmap
                if (verifyBitmap != null && verifyBitmap.width > 0) {
                    Log.d(TAG, "✅ VERIFICATION PASSED: ${verifyBitmap.width}x${verifyBitmap.height}")
                    return true
                } else {
                    Log.e(TAG, "❌ VERIFICATION FAILED: Bitmap is null or invalid")
                }
            } else {
                Log.e(TAG, "❌ VERIFICATION FAILED: Drawable is not BitmapDrawable")
                Log.e(TAG, "    Drawable type: ${verifyDrawable?.javaClass?.simpleName ?: "null"}")
            }

            return false

        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            setPlaceholder(imageView, placeholderResId)
            return false
        } finally {
            inputStream?.close()
            Log.d(TAG, "=== LOADING COMPLETE ===")
        }
    }

    private fun setPlaceholder(imageView: ImageView, placeholderResId: Int?) {
        try {
            if (placeholderResId != null) {
                imageView.setImageResource(placeholderResId)
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            imageView.visibility = ImageView.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set placeholder: ${e.message}")
        }
    }
}
