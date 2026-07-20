package com.lakepulse.data.remote

import com.lakepulse.data.model.FishingRegion
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.withDistances
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MichiganDnrFishingReportParserTest {

    @Test
    fun parseWeekList_readsJsonpWidgetPayload() {
        val jsonp = """
            GDWidgets[0].update([{"subject":"Weekly Fishing Report: July 8, 2026","pub_date":"07/08/2026 04:39 PM EDT","href":"https://content.govdelivery.com/bulletins/gd/MIDNR-41fa7c2?wgt_ref=MIDNR_WIDGET_2"},{"subject":"Weekly Fishing Report: July 1, 2026","pub_date":"07/01/2026 04:30 PM EDT","href":"https://content.govdelivery.com/bulletins/gd/MIDNR-41e9e6e?wgt_ref=MIDNR_WIDGET_2"}]);
        """.trimIndent()

        val weeks = MichiganDnrFishingReportParser.parseWeekList(jsonp)

        assertEquals(2, weeks.size)
        assertEquals("41fa7c2", weeks[0].id)
        assertEquals("July 8, 2026", weeks[0].title)
        assertTrue(weeks[0].bulletinUrl.contains("/accounts/MIDNR/bulletins/41fa7c2"))
    }

    @Test
    fun parseBulletin_extractsLocationsByRegion() {
        val html = """
            <h2>Southeast Lower Peninsula</h2>
            <p><strong>Sebewaing: </strong>Walleye were caught on the bar.</p>
            <p><strong>Lake Erie:</strong> Bass fishing was fair.</p>
            <h2>Southwest Lower Peninsula</h2>
            <p><strong>Muskegon</strong> Salmon action was slow.</p>
            <p><strong>Grand Haven:</strong> Pier anglers caught drum.</p>
            <h2>Fishing tip: Using a float</h2>
            <p><strong>Fishing tip:</strong> Floats can help.</p>
        """.trimIndent()

        val summary = FishingReportWeekSummary(
            id = "41fa7c2",
            title = "July 8, 2026",
            publishedLabel = "07/08/2026",
            bulletinUrl = "https://content.govdelivery.com/accounts/MIDNR/bulletins/41fa7c2",
        )

        val week = MichiganDnrFishingReportParser.parseBulletin(summary, html)

        assertEquals(4, week.locations.size)
        assertTrue(week.locations.any { it.name == "Sebewaing" })
        assertTrue(week.locations.any { it.name == "Muskegon" })
        assertEquals(
            FishingRegion.SOUTHEAST_LOWER,
            week.locations.first { it.name == "Lake Erie" }.region,
        )
        assertEquals(
            FishingRegion.SOUTHWEST_LOWER,
            week.locations.first { it.name == "Grand Haven" }.region,
        )
        assertTrue(week.locations.none { it.name.contains("tip", ignoreCase = true) })
    }

    @Test
    fun withDistances_sortsClosestFirst() {
        val summary = FishingReportWeekSummary(
            id = "x",
            title = "Test",
            publishedLabel = "",
            bulletinUrl = "https://example.com",
        )
        val html = """
            <h2>Northwest Lower Peninsula</h2>
            <p><strong>Marquette:</strong> Far UP report.</p>
            <h2>Southwest Lower Peninsula</h2>
            <p><strong>Grand Haven:</strong> Nearby report.</p>
        """.trimIndent()
        val week = MichiganDnrFishingReportParser.parseBulletin(summary, html)

        // Near Grand Haven
        val ranked = week.withDistances(
            userLat = 43.06,
            userLon = -86.23,
        )

        assertEquals("Grand Haven", ranked.locations.first().name)
        assertTrue((ranked.locations.first().distanceMiles ?: 999.0) < 5.0)
    }
}
