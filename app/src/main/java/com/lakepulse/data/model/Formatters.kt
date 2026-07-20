package com.lakepulse.data.model

fun Double.celsiusToFahrenheit(): Double = this * 9.0 / 5.0 + 32.0

fun windDirectionLabel(degrees: Int?): String {
    if (degrees == null) return "—"
    val directions = listOf(
        "N", "NNE", "NE", "ENE",
        "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW",
        "W", "WNW", "NW", "NNW",
    )
    val normalized = ((degrees % 360) + 360) % 360
    val index = ((normalized + 11.25) / 22.5).toInt() % directions.size
    return directions[index]
}
