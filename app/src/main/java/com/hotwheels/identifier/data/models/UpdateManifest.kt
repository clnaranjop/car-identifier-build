package com.hotwheels.identifier.data.models

import org.json.JSONObject

/**
 * Manifest structure for incremental database updates
 */
data class UpdateManifest(
    val version: String,
    val baseVersion: String,
    val lastUpdated: String,
    val collections: Map<String, Collection>
) {
    data class Collection(
        val version: String,
        val years: String,
        val modelCount: Int,
        val sizeMb: Int,
        val files: Files,
        val requires: List<String> = listOf(),
        val checksum: Checksum
    )

    data class Files(
        val images: String,
        val embeddings: String
    )

    data class Checksum(
        val images: String,
        val embeddings: String
    )

    companion object {
        fun fromJson(json: JSONObject): UpdateManifest {
            val collectionsJson = json.getJSONObject("collections")
            val collections = mutableMapOf<String, Collection>()

            collectionsJson.keys().forEach { key ->
                val collectionJson = collectionsJson.getJSONObject(key)
                val filesJson = collectionJson.getJSONObject("files")
                val checksumJson = collectionJson.getJSONObject("checksum")

                val requiresArray = collectionJson.optJSONArray("requires")
                val requires = mutableListOf<String>()
                if (requiresArray != null) {
                    for (i in 0 until requiresArray.length()) {
                        requires.add(requiresArray.getString(i))
                    }
                }

                collections[key] = Collection(
                    version = collectionJson.getString("version"),
                    years = collectionJson.getString("years"),
                    modelCount = collectionJson.getInt("model_count"),
                    sizeMb = collectionJson.getInt("size_mb"),
                    files = Files(
                        images = filesJson.getString("images"),
                        embeddings = filesJson.getString("embeddings")
                    ),
                    requires = requires,
                    checksum = Checksum(
                        images = checksumJson.getString("images"),
                        embeddings = checksumJson.getString("embeddings")
                    )
                )
            }

            return UpdateManifest(
                version = json.getString("version"),
                baseVersion = json.getString("base_version"),
                lastUpdated = json.getString("last_updated"),
                collections = collections
            )
        }
    }
}

/**
 * Information about available updates
 */
data class UpdateInfo(
    val hasUpdates: Boolean,
    val collections: Map<String, UpdateManifest.Collection>,
    val totalSizeMB: Int,
    val newModelCount: Int
)
