package com.hotwheels.identifier.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.hotwheels.identifier.viewmodel.CollectionItemWithModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CollectionExporter(private val context: Context) {

    private val tag = "CollectionExporter"

    /**
     * Export collection to CSV format
     */
    fun exportToCSV(items: List<CollectionItemWithModel>): Result<File> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "HotWheels_Collection_$timestamp.csv"
            val file = createExportFile(fileName)

            FileWriter(file).use { writer ->
                // Write header
                writer.append("Model ID,Name,Series,Year,Category,Manufacturer,Quantity,Condition,")
                writer.append("Acquired Date,Is Favorite,Is Treasure Hunt,Is Super TH,Rarity Level,")
                writer.append("Variation,Estimated Value,Image Path\n")

                // Write data rows
                items.forEach { item ->
                    writer.append(escapeCsvField(item.collectionItem.hotWheelModelId))
                    writer.append(",")
                    writer.append(escapeCsvField(item.modelName))
                    writer.append(",")
                    writer.append(escapeCsvField(item.modelSeries))
                    writer.append(",")
                    writer.append(item.modelYear.toString())
                    writer.append(",")
                    writer.append("") // category not available
                    writer.append(",")
                    writer.append("Mattel") // manufacturer
                    writer.append(",")
                    writer.append(item.collectionItem.quantity.toString())
                    writer.append(",")
                    writer.append(escapeCsvField(item.collectionItem.condition))
                    writer.append(",")
                    writer.append(item.collectionItem.acquiredDate.toString())
                    writer.append(",")
                    writer.append(item.collectionItem.isFavorite.toString())
                    writer.append(",")
                    writer.append(item.collectionItem.isTreasureHunt.toString())
                    writer.append(",")
                    writer.append(item.collectionItem.isSuperTreasureHunt.toString())
                    writer.append(",")
                    writer.append(item.collectionItem.rarityLevel.toString())
                    writer.append(",")
                    writer.append(escapeCsvField(item.collectionItem.variation ?: ""))
                    writer.append(",")
                    writer.append(item.collectionItem.estimatedValue?.toString() ?: "")
                    writer.append(",")
                    writer.append(escapeCsvField(item.collectionItem.imagesPaths ?: ""))
                    writer.append("\n")
                }
            }

            Log.d(tag, "✅ CSV export successful: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(tag, "❌ CSV export failed", e)
            Result.failure(e)
        }
    }

    /**
     * Export collection to JSON format
     */
    fun exportToJSON(items: List<CollectionItemWithModel>): Result<File> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "HotWheels_Collection_$timestamp.json"
            val file = createExportFile(fileName)

            val jsonArray = JSONArray()
            items.forEach { item ->
                val jsonObject = JSONObject().apply {
                    put("modelId", item.collectionItem.hotWheelModelId)
                    put("name", item.modelName)
                    put("series", item.modelSeries)
                    put("year", item.modelYear)
                    put("category", "")
                    put("manufacturer", "Mattel")
                    put("quantity", item.collectionItem.quantity)
                    put("condition", item.collectionItem.condition)
                    put("acquiredDate", item.collectionItem.acquiredDate)
                    put("isFavorite", item.collectionItem.isFavorite)
                    put("isTreasureHunt", item.collectionItem.isTreasureHunt)
                    put("isSuperTreasureHunt", item.collectionItem.isSuperTreasureHunt)
                    put("rarityLevel", item.collectionItem.rarityLevel)
                    put("variation", item.collectionItem.variation ?: "")
                    put("estimatedValue", item.collectionItem.estimatedValue ?: 0.0)
                    put("imagePath", item.collectionItem.imagesPaths ?: "")
                }
                jsonArray.put(jsonObject)
            }

            val jsonRoot = JSONObject().apply {
                put("exportDate", System.currentTimeMillis())
                put("totalItems", items.size)
                put("appVersion", "2.0")
                put("collection", jsonArray)
            }

            FileWriter(file).use { writer ->
                writer.write(jsonRoot.toString(2)) // Pretty print with 2 spaces
            }

            Log.d(tag, "✅ JSON export successful: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(tag, "❌ JSON export failed", e)
            Result.failure(e)
        }
    }

    /**
     * Create export file in Downloads directory
     */
    private fun createExportFile(fileName: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()

        return file
    }

    /**
     * Escape CSV field (handle commas and quotes)
     */
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    companion object {
        /**
         * Get export statistics
         */
        fun getExportStats(items: List<CollectionItemWithModel>): String {
            val totalItems = items.sumOf { it.collectionItem.quantity }
            val uniqueModels = items.size
            val favorites = items.count { it.collectionItem.isFavorite }
            val treasureHunts = items.count { it.collectionItem.isTreasureHunt }
            val superTH = items.count { it.collectionItem.isSuperTreasureHunt }

            return """
                Total Items: $totalItems
                Unique Models: $uniqueModels
                Favorites: $favorites
                Treasure Hunts: $treasureHunts
                Super TH: $superTH
            """.trimIndent()
        }
    }
}
