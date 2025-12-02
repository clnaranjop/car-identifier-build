package com.hotwheels.identifier.ml

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a match result from Hot Wheels identification
 */
@Parcelize
data class IdentificationMatch(
    val modelId: String,
    val confidence: Float,
    val method: String,
    val details: String = ""
) : Parcelable
