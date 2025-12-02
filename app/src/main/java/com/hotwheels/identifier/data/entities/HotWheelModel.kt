package com.hotwheels.identifier.data.entities

data class HotWheelModel(
    val id: String,
    val name: String,
    val series: String,
    val year: Int,
    val category: String,
    val manufacturer: String = "Mattel",
    val description: String? = null,
    val rarity: String = "Common", // Common, Uncommon, Rare, Ultra Rare
    val imageResourceName: String? = null,
    val templateImagePath: String? = null, // Path to template image for matching
    val localImagePath: String? = null, // Path to local asset image
    val keyFeatures: String? = null, // JSON string of key visual features
    val dimensions: String? = null, // Length x Width x Height in mm
    val weight: Float? = null, // Weight in grams
    val color: String? = null, // Main color (though we won't rely on this for ID)
    val tampoDetails: String? = null, // Special printing/tampo details
    val wheelType: String? = null, // Type of wheels
    val baseType: String? = null, // Type of base (metal, plastic, etc.)
    val dateAdded: Long = System.currentTimeMillis()
)