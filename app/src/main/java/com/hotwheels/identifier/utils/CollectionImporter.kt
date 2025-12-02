package com.hotwheels.identifier.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hotwheels.identifier.data.entities.UserCollection
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class CollectionImporter(private val context: Context) {

    private val tag = "CollectionImporter"

    /**
     * Import collection from CSV file
     */
    fun importFromCSV(uri: Uri, collectionName: String, ownerName: String?): Result<List<UserCollection>> {
        return try {
            val items = mutableListOf<UserCollection>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    reader.readLine()

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { csvLine ->
                            try {
                                val item = parseCsvLine(csvLine, collectionName, ownerName)
                                items.add(item)
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to parse line: $csvLine", e)
                            }
                        }
                    }
                }
            }

            Log.d(tag, "✅ CSV import successful: ${items.size} items")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(tag, "❌ CSV import failed", e)
            Result.failure(e)
        }
    }

    /**
     * Import collection from JSON file
     */
    fun importFromJSON(uri: Uri, collectionName: String, ownerName: String?): Result<List<UserCollection>> {
        return try {
            val items = mutableListOf<UserCollection>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val jsonRoot = JSONObject(jsonString)
                val jsonArray = jsonRoot.getJSONArray("collection")

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    try {
                        val item = parseJsonObject(jsonObject, collectionName, ownerName)
                        items.add(item)
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to parse JSON object at index $i", e)
                    }
                }
            }

            Log.d(tag, "✅ JSON import successful: ${items.size} items")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(tag, "❌ JSON import failed", e)
            Result.failure(e)
        }
    }

    /**
     * Parse CSV line into UserCollection
     */
    private fun parseCsvLine(line: String, collectionName: String, ownerName: String?): UserCollection {
        val fields = parseCsvFields(line)

        return UserCollection(
            id = 0, // Will be auto-generated
            hotWheelModelId = fields.getOrNull(0) ?: "",
            quantity = fields.getOrNull(6)?.toIntOrNull() ?: 1,
            condition = fields.getOrNull(7) ?: "Mint",
            acquiredDate = fields.getOrNull(8)?.toLongOrNull() ?: System.currentTimeMillis(),
            isFavorite = fields.getOrNull(9)?.toBoolean() ?: false,
            isTreasureHunt = fields.getOrNull(10)?.toBoolean() ?: false,
            isSuperTreasureHunt = fields.getOrNull(11)?.toBoolean() ?: false,
            rarityLevel = fields.getOrNull(12)?.toIntOrNull() ?: 1,
            variation = fields.getOrNull(13)?.takeIf { it.isNotBlank() },
            estimatedValue = fields.getOrNull(14)?.toFloatOrNull(),
            imagesPaths = null, // Don't import images
            collectionName = collectionName,
            collectionOwner = ownerName,
            isImported = true
        )
    }

    /**
     * Parse JSON object into UserCollection
     */
    private fun parseJsonObject(json: JSONObject, collectionName: String, ownerName: String?): UserCollection {
        return UserCollection(
            id = 0, // Will be auto-generated
            hotWheelModelId = json.getString("modelId"),
            quantity = json.optInt("quantity", 1),
            condition = json.optString("condition", "Mint"),
            acquiredDate = json.optLong("acquiredDate", System.currentTimeMillis()),
            isFavorite = json.optBoolean("isFavorite", false),
            isTreasureHunt = json.optBoolean("isTreasureHunt", false),
            isSuperTreasureHunt = json.optBoolean("isSuperTreasureHunt", false),
            rarityLevel = json.optInt("rarityLevel", 1),
            variation = json.optString("variation").takeIf { it.isNotBlank() },
            estimatedValue = if (json.has("estimatedValue")) json.getDouble("estimatedValue").toFloat() else null,
            imagesPaths = null, // Don't import images
            collectionName = collectionName,
            collectionOwner = ownerName,
            isImported = true
        )
    }

    /**
     * Parse CSV fields (handles quoted fields with commas)
     */
    private fun parseCsvFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        var currentField = StringBuilder()
        var insideQuotes = false

        for (char in line) {
            when {
                char == '"' -> insideQuotes = !insideQuotes
                char == ',' && !insideQuotes -> {
                    fields.add(currentField.toString())
                    currentField = StringBuilder()
                }
                else -> currentField.append(char)
            }
        }
        fields.add(currentField.toString())

        return fields
    }

    companion object {
        /**
         * Validate import file format
         */
        fun validateCSV(uri: Uri, context: Context): Boolean {
            return try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val firstLine = BufferedReader(InputStreamReader(inputStream)).readLine()
                    firstLine?.contains("Model ID") == true
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        fun validateJSON(uri: Uri, context: Context): Boolean {
            return try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonString)
                    json.has("collection")
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
}
