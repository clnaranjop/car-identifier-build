package com.hotwheels.identifier.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.hotwheels.identifier.data.entities.HotWheelModel
import com.hotwheels.identifier.data.entities.IdentificationResult
import com.hotwheels.identifier.data.entities.UserCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class HotWheelsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hotwheels_db", Context.MODE_PRIVATE)
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()
    private val tag = "HotWheelsRepository"

    // StateFlows for reactive data
    private val _allModels = MutableStateFlow<List<HotWheelModel>>(emptyList())
    private val _allResults = MutableStateFlow<List<IdentificationResult>>(emptyList())
    private val _allCollectionItems = MutableStateFlow<List<UserCollection>>(emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        // ALWAYS load from assets to ensure we have the complete database
        Log.d(tag, "🔄 Loading models from assets (forcing fresh load)...")
        val models = loadModelsFromAssets()

        // Log some sample model names to check corruption
        models.take(3).forEach { model ->
            Log.d(tag, "Loaded model: id='${model.id}', name='${model.name}'")
        }

        Log.d(tag, "✅ Total models loaded: ${models.size}")
        _allModels.value = models

        // Load results
        val resultsJson = prefs.getString("results", "[]")
        val resultType = object : TypeToken<List<IdentificationResult>>() {}.type
        _allResults.value = gson.fromJson(resultsJson, resultType) ?: emptyList()

        // Load collection items
        val collectionJson = prefs.getString("collection", "[]")
        val collectionType = object : TypeToken<List<UserCollection>>() {}.type
        _allCollectionItems.value = gson.fromJson(collectionJson, collectionType) ?: emptyList()
    }

    private fun loadModelsFromAssets(): List<HotWheelModel> {
        return try {
            Log.d(tag, "📂 Loading from assets/hotwheels_models.json...")
            val inputStream = context.assets.open("hotwheels_models.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            Log.d(tag, "📄 JSON file read: ${jsonString.length} characters")

            val jsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)
            val totalModels = jsonObject.get("total_models")?.asInt ?: 0
            val modelsArray = jsonObject.getAsJsonArray("models")

            Log.d(tag, "📊 JSON metadata - Total models: $totalModels, Array size: ${modelsArray.size()}")

            val models = mutableListOf<HotWheelModel>()
            for (i in 0 until modelsArray.size()) {
                try {
                    val obj = modelsArray.get(i).asJsonObject

                    // Helper function to safely get string or return default (handles JsonNull)
                    fun getStringOrDefault(key: String, default: String): String {
                        val element = obj.get(key)
                        return if (element != null && !element.isJsonNull) {
                            element.asString
                        } else {
                            default
                        }
                    }

                    models.add(HotWheelModel(
                        id = obj.get("id").asString,
                        name = obj.get("name").asString,
                        series = getStringOrDefault("series", "Unknown"),
                        year = obj.get("year").asInt,
                        category = getStringOrDefault("category", "Unknown"),
                        manufacturer = getStringOrDefault("manufacturer", "Mattel"),
                        rarity = getStringOrDefault("rarity", "Common"),
                        localImagePath = if (obj.has("localImagePath") && !obj.get("localImagePath").isJsonNull) {
                            obj.get("localImagePath").asString
                        } else {
                            null
                        }
                    ))

                    if (i % 500 == 0) {
                        Log.d(tag, "   Processing model $i/${modelsArray.size()}...")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing model $i: ${e.message}")
                }
            }

            Log.d(tag, "✅ Loaded ${models.size} models from assets")
            models
        } catch (e: Exception) {
            Log.e(tag, "❌ Error loading models from assets: ${e.message}", e)
            emptyList()
        }
    }

    private fun saveModels() {
        val json = gson.toJson(_allModels.value)
        prefs.edit().putString("models", json).apply()
    }

    private fun saveResults() {
        val json = gson.toJson(_allResults.value)
        prefs.edit().putString("results", json).apply()
    }

    private fun saveCollection() {
        val json = gson.toJson(_allCollectionItems.value)
        prefs.edit().putString("collection", json).apply()
    }

    // HotWheel Models
    fun getAllModels(): Flow<List<HotWheelModel>> = _allModels.asStateFlow()

    suspend fun getModelById(id: String): HotWheelModel? {
        val model = _allModels.value.find { it.id == id }
        Log.d(tag, "getModelById($id): Found model with name: '${model?.name}'")

        // Fix for specific corrupted model hw_2000_028
        if (id == "hw_2000_028" && model != null && model.name == "1979 Ford Truck") {
            Log.w(tag, "🔧 Detected corrupted model hw_2000_028, fixing name from '${model.name}' to 'Ford GT-90'")
            val correctedModel = model.copy(name = "Ford GT-90")
            updateModel(correctedModel)
            return correctedModel
        }

        return model
    }

    suspend fun searchModels(query: String): List<HotWheelModel> {
        if (query.isBlank()) return emptyList()

        val searchTerms = query.trim().split("\\s+".toRegex()) // Split by whitespace

        return _allModels.value.filter { model ->
            // Match if ANY search term is found in name, series, or category
            searchTerms.any { term ->
                model.name.contains(term, ignoreCase = true) ||
                model.series.contains(term, ignoreCase = true) ||
                model.category.contains(term, ignoreCase = true) ||
                model.id.contains(term, ignoreCase = true)
            }
        }.sortedByDescending { model ->
            // Score results: prioritize matches in name over series/category
            var score = 0
            searchTerms.forEach { term ->
                if (model.name.contains(term, ignoreCase = true)) score += 10
                if (model.series.contains(term, ignoreCase = true)) score += 5
                if (model.category.contains(term, ignoreCase = true)) score += 2
                // Exact match gets bonus points
                if (model.name.equals(query, ignoreCase = true)) score += 100
            }
            score
        }
    }

    suspend fun getModelsBySeries(series: String): List<HotWheelModel> =
        _allModels.value.filter { it.series == series }

    suspend fun getModelsByYear(year: Int): List<HotWheelModel> =
        _allModels.value.filter { it.year == year }

    suspend fun getAllSeries(): List<String> =
        _allModels.value.map { it.series }.distinct()

    suspend fun getAllYears(): List<Int> =
        _allModels.value.map { it.year }.distinct()

    suspend fun insertModel(model: HotWheelModel) {
        val currentList = _allModels.value.toMutableList()
        currentList.add(model)
        _allModels.value = currentList
        saveModels()
    }

    suspend fun insertModels(models: List<HotWheelModel>) {
        Log.d(tag, "insertModels: Adding ${models.size} models")
        models.take(3).forEach { model ->
            Log.d(tag, "Inserting model: id='${model.id}', name='${model.name}'")
        }

        val currentList = _allModels.value.toMutableList()
        val sizeBefore = currentList.size
        currentList.addAll(models)
        _allModels.value = currentList

        Log.d(tag, "📊 Models before: $sizeBefore, after: ${_allModels.value.size}")
        Log.d(tag, "📄 Saving to SharedPreferences...")
        saveModels()

        // Verificar que se guardó correctamente
        val savedCount = getModelsCount()
        Log.d(tag, "✅ Verified count after save: $savedCount")
    }

    suspend fun updateModel(model: HotWheelModel) {
        val currentList = _allModels.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == model.id }
        if (index >= 0) {
            currentList[index] = model
            _allModels.value = currentList
            saveModels()
        }
    }

    suspend fun deleteModel(model: HotWheelModel) {
        val currentList = _allModels.value.toMutableList()
        currentList.removeIf { it.id == model.id }
        _allModels.value = currentList
        saveModels()
    }

    suspend fun getModelsCount(): Int {
        val count = _allModels.value.size
        Log.d(tag, "🔢 getModelsCount() returning: $count")
        return count
    }

    // Identification Results
    fun getAllResults(): Flow<List<IdentificationResult>> = _allResults.asStateFlow()

    suspend fun getResultById(id: Long): IdentificationResult? =
        _allResults.value.find { it.id == id }

    suspend fun getResultsByModelId(modelId: String): List<IdentificationResult> =
        _allResults.value.filter { it.hotWheelModelId == modelId }

    suspend fun getRecentResults(limit: Int): List<IdentificationResult> =
        _allResults.value.sortedByDescending { it.dateIdentified }.take(limit)

    suspend fun insertResult(result: IdentificationResult): Long {
        val newId = (_allResults.value.maxOfOrNull { it.id } ?: 0) + 1
        val newResult = result.copy(id = newId)
        val currentList = _allResults.value.toMutableList()
        currentList.add(newResult)
        _allResults.value = currentList
        saveResults()
        return newId
    }

    suspend fun updateResult(result: IdentificationResult) {
        val currentList = _allResults.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == result.id }
        if (index >= 0) {
            currentList[index] = result
            _allResults.value = currentList
            saveResults()
        }
    }

    suspend fun deleteResult(result: IdentificationResult) {
        val currentList = _allResults.value.toMutableList()
        currentList.removeIf { it.id == result.id }
        _allResults.value = currentList
        saveResults()
    }

    suspend fun getResultsCount(): Int = _allResults.value.size

    suspend fun getConfirmedResultsCount(): Int =
        _allResults.value.count { it.userConfirmed }

    // User Collection
    fun getAllCollectionItems(): Flow<List<UserCollection>> = _allCollectionItems.asStateFlow()

    suspend fun getCollectionItemById(id: Long): UserCollection? =
        _allCollectionItems.value.find { it.id == id }

    suspend fun getCollectionItemsByModelId(modelId: String): List<UserCollection> =
        _allCollectionItems.value.filter { it.hotWheelModelId == modelId }

    fun getFavoriteItems(): Flow<List<UserCollection>> =
        MutableStateFlow(_allCollectionItems.value.filter { it.isFavorite }).asStateFlow()

    suspend fun getTotalQuantity(): Int =
        _allCollectionItems.value.sumOf { it.quantity }

    suspend fun getUniqueModelsCount(): Int =
        _allCollectionItems.value.map { it.hotWheelModelId }.distinct().size

    suspend fun getItemsByCondition(condition: String): List<UserCollection> =
        _allCollectionItems.value.filter { it.condition == condition }

    suspend fun insertCollectionItem(item: UserCollection): Long {
        val newId = (_allCollectionItems.value.maxOfOrNull { it.id } ?: 0) + 1
        val newItem = item.copy(id = newId)
        val currentList = _allCollectionItems.value.toMutableList()
        currentList.add(newItem)
        _allCollectionItems.value = currentList
        saveCollection()
        return newId
    }

    suspend fun updateCollectionItem(item: UserCollection) {
        val currentList = _allCollectionItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            currentList[index] = item
            _allCollectionItems.value = currentList
            saveCollection()
        }
    }

    suspend fun deleteCollectionItem(item: UserCollection) {
        val currentList = _allCollectionItems.value.toMutableList()
        currentList.removeIf { it.id == item.id }
        _allCollectionItems.value = currentList
        saveCollection()
    }

    suspend fun deleteItemsByModelId(modelId: String) {
        val currentList = _allCollectionItems.value.toMutableList()
        currentList.removeIf { it.hotWheelModelId == modelId }
        _allCollectionItems.value = currentList
        saveCollection()
    }

    suspend fun clearAllCollectionItems() {
        _allCollectionItems.value = emptyList()
        saveCollection()
    }

    /**
     * Fixes corrupted model names in the database
     */
    suspend fun fixCorruptedModelNames(): Int {
        var fixedCount = 0
        val currentList = _allModels.value.toMutableList()

        for (i in currentList.indices) {
            val model = currentList[i]
            var needsUpdate = false
            var correctedName = model.name

            // Fix known corrupted names
            when {
                model.name == "1979 Ford Truck" && model.id == "hw_2000_028" -> {
                    correctedName = "Ford GT-90"
                    needsUpdate = true
                }
                model.name.contains("1979 Ford Truck", ignoreCase = true) -> {
                    correctedName = "Ford GT-90"
                    needsUpdate = true
                }
                // Add more specific corrections here as needed
            }

            if (needsUpdate) {
                currentList[i] = model.copy(name = correctedName)
                fixedCount++
                Log.d(tag, "🔧 Fixed model ${model.id}: '${model.name}' → '$correctedName'")
            }
        }

        if (fixedCount > 0) {
            _allModels.value = currentList
            saveModels()
            Log.d(tag, "✅ Fixed $fixedCount corrupted model names")
        }

        return fixedCount
    }
}