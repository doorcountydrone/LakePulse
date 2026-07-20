package com.lakepulse.data.remote

import com.lakepulse.data.model.BuoyTempTrend
import java.time.Duration
import java.util.Locale

class NdbcRealtimeHistoryClient(
    private val httpClient: HttpClient = HttpClient,
) {
    private data class CacheEntry(
        val fetchedAtMs: Long,
        val trend: BuoyTempTrend,
    )

    private val cache = mutableMapOf<String, CacheEntry>()

    @Synchronized
    fun fetchTempTrend(
        stationId: String,
        window: Duration = Duration.ofHours(24),
        maxAgeMs: Long = 10 * 60 * 1000L,
    ): BuoyTempTrend {
        val now = System.currentTimeMillis()
        val cached = cache[stationId]
        if (cached != null && now - cached.fetchedAtMs <= maxAgeMs) {
            return cached.trend
        }

        // NDBC realtime2 paths are case-sensitive: C-MAN / letter IDs must be UPPERCASE
        // (e.g. MNMM4.txt). Numeric buoy IDs work either way.
        val id = stationId.uppercase(Locale.US)
        val body = httpClient.get("https://www.ndbc.noaa.gov/data/realtime2/$id.txt")
        val trend = NdbcRealtimeHistoryParser.parseTempTrend(
            stationId = stationId,
            raw = body,
            window = window,
        )
        cache[stationId] = CacheEntry(fetchedAtMs = now, trend = trend)
        return trend
    }
}
