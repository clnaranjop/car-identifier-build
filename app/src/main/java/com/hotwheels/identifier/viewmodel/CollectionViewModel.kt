package com.hotwheels.identifier.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheels.identifier.data.entities.UserCollection
import com.hotwheels.identifier.data.repository.HotWheelsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CollectionStats(
    val totalItems: Int,
    val uniqueModels: Int,
    val favorites: Int
)

data class CollectionItemWithModel(
    val collectionItem: UserCollection,
    val modelName: String,
    val modelSeries: String,
    val modelYear: Int
)

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HotWheelsRepository

    private val _collectionItems = MutableStateFlow<List<CollectionItemWithModel>>(emptyList())
    val collectionItems: StateFlow<List<CollectionItemWithModel>> = _collectionItems.asStateFlow()

    private val _stats = MutableStateFlow(CollectionStats(0, 0, 0))
    val stats: StateFlow<CollectionStats> = _stats.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        repository = HotWheelsRepository(application)

        loadCollection()
        loadStats()
    }

    private fun loadCollection() {
        viewModelScope.launch {
            try {
                android.util.Log.d("CollectionViewModel", "🔄 Starting to load collection...")
                repository.getAllCollectionItems().combine(
                    repository.getAllModels()
                ) { collectionItems, models ->
                    android.util.Log.d("CollectionViewModel", "📦 Collection items from repo: ${collectionItems.size}")
                    android.util.Log.d("CollectionViewModel", "📚 Total models in database: ${models.size}")

                    collectionItems.mapNotNull { item ->
                        android.util.Log.d("CollectionViewModel", "Processing item: modelId=${item.hotWheelModelId}")
                        val model = models.find { it.id == item.hotWheelModelId }
                        if (model == null) {
                            android.util.Log.e("CollectionViewModel", "❌ Model not found for id: ${item.hotWheelModelId}")
                        } else {
                            android.util.Log.d("CollectionViewModel", "✅ Found model: ${model.name}")
                        }
                        model?.let {
                            CollectionItemWithModel(
                                collectionItem = item,
                                modelName = it.name,
                                modelSeries = it.series,
                                modelYear = it.year
                            )
                        }
                    }
                }.collect { items ->
                    android.util.Log.d("CollectionViewModel", "✅ Emitting ${items.size} collection items")
                    items.forEach { item ->
                        android.util.Log.d("CollectionViewModel", "  - ${item.modelName} (${item.modelYear})")
                    }
                    _collectionItems.value = items
                }
            } catch (e: Exception) {
                android.util.Log.e("CollectionViewModel", "❌ Error loading collection", e)
                _errorMessage.value = "Failed to load collection: ${e.message}"
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val totalItems = repository.getTotalQuantity()
                val uniqueModels = repository.getUniqueModelsCount()

                // Count favorites
                repository.getFavoriteItems().collect { favoriteItems ->
                    val favoritesCount = favoriteItems.size

                    _stats.value = CollectionStats(
                        totalItems = totalItems,
                        uniqueModels = uniqueModels,
                        favorites = favoritesCount
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load stats: ${e.message}"
            }
        }
    }

    fun updateCollectionItem(collectionItem: UserCollection) {
        viewModelScope.launch {
            try {
                repository.updateCollectionItem(collectionItem)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update item: ${e.message}"
            }
        }
    }

    fun toggleFavorite(collectionItem: UserCollection) {
        viewModelScope.launch {
            try {
                val updatedItem = collectionItem.copy(isFavorite = !collectionItem.isFavorite)
                repository.updateCollectionItem(updatedItem)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update favorite: ${e.message}"
            }
        }
    }

    fun removeFromCollection(collectionItem: UserCollection) {
        viewModelScope.launch {
            try {
                repository.deleteCollectionItem(collectionItem)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove item: ${e.message}"
            }
        }
    }

    fun insertCollectionItem(collectionItem: UserCollection) {
        viewModelScope.launch {
            try {
                repository.insertCollectionItem(collectionItem)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to insert item: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}