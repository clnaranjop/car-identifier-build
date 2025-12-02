package com.hotwheels.identifier.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.hotwheels.identifier.R
import com.hotwheels.identifier.data.ModelPrices
import com.hotwheels.identifier.data.PriceRange
import com.hotwheels.identifier.data.PriceSource
import com.hotwheels.identifier.data.PriceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Price estimator using multiple marketplaces APIs
 *
 * APIs Used:
 * - eBay Finding API (Free - 5,000 calls/day)
 * - Amazon Product Advertising API (Free with affiliate account)
 * - Mercado Libre API (Free)
 *
 * Note: Configure API keys in strings.xml (ebay_app_id, amazon_access_key)
 */
class PriceEstimator(private val context: Context) {

    companion object {
        private const val TAG = "PriceEstimator"

        // eBay Finding API endpoint
        private const val EBAY_FINDING_API = "https://svcs.ebay.com/services/search/FindingService/v1"

        // Mercado Libre API endpoint
        private const val MERCADO_LIBRE_API = "https://api.mercadolibre.com/sites/MLM/search"
    }

    /**
     * Check if device has internet connection
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    /**
     * Get prices from all sources for a Hot Wheels model
     */
    suspend fun getAllPrices(
        modelName: String,
        year: Int,
        modelId: String
    ): ModelPrices = withContext(Dispatchers.IO) {

        // Check internet connection first
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No internet connection available")
            return@withContext ModelPrices(
                modelId = modelId,
                modelName = modelName,
                year = year,
                ebay = PriceSource(
                    name = "eBay",
                    status = PriceStatus.ERROR,
                    errorMessage = "No internet connection"
                ),
                amazon = PriceSource(
                    name = "Amazon",
                    status = PriceStatus.ERROR,
                    errorMessage = "No internet connection"
                ),
                mercadoLibre = PriceSource(
                    name = "Mercado Libre",
                    status = PriceStatus.ERROR,
                    errorMessage = "No internet connection"
                ),
                lastUpdated = System.currentTimeMillis()
            )
        }

        // Fetch from all sources in parallel
        val ebayDeferred = async { getEbayPrices(modelName, year) }
        val amazonDeferred = async { getAmazonPrice(modelName, year) }
        val mercadoLibreDeferred = async { getMercadoLibrePrice(modelName, year) }

        ModelPrices(
            modelId = modelId,
            modelName = modelName,
            year = year,
            ebay = ebayDeferred.await(),
            amazon = amazonDeferred.await(),
            mercadoLibre = mercadoLibreDeferred.await(),
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * eBay Finding API - Get completed listings prices
     * Free tier: 5,000 calls/day
     */
    private suspend fun getEbayPrices(modelName: String, year: Int): PriceSource {
        return try {
            val query = "$modelName Hot Wheels $year"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            // Get API key from resources
            val ebayAppId = context.getString(R.string.ebay_app_id)

            // Check if API key is configured
            if (ebayAppId == "YOUR_EBAY_APP_ID_HERE") {
                Log.w(TAG, "eBay API key not configured")
                return PriceSource(
                    name = "eBay",
                    status = PriceStatus.NOT_AVAILABLE,
                    errorMessage = "API key not configured. Get free key at developer.ebay.com"
                )
            }

            val url = buildString {
                append(EBAY_FINDING_API)
                append("?OPERATION-NAME=findCompletedItems")
                append("&SERVICE-VERSION=1.0.0")
                append("&SECURITY-APPNAME=$ebayAppId")
                append("&RESPONSE-DATA-FORMAT=JSON")
                append("&REST-PAYLOAD")
                append("&keywords=$encodedQuery")
                append("&categoryId=222")  // Diecast & Toy Vehicles
                append("&itemFilter(0).name=SoldItemsOnly")
                append("&itemFilter(0).value=true")
                append("&itemFilter(1).name=Condition")
                append("&itemFilter(1).value=New")
                append("&sortOrder=PricePlusShippingLowest")
                append("&paginationInput.entriesPerPage=100")
            }

            val response = URL(url).readText()
            val jsonResponse = JSONObject(response)

            // Parse eBay response
            val searchResult = jsonResponse
                .getJSONArray("findCompletedItemsResponse")
                .getJSONObject(0)
                .getJSONArray("searchResult")
                .getJSONObject(0)

            val itemsArray = searchResult.optJSONArray("item")

            if (itemsArray == null || itemsArray.length() == 0) {
                return PriceSource(
                    name = "eBay",
                    status = PriceStatus.NOT_AVAILABLE,
                    errorMessage = "No completed sales found"
                )
            }

            // Extract prices
            val prices = mutableListOf<Double>()
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val sellingStatus = item.getJSONArray("sellingStatus").getJSONObject(0)
                val currentPrice = sellingStatus.getJSONArray("currentPrice").getJSONObject(0)
                val price = currentPrice.getString("__value__").toDoubleOrNull()

                if (price != null && price > 0) {
                    prices.add(price)
                }
            }

            if (prices.isEmpty()) {
                return PriceSource(
                    name = "eBay",
                    status = PriceStatus.NOT_AVAILABLE,
                    errorMessage = "No valid prices found"
                )
            }

            // Calculate statistics
            val sortedPrices = prices.sorted()
            val priceRange = PriceRange(
                min = sortedPrices.first(),
                avg = sortedPrices.average(),
                max = sortedPrices.last(),
                currency = "USD",
                salesCount = prices.size
            )

            Log.d(TAG, "eBay prices fetched: $priceRange")

            PriceSource(
                name = "eBay",
                priceRange = priceRange,
                status = PriceStatus.SUCCESS
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching eBay prices", e)
            PriceSource(
                name = "eBay",
                status = PriceStatus.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Amazon Product Advertising API
     * Note: Requires Amazon affiliate account (free)
     */
    private suspend fun getAmazonPrice(modelName: String, year: Int): PriceSource {
        return try {
            // Get API key from resources
            val amazonAccessKey = context.getString(R.string.amazon_access_key)

            // Check if API key is configured
            if (amazonAccessKey == "YOUR_AMAZON_ACCESS_KEY_HERE") {
                Log.w(TAG, "Amazon API key not configured")
                return PriceSource(
                    name = "Amazon",
                    status = PriceStatus.NOT_AVAILABLE,
                    errorMessage = "API key not configured. Sign up at affiliate-program.amazon.com"
                )
            }

            // TODO: Implement Amazon Product Advertising API
            // This requires AWS SDK and signature generation
            // For now, return not available

            Log.w(TAG, "Amazon API not yet implemented")
            PriceSource(
                name = "Amazon",
                status = PriceStatus.NOT_AVAILABLE,
                errorMessage = "Coming soon - API implementation in progress"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Amazon price", e)
            PriceSource(
                name = "Amazon",
                status = PriceStatus.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Mercado Libre API - Free public API
     * Good for Latin America markets
     */
    private suspend fun getMercadoLibrePrice(modelName: String, year: Int): PriceSource {
        return try {
            val query = "$modelName Hot Wheels $year"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            // Search in Mexico site (MLM)
            val url = "$MERCADO_LIBRE_API?q=$encodedQuery&category=MLM1168&limit=20"

            val response = URL(url).readText()
            val jsonResponse = JSONObject(response)

            val results = jsonResponse.getJSONArray("results")

            if (results.length() == 0) {
                return PriceSource(
                    name = "Mercado Libre",
                    status = PriceStatus.NOT_AVAILABLE,
                    errorMessage = "No listings found"
                )
            }

            // Extract prices
            val prices = mutableListOf<Double>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val price = item.getDouble("price")

                if (price > 0) {
                    prices.add(price)
                }
            }

            if (prices.isEmpty()) {
                return PriceSource(
                    name = "Mercado Libre",
                    status = PriceStatus.NOT_AVAILABLE,
                    errorMessage = "No valid prices found"
                )
            }

            val avgPrice = prices.average()

            Log.d(TAG, "Mercado Libre price fetched: $avgPrice MXN")

            PriceSource(
                name = "Mercado Libre",
                price = avgPrice,
                priceRange = PriceRange(
                    min = prices.minOrNull() ?: 0.0,
                    avg = avgPrice,
                    max = prices.maxOrNull() ?: 0.0,
                    currency = "MXN",
                    salesCount = prices.size
                ),
                status = PriceStatus.SUCCESS
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Mercado Libre price", e)
            PriceSource(
                name = "Mercado Libre",
                status = PriceStatus.ERROR,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Format price for display
     */
    fun formatPrice(price: Double?, currency: String = "USD"): String {
        if (price == null) return "N/A"

        return when (currency) {
            "USD" -> "$${price.roundToInt()}"
            "MXN" -> "$${"%.0f".format(price)} MXN"
            else -> "${"%.2f".format(price)} $currency"
        }
    }

    /**
     * Format price range for display
     */
    fun formatPriceRange(priceRange: PriceRange?): String {
        if (priceRange == null) return "N/A"

        val currency = priceRange.currency
        val min = formatPrice(priceRange.min, currency)
        val max = formatPrice(priceRange.max, currency)

        return "$min - $max"
    }
}
