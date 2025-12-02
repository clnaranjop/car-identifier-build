package com.hotwheels.identifier.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheels.identifier.data.entities.HotWheelModel
import com.hotwheels.identifier.data.entities.UserCollection
import com.hotwheels.identifier.data.repository.HotWheelsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HotWheelsRepository

    private val _hotWheelModel = MutableStateFlow<HotWheelModel?>(null)
    val hotWheelModel: StateFlow<HotWheelModel?> = _hotWheelModel.asStateFlow()

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus: StateFlow<String?> = _saveStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        repository = HotWheelsRepository(application)
    }

    fun loadHotWheelModel(modelId: String) {
        viewModelScope.launch {
            try {
                val model = repository.getModelById(modelId)
                _hotWheelModel.value = model
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load model details: ${e.message}"
            }
        }
    }

    fun saveToCollection(modelId: String, imagePath: String? = null) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ResultViewModel", "💾 Saving model to collection: $modelId with image: $imagePath")

                // Copy image from cache to permanent storage
                val permanentImagePath = imagePath?.let { copyImageToPermanentStorage(it, modelId) }
                android.util.Log.d("ResultViewModel", "📁 Permanent image path: $permanentImagePath")

                // Check if already in collection
                val existingItems = repository.getCollectionItemsByModelId(modelId)
                android.util.Log.d("ResultViewModel", "Existing items found: ${existingItems.size}")

                if (existingItems.isNotEmpty()) {
                    // Update quantity and image if not set
                    val existingItem = existingItems.first()
                    val updatedItem = existingItem.copy(
                        quantity = existingItem.quantity + 1,
                        acquiredDate = System.currentTimeMillis(),
                        imagesPaths = existingItem.imagesPaths ?: permanentImagePath  // Update image if not set
                    )
                    repository.updateCollectionItem(updatedItem)
                    android.util.Log.d("ResultViewModel", "✅ Updated quantity to ${updatedItem.quantity}")
                    _saveStatus.value = "Added another one to your collection!"
                } else {
                    // Add new item with permanent image path
                    val newItem = UserCollection(
                        hotWheelModelId = modelId,
                        quantity = 1,
                        condition = "Mint",
                        acquiredDate = System.currentTimeMillis(),
                        imagesPaths = permanentImagePath  // Save permanent path
                    )
                    val insertedId = repository.insertCollectionItem(newItem)
                    android.util.Log.d("ResultViewModel", "✅ Inserted new item with ID: $insertedId and image: $permanentImagePath")
                    _saveStatus.value = "Added to your collection!"
                }

            } catch (e: Exception) {
                android.util.Log.e("ResultViewModel", "❌ Error saving to collection", e)
                _errorMessage.value = "Failed to save to collection: ${e.message}"
            }
        }
    }

    private fun copyImageToPermanentStorage(sourcePath: String, modelId: String): String? {
        return try {
            val sourceFile = java.io.File(sourcePath)
            if (!sourceFile.exists()) {
                android.util.Log.e("ResultViewModel", "❌ Source file does not exist: $sourcePath")
                return null
            }

            // Create collection_images directory in app's files directory
            val context = getApplication<android.app.Application>()
            val collectionImagesDir = java.io.File(context.filesDir, "collection_images")
            if (!collectionImagesDir.exists()) {
                collectionImagesDir.mkdirs()
            }

            // Create destination file with unique name based on modelId and timestamp
            val timestamp = System.currentTimeMillis()
            val destFile = java.io.File(collectionImagesDir, "${modelId}_${timestamp}.jpg")

            // Copy file
            sourceFile.copyTo(destFile, overwrite = true)
            android.util.Log.d("ResultViewModel", "✅ Image copied to: ${destFile.absolutePath}")

            destFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ResultViewModel", "❌ Error copying image", e)
            null
        }
    }

    fun clearSaveStatus() {
        _saveStatus.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}