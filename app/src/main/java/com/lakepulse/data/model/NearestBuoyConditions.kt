package com.lakepulse.data.model

import com.lakepulse.data.location.distanceMiles

data class NearestBuoyConditions(
    val stationId: String,
    val displayName: String,
    val distanceMiles: Double,
    val waterTempF: Double,
    val windSpeedMph: Double?,
    val windDirectionDeg: Int?,
    val waveHeightFt: Double?,
    val observedAt: String?,
)

fun findNearestBuoy(
    latitude: Double,
    longitude: Double,
    buoys: List<BuoyObservation>,
    maxMiles: Double = 75.0,
): NearestBuoyConditions? {
    if (buoys.isEmpty()) return null
    val nearest = buoys
        .map { buoy ->
            buoy to distanceMiles(latitude, longitude, buoy.latitude, buoy.longitude)
        }
        .minByOrNull { it.second }
        ?: return null
    if (nearest.second > maxMiles) return null
    val buoy = nearest.first
    return NearestBuoyConditions(
        stationId = buoy.stationId,
        displayName = BuoyNames.displayName(buoy.stationId),
        distanceMiles = nearest.second,
        waterTempF = buoy.waterTempF,
        windSpeedMph = buoy.windSpeedMph,
        windDirectionDeg = buoy.windDirectionDeg,
        waveHeightFt = buoy.waveHeightFt,
        observedAt = buoy.observedAt,
    )
}
