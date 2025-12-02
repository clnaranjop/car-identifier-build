package com.hotwheels.identifier.data

/**
 * Data classes for price estimation from multiple sources
 */

data class PriceRange(
    val min: Double,
    val avg: Double,
    val max: Double,
    val currency: String = "USD",
    val salesCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class PriceSource(
    val name: String,  // "eBay", "Amazon", "Mercado Libre"
    val price: Double? = null,
    val priceRange: PriceRange? = null,
    val status: PriceStatus = PriceStatus.LOADING,
    val errorMessage: String? = null,
    val url: String? = null  // Link to product page
)

enum class PriceStatus {
    LOADING,
    SUCCESS,
    ERROR,
    NOT_AVAILABLE
}

data class ModelPrices(
    val modelId: String,
    val modelName: String,
    val year: Int,
    val ebay: PriceSource? = null,
    val amazon: PriceSource? = null,
    val mercadoLibre: PriceSource? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getEstimatedValue(): Double? {
        val prices = listOfNotNull(
            ebay?.priceRange?.avg,
            ebay?.price,
            amazon?.price,
            mercadoLibre?.price
        )
        return if (prices.isNotEmpty()) prices.average() else null
    }

    fun isStale(maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000): Boolean {
        return System.currentTimeMillis() - lastUpdated > maxAgeMillis
    }
}
