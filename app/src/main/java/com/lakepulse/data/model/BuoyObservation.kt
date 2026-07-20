package com.lakepulse.data.model

data class BuoyObservation(
    val stationId: String,
    val latitude: Double,
    val longitude: Double,
    val waterTempF: Double,
    val windSpeedMph: Double?,
    val windGustMph: Double?,
    val windDirectionDeg: Int?,
    val waveHeightFt: Double?,
    val observedAt: String?,
)
