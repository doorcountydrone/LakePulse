package com.lakepulse.data.model

enum class FishingSource(val displayName: String) {
    MICHIGAN("Michigan"),
    WISCONSIN("Wisconsin"),
}

data class FishingReportWeekSummary(
    val id: String,
    val title: String,
    val publishedLabel: String,
    val bulletinUrl: String,
    val source: FishingSource = FishingSource.MICHIGAN,
)

data class FishingRegionReport(
    val region: FishingRegion,
    val locations: List<FishingLocationReport>,
)

data class FishingLocationReport(
    val id: String,
    val name: String,
    val region: FishingRegion,
    val body: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMiles: Double? = null,
    val source: FishingSource = FishingSource.MICHIGAN,
)

data class FishingReportWeek(
    val summary: FishingReportWeekSummary,
    val regions: List<FishingRegionReport>,
    val locations: List<FishingLocationReport>,
)

enum class FishingRegion(val displayName: String) {
    SOUTHEAST_LOWER("Southeast Lower Peninsula"),
    SOUTHWEST_LOWER("Southwest Lower Peninsula"),
    NORTHEAST_LOWER("Northeast Lower Peninsula"),
    NORTHWEST_LOWER("Northwest Lower Peninsula"),
    UPPER_PENINSULA("Upper Peninsula"),
    WEST_SHORE_GREEN_BAY("West Shore Green Bay"),
    EAST_SHORE_GREEN_BAY("East Shore Green Bay"),
    NORTHERN_LAKE_MICHIGAN("Northern Lake Michigan"),
    SOUTHERN_LAKE_MICHIGAN("Southern Lake Michigan"),
    LAKE_SUPERIOR("Lake Superior"),
    UNKNOWN("Other"),
    ;

    companion object {
        fun fromHeading(heading: String): FishingRegion {
            val normalized = heading.trim().lowercase()
            return when {
                normalized.contains("west shore") && normalized.contains("green bay") ->
                    WEST_SHORE_GREEN_BAY
                normalized.contains("east shore") && normalized.contains("green bay") ->
                    EAST_SHORE_GREEN_BAY
                normalized.contains("northern") && normalized.contains("lake michigan") ->
                    NORTHERN_LAKE_MICHIGAN
                normalized.contains("southern") && normalized.contains("lake michigan") ->
                    SOUTHERN_LAKE_MICHIGAN
                normalized.contains("lake superior") || normalized == "superior" ->
                    LAKE_SUPERIOR
                normalized.contains("southeast") -> SOUTHEAST_LOWER
                normalized.contains("southwest") -> SOUTHWEST_LOWER
                normalized.contains("northeast") -> NORTHEAST_LOWER
                normalized.contains("northwest") -> NORTHWEST_LOWER
                normalized.contains("upper") -> UPPER_PENINSULA
                else -> UNKNOWN
            }
        }
    }
}

fun FishingReportWeek.withDistances(
    userLat: Double?,
    userLon: Double?,
): FishingReportWeek {
    if (userLat == null || userLon == null) return this
    val ranked = locations.map { location ->
        location.copy(
            distanceMiles = com.lakepulse.data.location.distanceMiles(
                userLat,
                userLon,
                location.latitude,
                location.longitude,
            ),
        )
    }.sortedBy { it.distanceMiles ?: Double.MAX_VALUE }

    val byRegion = ranked.groupBy { it.region }.map { (region, locs) ->
        FishingRegionReport(region, locs)
    }
    return copy(regions = byRegion, locations = ranked)
}

/** Avoid LazyColumn crashes when DNR repeats names like "Pier/Shore" or "Ramp". */
fun FishingReportWeek.ensuringUniqueIds(): FishingReportWeek {
    val counts = mutableMapOf<String, Int>()
    val unique = locations.mapIndexed { index, location ->
        val base = location.id.ifBlank { "location-$index" }
        val seen = counts[base] ?: 0
        counts[base] = seen + 1
        if (seen == 0) location else location.copy(id = "$base-$seen")
    }
    return copy(
        locations = unique,
        regions = unique.groupBy { it.region }.map { (region, locs) ->
            FishingRegionReport(region, locs)
        },
    )
}
