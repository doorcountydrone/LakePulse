package com.lakepulse.data.remote

import com.lakepulse.data.model.BuoyObservation

class NdbcLatestObsClient(
    private val httpClient: HttpClient = HttpClient,
) {
    @Volatile
    private var cachedAtMs: Long = 0L

    @Volatile
    private var cachedBuoys: List<BuoyObservation> = emptyList()

    @Synchronized
    fun fetchGreatLakes(maxAgeMs: Long = 3 * 60 * 1000L): List<BuoyObservation> {
        val now = System.currentTimeMillis()
        if (cachedBuoys.isNotEmpty() && now - cachedAtMs <= maxAgeMs) {
            return cachedBuoys
        }
        val body = httpClient.get("https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt")
        val buoys = NdbcLatestObsParser.parse(body)
        cachedBuoys = buoys
        cachedAtMs = now
        return buoys
    }
}
