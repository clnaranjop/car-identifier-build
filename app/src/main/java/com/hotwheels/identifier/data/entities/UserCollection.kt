package com.hotwheels.identifier.data.entities

data class UserCollection(
    val id: Long = 0,
    val hotWheelModelId: String,
    val quantity: Int = 1,
    val condition: String = "Mint", // Mint, Near Mint, Good, Poor
    val acquiredDate: Long = System.currentTimeMillis(),
    val acquiredPrice: Float? = null,
    val location: String? = null, // Where it's stored
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val imagesPaths: String? = null, // JSON array of image paths

    // NEW: Collection separation
    val collectionName: String = "My Collection", // Name of the collection
    val collectionOwner: String? = null, // Owner's name (null = current user)
    val isImported: Boolean = false, // True if imported from another user

    // NEW: Special features for enhanced collection display
    val isTreasureHunt: Boolean = false,  // TH badge (green)
    val isSuperTreasureHunt: Boolean = false,  // STH badge (gold)
    val rarityLevel: Int = 1,  // 1-5 stars (Common to Ultra Rare)
    val variation: String? = null,  // "Zamac", "Chase", "Error", etc.
    val estimatedValue: Float? = null,  // Cached market value
    val lastValueUpdate: Long? = null  // Last price update timestamp
)