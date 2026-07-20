package com.lakepulse.data.location

import com.lakepulse.data.model.GreatLake

/** Miles from a Great Lakes bounding box to count as "near shore". */
private const val NEAR_SHORE_MILES = 35.0

fun UserLocation.isNearGreatLakesShore(): Boolean =
    isNearGreatLakesShore(latitude, longitude)

fun isNearGreatLakesShore(
    latitude: Double,
    longitude: Double,
    thresholdMiles: Double = NEAR_SHORE_MILES,
): Boolean {
    if (GreatLake.containing(latitude, longitude) != null) return true
    return GreatLake.entries.any { lake ->
        distanceMilesToLake(latitude, longitude, lake) <= thresholdMiles
    }
}

/**
 * Distance to the lake's bounding box (0 when inside).
 * Rough shoreline proxy used for default-tab context.
 */
fun distanceMilesToLake(
    latitude: Double,
    longitude: Double,
    lake: GreatLake,
): Double {
    if (lake.contains(latitude, longitude)) return 0.0
    val nearestLat = latitude.coerceIn(lake.minLat, lake.maxLat)
    val nearestLon = longitude.coerceIn(lake.minLon, lake.maxLon)
    return distanceMiles(latitude, longitude, nearestLat, nearestLon)
}
