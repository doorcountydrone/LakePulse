package com.lakepulse.ui.map

/**
 * Request to open Hybrid map on a fishing spot (usually with Chart basemap).
 */
data class FishingSpotFocus(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)
