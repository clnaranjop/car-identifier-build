package com.hotwheels.identifier.data

import com.hotwheels.identifier.data.entities.HotWheelModel
import com.hotwheels.identifier.data.entities.IdentificationResult
import com.hotwheels.identifier.data.entities.UserCollection
import kotlinx.coroutines.flow.Flow

class SimpleRepository(private val storage: SimpleDataStorage) {

    // Model operations
    suspend fun insertModel(model: HotWheelModel): Long = storage.insertModel(model)

    fun getAllModels(): Flow<List<HotWheelModel>> = storage.models

    suspend fun getModelsCount(): Int = storage.getModelsCount()

    // Result operations
    suspend fun insertResult(result: IdentificationResult): Long = storage.insertResult(result)

    suspend fun getResultsCount(): Int = storage.getResultsCount()

    // Collection operations
    suspend fun getUniqueModelsCount(): Int = storage.getUniqueModelsCount()

    suspend fun insertCollection(collection: UserCollection): Long = storage.insertCollection(collection)
}