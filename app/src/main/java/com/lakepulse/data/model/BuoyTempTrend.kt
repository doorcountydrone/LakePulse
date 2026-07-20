package com.lakepulse.data.model

import java.time.Instant

data class BuoyTempSample(
    val observedAt: Instant,
    val waterTempF: Double,
)

data class BuoyTempTrend(
    val stationId: String,
    val samples: List<BuoyTempSample>,
) {
    val minTempF: Double? get() = samples.minOfOrNull { it.waterTempF }
    val maxTempF: Double? get() = samples.maxOfOrNull { it.waterTempF }
    val latestTempF: Double? get() = samples.lastOrNull()?.waterTempF
    val oldestTempF: Double? get() = samples.firstOrNull()?.waterTempF
    val deltaF: Double? get() {
        val latest = latestTempF ?: return null
        val oldest = oldestTempF ?: return null
        return latest - oldest
    }
}
