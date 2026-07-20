package com.lakepulse.data.remote

import com.lakepulse.data.model.FishingReportWeek
import com.lakepulse.data.model.FishingReportWeekSummary

class MichiganDnrFishingReportsClient(
    private val httpGet: (String) -> String = HttpClient::get,
) {
    @Volatile
    private var cachedWeeks: List<FishingReportWeekSummary>? = null

    private val bulletinCache = mutableMapOf<String, FishingReportWeek>()

    fun getRecentWeeks(forceRefresh: Boolean = false): List<FishingReportWeekSummary> {
        if (!forceRefresh) {
            cachedWeeks?.let { return it }
        }
        val payload = httpGet(WIDGET_JSON_URL)
        val weeks = MichiganDnrFishingReportParser.parseWeekList(payload)
        cachedWeeks = weeks
        return weeks
    }

    fun getWeek(
        summary: FishingReportWeekSummary,
        forceRefresh: Boolean = false,
    ): FishingReportWeek {
        if (!forceRefresh) {
            bulletinCache[summary.id]?.let { return it }
        }
        val html = httpGet(summary.bulletinUrl)
        val week = MichiganDnrFishingReportParser.parseBulletin(summary, html)
        bulletinCache[summary.id] = week
        return week
    }

    companion object {
        const val WIDGET_JSON_URL =
            "https://content.govdelivery.com/accounts/MIDNR/widgets/MIDNR_WIDGET_2/0.json"
        const val SOURCE_PAGE =
            "https://www.michigan.gov/dnr/things-to-do/fishing/weekly"
    }
}
