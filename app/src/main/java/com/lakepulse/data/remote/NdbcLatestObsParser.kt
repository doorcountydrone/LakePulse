package com.lakepulse.data.remote

import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.model.celsiusToFahrenheit

object NdbcLatestObsParser {
    /** Rough Great Lakes basin (excludes Lake Winnipeg / Manitoba). */
    private const val MIN_LAT = 41.0
    private const val MAX_LAT = 49.5
    private const val MIN_LON = -93.0
    private const val MAX_LON = -75.5

    fun parse(raw: String): List<BuoyObservation> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { parseLine(it) }
            .toList()
    }

    private fun parseLine(line: String): BuoyObservation? {
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 19) return null

        val stationId = parts[0]
        val latitude = parts[1].toDoubleOrNull() ?: return null
        val longitude = parts[2].toDoubleOrNull() ?: return null
        if (latitude !in MIN_LAT..MAX_LAT || longitude !in MIN_LON..MAX_LON) return null

        val waterC = parts[18].toDoubleOrNull() ?: return null

        val windDir = parts.getOrNull(8)?.toIntOrNull()
        val windMs = parts.getOrNull(9)?.toDoubleOrNull()
        val gustMs = parts.getOrNull(10)?.toDoubleOrNull()
        val waveM = parts.getOrNull(11)?.toDoubleOrNull()

        val observedAt = runCatching {
            val year = parts[3]
            val month = parts[4].padStart(2, '0')
            val day = parts[5].padStart(2, '0')
            val hour = parts[6].padStart(2, '0')
            val minute = parts[7].padStart(2, '0')
            "$year-$month-$day ${hour}:$minute UTC"
        }.getOrNull()

        return BuoyObservation(
            stationId = stationId,
            latitude = latitude,
            longitude = longitude,
            waterTempF = waterC.celsiusToFahrenheit(),
            windSpeedMph = windMs?.metersPerSecondToMph(),
            windGustMph = gustMs?.metersPerSecondToMph(),
            windDirectionDeg = windDir,
            waveHeightFt = waveM?.metersToFeet(),
            observedAt = observedAt,
        )
    }

    private fun Double.metersPerSecondToMph(): Double = this * 2.23693629
    private fun Double.metersToFeet(): Double = this * 3.2808399
}
