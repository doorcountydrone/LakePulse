package com.lakepulse.data.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
)

fun distanceMiles(
    fromLat: Double,
    fromLon: Double,
    toLat: Double,
    toLon: Double,
): Double {
    val earthRadiusMiles = 3958.7613
    val dLat = Math.toRadians(toLat - fromLat)
    val dLon = Math.toRadians(toLon - fromLon)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(fromLat)) * cos(Math.toRadians(toLat)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMiles * c
}
