package com.lakepulse.data.alerts

import android.content.Context
import com.lakepulse.data.favorites.FavoritesStore
import com.lakepulse.data.model.FishingSource
import com.lakepulse.data.remote.NetworkModule
import com.lakepulse.data.repository.FishingReportsRepository
import java.time.LocalDate
import java.time.ZoneId

/**
 * Shared alert evaluation used by background alarms and “Check now”.
 */
object AlertChecker {
    suspend fun run(context: Context) {
        val store = AlertsStore(context)
        val settings = store.snapshot()
        if (!settings.masterEnabled) return

        val favorites = FavoritesStore(context)
        val buoys = NetworkModule.ndbcLatestObsClient.fetchGreatLakes(maxAgeMs = 0L)
        val stationId = AlertsEvaluator.resolveStationId(
            settings = settings,
            favoriteBuoyIds = favorites.favoriteBuoyIds(),
            available = buoys,
        )
        val today = LocalDate.now(ZoneId.systemDefault()).toString()

        if (stationId != null) {
            val buoy = buoys.firstOrNull { it.stationId == stationId }
            if (buoy != null) {
                AlertsEvaluator.evaluateBuoy(settings, buoy, today).forEach { event ->
                    LakePulseNotifier.notify(context, event)
                    when (event.kind) {
                        AlertKind.TEMP_BAND ->
                            store.setLastTempFiredKey("temp|${buoy.stationId}|$today")
                        AlertKind.CALM_WIND ->
                            store.setLastWindFiredKey("wind|${buoy.stationId}|$today")
                        AlertKind.NEW_DNR_WEEK -> Unit
                    }
                }
            }
        }

        val fishingRepo = FishingReportsRepository()
        val miWeeks = if (settings.fishingMiEnabled) {
            runCatching { fishingRepo.getRecentWeeks(FishingSource.MICHIGAN) }
                .getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val wiWeeks = if (settings.fishingWiEnabled) {
            runCatching { fishingRepo.getRecentWeeks(FishingSource.WISCONSIN) }
                .getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val fishing = AlertsEvaluator.evaluateFishingWeeks(settings, miWeeks, wiWeeks)
        fishing.events.forEach { LakePulseNotifier.notify(context, it) }
        fishing.lastSeenUpdates.forEach { (source, weekId) ->
            store.setLastSeenWeek(source, weekId)
        }
    }
}
