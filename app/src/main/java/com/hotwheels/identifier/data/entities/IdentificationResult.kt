package com.hotwheels.identifier.data.entities

data class IdentificationResult(
    val id: Long = 0,
    val hotWheelModelId: String,
    val imagePath: String,
    val confidence: Float,
    val identificationMethod: String, // "SHAPE_MATCHING", "TEMPLATE_MATCHING", "FEATURE_DETECTION", "COMBINED"
    val processingTimeMs: Long,
    val dateIdentified: Long = System.currentTimeMillis(),
    val userConfirmed: Boolean = false,
    val notes: String? = null
)