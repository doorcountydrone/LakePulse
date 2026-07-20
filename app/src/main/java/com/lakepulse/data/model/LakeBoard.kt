package com.lakepulse.data.model

import com.lakepulse.data.location.UserLocation
import com.lakepulse.data.location.distanceMiles

enum class BuoySort(val label: String) {
    NEAREST("Nearest"),
    TEMP_COLD("Coldest"),
    TEMP_WARM("Warmest"),
    WIND("Windiest"),
    WAVES("Biggest waves"),
}

data class RankedBuoy(
    val observation: BuoyObservation,
    val distanceMiles: Double?,
) {
    val stationId: String get() = observation.stationId
}

data class LakeBoard(
    val lake: GreatLake,
    val buoys: List<RankedBuoy>,
    val minTempF: Double?,
    val maxTempF: Double?,
    val avgTempF: Double?,
    val windiest: BuoyObservation?,
    val biggestWaves: BuoyObservation?,
    val sortedByDistance: Boolean,
) {
    val buoyCount: Int get() = buoys.size
}

fun List<BuoyObservation>.toLakeBoard(
    lake: GreatLake,
    sort: BuoySort,
    userLocation: UserLocation? = null,
): LakeBoard {
    val lakeBuoys = filter { it.assignedLake() == lake }.map { buoy ->
        RankedBuoy(
            observation = buoy,
            distanceMiles = userLocation?.let {
                distanceMiles(it.latitude, it.longitude, buoy.latitude, buoy.longitude)
            },
        )
    }

    val effectiveSort =
        if (sort == BuoySort.NEAREST && userLocation == null) BuoySort.TEMP_COLD else sort

    val sorted = when (effectiveSort) {
        BuoySort.NEAREST -> lakeBuoys.sortedBy { it.distanceMiles ?: Double.MAX_VALUE }
        BuoySort.TEMP_COLD -> lakeBuoys.sortedBy { it.observation.waterTempF }
        BuoySort.TEMP_WARM -> lakeBuoys.sortedByDescending { it.observation.waterTempF }
        BuoySort.WIND -> lakeBuoys.sortedByDescending { it.observation.windSpeedMph ?: -1.0 }
        BuoySort.WAVES -> lakeBuoys.sortedByDescending { it.observation.waveHeightFt ?: -1.0 }
    }

    val observations = lakeBuoys.map { it.observation }
    val temps = observations.map { it.waterTempF }
    return LakeBoard(
        lake = lake,
        buoys = sorted,
        minTempF = temps.minOrNull(),
        maxTempF = temps.maxOrNull(),
        avgTempF = if (temps.isEmpty()) null else temps.average(),
        windiest = observations.maxByOrNull { it.windSpeedMph ?: Double.NEGATIVE_INFINITY },
        biggestWaves = observations.maxByOrNull { it.waveHeightFt ?: Double.NEGATIVE_INFINITY },
        sortedByDistance = effectiveSort == BuoySort.NEAREST && userLocation != null,
    )
}

object BuoyNames {
    private val names = mapOf(
        "45001" to "Mid Superior",
        "45002" to "North Michigan",
        "45005" to "West Erie",
        "45006" to "West Superior",
        "45012" to "East Ontario",
        "45013" to "Milwaukee",
        "45023" to "Keweenaw",
        "45025" to "Munising",
        "45026" to "South Haven",
        "45027" to "Duluth",
        "45029" to "Holland",
        "45132" to "Port Stanley",
        "45135" to "Prince Edward Point",
        "45136" to "Caribou Island",
        "45137" to "Georgian Bay",
        "45139" to "West Ontario",
        "45142" to "Port Colborne",
        "45143" to "Georgian Bay",
        "45147" to "Lake St. Clair",
        "45149" to "Southern Huron",
        "45151" to "Nottawasaga",
        "45152" to "North Channel",
        "45154" to "Spanish River",
        "45159" to "Toronto East",
        "45164" to "Cleveland",
        "45165" to "Toledo",
        "45168" to "Muskegon",
        "45170" to "Michigan City",
        "45176" to "Cleveland East",
        "45186" to "Waukegan",
        "45187" to "Wilmette",
        "45194" to "Mackinac",
        "45198" to "Chicago",
        "45210" to "Central Michigan",
        "45211" to "East Superior",
        "45213" to "Caribou / Mid Superior",
    )

    fun displayName(stationId: String): String =
        names[stationId] ?: "Buoy $stationId"
}
