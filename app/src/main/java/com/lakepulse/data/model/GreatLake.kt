package com.lakepulse.data.model

enum class GreatLake(
    val displayName: String,
    val shortName: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
) {
    SUPERIOR("Lake Superior", "Superior", 46.4, 49.2, -92.5, -84.3),
    MICHIGAN("Lake Michigan", "Michigan", 41.5, 46.25, -88.2, -84.65),
    HURON("Lake Huron", "Huron", 43.0, 46.5, -84.65, -79.5),
    ERIE("Lake Erie", "Erie", 41.3, 43.0, -83.7, -78.8),
    ONTARIO("Lake Ontario", "Ontario", 43.1, 44.4, -79.9, -75.8),
    ;

    val centerLatitude: Double get() = (minLat + maxLat) / 2.0
    val centerLongitude: Double get() = (minLon + maxLon) / 2.0

    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in minLat..maxLat && longitude in minLon..maxLon

    companion object {
        fun containing(latitude: Double, longitude: Double): GreatLake? =
            entries.firstOrNull { it.contains(latitude, longitude) }

        fun nearestTo(latitude: Double, longitude: Double): GreatLake {
            containing(latitude, longitude)?.let { return it }
            return entries.minBy { lake ->
                com.lakepulse.data.location.distanceMiles(
                    latitude,
                    longitude,
                    lake.centerLatitude,
                    lake.centerLongitude,
                )
            }
        }
    }
}

fun BuoyObservation.assignedLake(): GreatLake? =
    GreatLake.containing(latitude, longitude)
