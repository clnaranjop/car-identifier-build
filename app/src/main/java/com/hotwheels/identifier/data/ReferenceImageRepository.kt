package com.hotwheels.identifier.data

import android.content.Context
import android.util.Log
import java.io.IOException

/**
 * Repositorio para acceder a las imágenes de referencia desde assets.
 */
class ReferenceImageRepository(private val context: Context) {

    private val tag = "ReferenceImageRepository"
    private val baseAssetPath = "reference_images"

    /**
     * Carga todas las imágenes de referencia disponibles.
     */
    fun loadAllReferenceImages(): List<ReferenceImage> {
        val images = mutableListOf<ReferenceImage>()

        try {
            val yearFolders = context.assets.list(baseAssetPath) ?: emptyArray()

            for (yearFolder in yearFolders) {
                val yearPath = "$baseAssetPath/$yearFolder"
                val imageFiles = context.assets.list(yearPath) ?: emptyArray()

                for (imageFile in imageFiles) {
                    if (imageFile.endsWith(".jpg", ignoreCase = true)) {
                        val image = ReferenceImage(
                            fileName = imageFile,
                            displayName = ReferenceImage.extractDisplayName(imageFile),
                            year = yearFolder,
                            assetPath = "$yearPath/$imageFile"
                        )
                        images.add(image)
                    }
                }
            }

            Log.d(tag, "Loaded ${images.size} reference images")
        } catch (e: IOException) {
            Log.e(tag, "Error loading reference images", e)
        }

        return images.sortedBy { it.fileName }
    }

    /**
     * Carga imágenes agrupadas por año.
     */
    fun loadImagesByYear(): Map<String, List<ReferenceImage>> {
        val allImages = loadAllReferenceImages()
        return allImages.groupBy { it.year }.toSortedMap()
    }

    /**
     * Obtiene lista de años disponibles.
     */
    fun getAvailableYears(): List<String> {
        return try {
            val years = context.assets.list(baseAssetPath)?.toList() ?: emptyList()
            years.sorted()
        } catch (e: IOException) {
            Log.e(tag, "Error getting available years", e)
            emptyList()
        }
    }
}
