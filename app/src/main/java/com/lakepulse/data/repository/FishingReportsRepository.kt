package com.lakepulse.data.repository

import com.lakepulse.data.location.UserLocation
import com.lakepulse.data.model.FishingReportWeek
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource
import com.lakepulse.data.model.withDistances
import com.lakepulse.data.remote.MichiganDnrFishingReportsClient
import com.lakepulse.data.remote.NetworkModule
import com.lakepulse.data.remote.WisconsinDnrFishingReportsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FishingReportsRepository(
    private val michiganClient: MichiganDnrFishingReportsClient =
        NetworkModule.fishingReportsClient,
    private val wisconsinClient: WisconsinDnrFishingReportsClient =
        NetworkModule.wisconsinFishingReportsClient,
) {
    suspend fun getRecentWeeks(
        source: FishingSource,
        forceRefresh: Boolean = false,
    ): List<FishingReportWeekSummary> = withContext(Dispatchers.IO) {
        when (source) {
            FishingSource.MICHIGAN -> michiganClient.getRecentWeeks(forceRefresh)
            FishingSource.WISCONSIN -> wisconsinClient.getRecentReports(forceRefresh)
        }
    }

    suspend fun getWeek(
        summary: FishingReportWeekSummary,
        userLocation: UserLocation? = null,
        forceRefresh: Boolean = false,
    ): FishingReportWeek = withContext(Dispatchers.IO) {
        val week = when (summary.source) {
            FishingSource.MICHIGAN -> michiganClient.getWeek(summary, forceRefresh)
            FishingSource.WISCONSIN -> wisconsinClient.getReport(summary, forceRefresh)
        }
        week.withDistances(
            userLat = userLocation?.latitude,
            userLon = userLocation?.longitude,
        )
    }
}
