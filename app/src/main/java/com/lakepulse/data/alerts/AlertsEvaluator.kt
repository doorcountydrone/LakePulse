package com.lakepulse.data.alerts

import com.lakepulse.data.model.BuoyNames
import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

object AlertsEvaluator {
    fun resolveStationId(
        settings: AlertSettings,
        favoriteBuoyIds: Set<String>,
        available: List<BuoyObservation>,
    ): String? {
        settings.watchedStationId?.let { watched ->
            if (available.any { it.stationId == watched }) return watched
        }
        favoriteBuoyIds.firstOrNull { id -> available.any { it.stationId == id } }
            ?.let { return it }
        return available.firstOrNull()?.stationId
    }

    fun evaluateBuoy(
        settings: AlertSettings,
        buoy: BuoyObservation,
        todayKey: String = LocalDate.now(ZoneId.systemDefault()).toString(),
    ): List<AlertEvent> {
        if (!settings.masterEnabled) return emptyList()
        val events = mutableListOf<AlertEvent>()
        val name = BuoyNames.displayName(buoy.stationId)
        val temp = buoy.waterTempF.roundToInt()

        if (settings.tempBandEnabled) {
            val min = minOf(settings.tempMinF, settings.tempMaxF)
            val max = maxOf(settings.tempMinF, settings.tempMaxF)
            val inBand = buoy.waterTempF in min..max
            val fireKey = "temp|${buoy.stationId}|$todayKey"
            if (inBand && settings.lastTempFiredKey != fireKey) {
                events += AlertEvent(
                    kind = AlertKind.TEMP_BAND,
                    title = "Temp in your band · $name",
                    body = "${temp}°F is inside ${min.roundToInt()}–${max.roundToInt()}°F",
                    notificationId = NOTIFY_TEMP,
                )
            }
        }

        if (settings.calmWindEnabled) {
            val wind = buoy.windSpeedMph
            val fireKey = "wind|${buoy.stationId}|$todayKey"
            if (wind != null &&
                wind <= settings.calmWindMaxMph &&
                settings.lastWindFiredKey != fireKey
            ) {
                events += AlertEvent(
                    kind = AlertKind.CALM_WIND,
                    title = "Calm wind · $name",
                    body = "${wind.roundToInt()} mph (≤ ${settings.calmWindMaxMph.roundToInt()} mph)",
                    notificationId = NOTIFY_WIND,
                )
            }
        }

        return events
    }

    /**
     * @return events to notify, plus week ids that should be persisted as last-seen
     * (including silent seed on first run).
     */
    fun evaluateFishingWeeks(
        settings: AlertSettings,
        miWeeks: List<FishingReportWeekSummary>,
        wiWeeks: List<FishingReportWeekSummary>,
    ): FishingEvalResult {
        if (!settings.masterEnabled) {
            return FishingEvalResult(emptyList(), emptyMap())
        }
        val events = mutableListOf<AlertEvent>()
        val lastSeenUpdates = mutableMapOf<FishingSource, String>()

        fun check(
            enabled: Boolean,
            source: FishingSource,
            weeks: List<FishingReportWeekSummary>,
            lastSeen: String?,
        ) {
            if (!enabled) return
            val latest = weeks.firstOrNull() ?: return
            lastSeenUpdates[source] = latest.id
            if (lastSeen == null) {
                // First run: seed without notifying.
                return
            }
            if (latest.id != lastSeen) {
                val label = when (source) {
                    FishingSource.MICHIGAN -> "Michigan"
                    FishingSource.WISCONSIN -> "Wisconsin"
                }
                events += AlertEvent(
                    kind = AlertKind.NEW_DNR_WEEK,
                    title = "New $label DNR fishing report",
                    body = latest.title.ifBlank { "A new weekly report is available" },
                    notificationId = when (source) {
                        FishingSource.MICHIGAN -> NOTIFY_FISH_MI
                        FishingSource.WISCONSIN -> NOTIFY_FISH_WI
                    },
                )
            }
        }

        check(
            settings.fishingMiEnabled,
            FishingSource.MICHIGAN,
            miWeeks,
            settings.lastSeenMiWeekId,
        )
        check(
            settings.fishingWiEnabled,
            FishingSource.WISCONSIN,
            wiWeeks,
            settings.lastSeenWiWeekId,
        )

        return FishingEvalResult(events, lastSeenUpdates)
    }

    data class FishingEvalResult(
        val events: List<AlertEvent>,
        val lastSeenUpdates: Map<FishingSource, String>,
    )

    const val NOTIFY_TEMP = 1001
    const val NOTIFY_WIND = 1002
    const val NOTIFY_FISH_MI = 1003
    const val NOTIFY_FISH_WI = 1004
}
