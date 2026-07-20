package com.lakepulse.data.remote

import com.lakepulse.data.model.FishingReportWeek
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource

class WisconsinDnrFishingReportsClient(
    private val httpGet: (String) -> String = HttpClient::get,
) {
    @Volatile
    private var cachedSummaries: List<FishingReportWeekSummary>? = null

    private val weekCache = mutableMapOf<String, FishingReportWeek>()

    fun getRecentReports(forceRefresh: Boolean = false): List<FishingReportWeekSummary> {
        if (!forceRefresh) {
            cachedSummaries?.let { return it }
        }
        val lakeMichiganHtml = httpGet(LAKE_MICHIGAN_URL)
        val lakeSuperiorHtml = httpGet(LAKE_SUPERIOR_URL)
        val lmDate = WisconsinDnrFishingReportParser.extractReportDate(lakeMichiganHtml)
            .ifBlank { "Latest" }
        val superiorDate = WisconsinDnrFishingReportParser.extractReportDate(lakeSuperiorHtml)
            .ifBlank { "Latest" }

        val summaries = listOf(
            FishingReportWeekSummary(
                id = ID_LAKE_MICHIGAN,
                title = "Lake Michigan · $lmDate",
                publishedLabel = lmDate,
                bulletinUrl = LAKE_MICHIGAN_URL,
                source = FishingSource.WISCONSIN,
            ),
            FishingReportWeekSummary(
                id = ID_LAKE_SUPERIOR,
                title = "Lake Superior · $superiorDate",
                publishedLabel = superiorDate,
                bulletinUrl = LAKE_SUPERIOR_URL,
                source = FishingSource.WISCONSIN,
            ),
        )

        // Warm cache while we already have HTML.
        weekCache[ID_LAKE_MICHIGAN] = WisconsinDnrFishingReportParser.parseLakeMichigan(
            summaries[0],
            lakeMichiganHtml,
        )
        weekCache[ID_LAKE_SUPERIOR] = WisconsinDnrFishingReportParser.parseLakeSuperior(
            summaries[1],
            lakeSuperiorHtml,
        )
        cachedSummaries = summaries
        return summaries
    }

    fun getReport(
        summary: FishingReportWeekSummary,
        forceRefresh: Boolean = false,
    ): FishingReportWeek {
        if (!forceRefresh) {
            weekCache[summary.id]?.let { return it }
        }
        val html = httpGet(summary.bulletinUrl)
        val week = when (summary.id) {
            ID_LAKE_SUPERIOR -> WisconsinDnrFishingReportParser.parseLakeSuperior(summary, html)
            else -> WisconsinDnrFishingReportParser.parseLakeMichigan(summary, html)
        }
        weekCache[summary.id] = week
        return week
    }

    companion object {
        const val ID_LAKE_MICHIGAN = "wi-lm"
        const val ID_LAKE_SUPERIOR = "wi-superior"
        const val LAKE_MICHIGAN_URL =
            "https://dnr.wisconsin.gov/topic/Fishing/lakemichigan/OutdoorReport"
        const val LAKE_SUPERIOR_URL =
            "https://dnr.wisconsin.gov/topic/Fishing/lakesuperior/OutdoorReport"
    }
}
