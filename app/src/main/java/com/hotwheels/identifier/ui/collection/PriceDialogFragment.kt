package com.hotwheels.identifier.ui.collection

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.hotwheels.identifier.R
import com.hotwheels.identifier.data.PriceStatus
import com.hotwheels.identifier.databinding.DialogPriceValuesBinding
import com.hotwheels.identifier.utils.PriceEstimator
import kotlinx.coroutines.launch

/**
 * Dialog fragment that displays market values from multiple sources
 * Shows prices from eBay, Amazon, and Mercado Libre
 */
class PriceDialogFragment : DialogFragment() {

    private var _binding: DialogPriceValuesBinding? = null
    private val binding get() = _binding!!

    private lateinit var priceEstimator: PriceEstimator

    private var modelName: String = ""
    private var year: Int = 0
    private var modelId: String = ""

    companion object {
        private const val TAG = "PriceDialogFragment"
        private const val ARG_MODEL_NAME = "model_name"
        private const val ARG_YEAR = "year"
        private const val ARG_MODEL_ID = "model_id"

        fun newInstance(modelName: String, year: Int, modelId: String): PriceDialogFragment {
            return PriceDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODEL_NAME, modelName)
                    putInt(ARG_YEAR, year)
                    putString(ARG_MODEL_ID, modelId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            modelName = it.getString(ARG_MODEL_NAME, "")
            year = it.getInt(ARG_YEAR, 0)
            modelId = it.getString(ARG_MODEL_ID, "")
        }

        priceEstimator = PriceEstimator(requireContext())

        setStyle(STYLE_NORMAL, R.style.Theme_HotWheelsIdentifier)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPriceValuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadPrices()
    }

    private fun setupUI() {
        // Set model name in header
        binding.tvModelNameHeader.text = "$modelName ($year)"

        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            loadPrices()
        }
    }

    private fun loadPrices() {
        Log.d(TAG, "Loading prices for: $modelName ($year)")

        // Show loading state for all sources
        showLoadingState()

        // Fetch prices asynchronously
        lifecycleScope.launch {
            try {
                val prices = priceEstimator.getAllPrices(modelName, year, modelId)

                // Display eBay prices
                displayEbayPrices(prices.ebay)

                // Display Amazon price
                displayAmazonPrice(prices.amazon)

                // Display Mercado Libre price
                displayMercadoLibrePrice(prices.mercadoLibre)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading prices", e)
                Toast.makeText(
                    requireContext(),
                    "Error al cargar precios: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Show error state
                showErrorState(e.message ?: "Unknown error")
            }
        }
    }

    private fun showLoadingState() {
        // eBay loading
        binding.progressEbay.visibility = View.VISIBLE
        binding.layoutEbayPrices.visibility = View.GONE
        binding.tvEbayError.visibility = View.GONE

        // Amazon loading
        binding.progressAmazon.visibility = View.VISIBLE
        binding.tvAmazonPrice.visibility = View.GONE
        binding.tvAmazonError.visibility = View.GONE

        // Mercado Libre loading
        binding.progressMercadoLibre.visibility = View.VISIBLE
        binding.tvMercadoLibrePrice.visibility = View.GONE
        binding.tvMercadoLibreError.visibility = View.GONE
    }

    private fun displayEbayPrices(ebaySource: com.hotwheels.identifier.data.PriceSource?) {
        binding.progressEbay.visibility = View.GONE

        when (ebaySource?.status) {
            PriceStatus.SUCCESS -> {
                val priceRange = ebaySource.priceRange
                if (priceRange != null) {
                    binding.layoutEbayPrices.visibility = View.VISIBLE
                    binding.tvEbayError.visibility = View.GONE

                    binding.tvEbayMin.text = priceEstimator.formatPrice(priceRange.min, priceRange.currency)
                    binding.tvEbayAvg.text = priceEstimator.formatPrice(priceRange.avg, priceRange.currency)
                    binding.tvEbayMax.text = priceEstimator.formatPrice(priceRange.max, priceRange.currency)
                    binding.tvEbayStatus.text = "${priceRange.salesCount} vendidos"

                    Log.d(TAG, "eBay prices displayed: ${priceRange.avg} ${priceRange.currency}")
                } else {
                    showEbayError("No price data available")
                }
            }

            PriceStatus.ERROR -> {
                showEbayError(ebaySource.errorMessage ?: "Error al obtener precios")
            }

            PriceStatus.NOT_AVAILABLE -> {
                showEbayError(ebaySource.errorMessage ?: "No disponible")
            }

            else -> {
                showEbayError("Sin datos")
            }
        }
    }

    private fun displayAmazonPrice(amazonSource: com.hotwheels.identifier.data.PriceSource?) {
        binding.progressAmazon.visibility = View.GONE

        when (amazonSource?.status) {
            PriceStatus.SUCCESS -> {
                val price = amazonSource.price
                if (price != null) {
                    binding.tvAmazonPrice.visibility = View.VISIBLE
                    binding.tvAmazonError.visibility = View.GONE

                    binding.tvAmazonPrice.text = priceEstimator.formatPrice(price, "USD")
                    binding.tvAmazonStatus.text = "Disponible"

                    Log.d(TAG, "Amazon price displayed: $price USD")
                } else {
                    showAmazonError("No price data available")
                }
            }

            PriceStatus.ERROR -> {
                showAmazonError(amazonSource.errorMessage ?: "Error al obtener precio")
            }

            PriceStatus.NOT_AVAILABLE -> {
                showAmazonError(amazonSource.errorMessage ?: "Próximamente")
            }

            else -> {
                showAmazonError("Sin datos")
            }
        }
    }

    private fun displayMercadoLibrePrice(mercadoLibreSource: com.hotwheels.identifier.data.PriceSource?) {
        binding.progressMercadoLibre.visibility = View.GONE

        when (mercadoLibreSource?.status) {
            PriceStatus.SUCCESS -> {
                val priceRange = mercadoLibreSource.priceRange
                if (priceRange != null) {
                    binding.tvMercadoLibrePrice.visibility = View.VISIBLE
                    binding.tvMercadoLibreError.visibility = View.GONE

                    binding.tvMercadoLibrePrice.text = priceEstimator.formatPrice(
                        priceRange.avg,
                        priceRange.currency
                    )
                    binding.tvMercadoLibreStatus.text = priceEstimator.formatPriceRange(priceRange)

                    Log.d(TAG, "Mercado Libre price displayed: ${priceRange.avg} ${priceRange.currency}")
                } else {
                    showMercadoLibreError("No price data available")
                }
            }

            PriceStatus.ERROR -> {
                showMercadoLibreError(mercadoLibreSource.errorMessage ?: "Error al obtener precio")
            }

            PriceStatus.NOT_AVAILABLE -> {
                showMercadoLibreError(mercadoLibreSource.errorMessage ?: "No disponible")
            }

            else -> {
                showMercadoLibreError("Sin datos")
            }
        }
    }

    private fun showEbayError(message: String) {
        binding.layoutEbayPrices.visibility = View.GONE
        binding.tvEbayError.visibility = View.VISIBLE
        binding.tvEbayError.text = message
    }

    private fun showAmazonError(message: String) {
        binding.tvAmazonPrice.visibility = View.GONE
        binding.tvAmazonError.visibility = View.VISIBLE
        binding.tvAmazonError.text = message
        binding.tvAmazonStatus.text = "Error"
    }

    private fun showMercadoLibreError(message: String) {
        binding.tvMercadoLibrePrice.visibility = View.GONE
        binding.tvMercadoLibreError.visibility = View.VISIBLE
        binding.tvMercadoLibreError.text = message
        binding.tvMercadoLibreStatus.text = "Error"
    }

    private fun showErrorState(message: String) {
        showEbayError(message)
        showAmazonError(message)
        showMercadoLibreError(message)
    }

    override fun onStart() {
        super.onStart()
        // Make dialog fullscreen on small screens
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
