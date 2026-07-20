package com.lakepulse.data.remote

import com.lakepulse.data.model.FishingRegion
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource
import com.lakepulse.data.model.withDistances
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WisconsinDnrFishingReportParserTest {

    @Test
    fun extractReportDate_fromTitle() {
        val html = """
            <title>Lake Michigan Outdoor Fishing Report - July 6, 2026 | Fishing Wisconsin | Wisconsin DNR</title>
        """.trimIndent()
        assertEquals(
            "July 6, 2026",
            WisconsinDnrFishingReportParser.extractReportDate(html),
        )
    }

    @Test
    fun parseLakeMichigan_readsCountiesAndWaterbodies() {
        val html = """
            <title>Lake Michigan Outdoor Fishing Report - July 6, 2026 | Wisconsin DNR</title>
            <h2>July 6, 2026: West Shore Green Bay Fishing Report</h2>
            <h3>Brown County</h3>
            <p><strong>Fox River</strong>: Catfish were biting on live bait.</p>
            <h3>Marinette County</h3>
            <p>Anglers trolling on the Bay saw Chinook salmon.</p>
            <h2>July 6, 2026: East Shore Green Bay Fishing Report</h2>
            <p><strong>Bayshore Park</strong>: Perch fishing was fair in 20 feet.</p>
            <h2>July 6, 2026: Southern Lake Michigan Fishing Report</h2>
            <h3>Sheboygan County</h3>
            <p>Boat anglers reported lake trout near the harbor.</p>
        """.trimIndent()

        val summary = FishingReportWeekSummary(
            id = "wi-lm",
            title = "Lake Michigan · July 6, 2026",
            publishedLabel = "July 6, 2026",
            bulletinUrl = WisconsinDnrFishingReportsClient.LAKE_MICHIGAN_URL,
            source = FishingSource.WISCONSIN,
        )

        val week = WisconsinDnrFishingReportParser.parseLakeMichigan(summary, html)

        assertTrue(week.locations.any { it.name == "Fox River" })
        assertTrue(week.locations.any { it.name == "Marinette County" })
        assertTrue(week.locations.any { it.name == "Bayshore Park" })
        assertTrue(week.locations.any { it.name == "Sheboygan County" })
        assertEquals(
            FishingRegion.WEST_SHORE_GREEN_BAY,
            week.locations.first { it.name == "Fox River" }.region,
        )
        assertEquals(
            FishingRegion.EAST_SHORE_GREEN_BAY,
            week.locations.first { it.name == "Bayshore Park" }.region,
        )
    }

    @Test
    fun parseLakeSuperior_readsPortAreas() {
        val html = """
            <title>Lake Superior Outdoor Fishing Report - June 25, 2026 | Wisconsin DNR</title>
            <h2>Apostle Islands</h2>
            <p>Brown trout action was fair near shore.</p>
            <h2>Chequamegon Bay</h2>
            <p>Walleye fishing remained good.</p>
            <h2>Superior</h2>
            <p>Coho salmon were caught trolling mudlines.</p>
            <h2>For more information, contact</h2>
            <p>Call the office.</p>
        """.trimIndent()

        val summary = FishingReportWeekSummary(
            id = "wi-superior",
            title = "Lake Superior · June 25, 2026",
            publishedLabel = "June 25, 2026",
            bulletinUrl = WisconsinDnrFishingReportsClient.LAKE_SUPERIOR_URL,
            source = FishingSource.WISCONSIN,
        )

        val week = WisconsinDnrFishingReportParser.parseLakeSuperior(summary, html)

        assertEquals(3, week.locations.size)
        assertTrue(week.locations.all { it.region == FishingRegion.LAKE_SUPERIOR })
        assertEquals("Apostle Islands", week.locations.first().name)
    }

    @Test
    fun parseLakeMichigan_uniqueIdsForRepeatedPierShoreNames() {
        val html = """
            <title>Lake Michigan Outdoor Fishing Report - July 6, 2026 | Wisconsin DNR</title>
            <h2>July 6, 2026: Southern Lake Michigan Fishing Report</h2>
            <h3>Kenosha County</h3>
            <p><strong>Pier/Shore</strong>: Shore anglers caught drum.</p>
            <h3>Racine County</h3>
            <p><strong>Pier/Shore</strong>: Pier fishing was slow.</p>
            <h3>Milwaukee County</h3>
            <p><strong>Ramp</strong>: Boat traffic was light.</p>
            <h3>Sheboygan County</h3>
            <p><strong>Ramp</strong>: Anglers reported lake trout.</p>
        """.trimIndent()
        val summary = FishingReportWeekSummary(
            id = "wi-lm",
            title = "Lake Michigan · July 6, 2026",
            publishedLabel = "July 6, 2026",
            bulletinUrl = WisconsinDnrFishingReportsClient.LAKE_MICHIGAN_URL,
            source = FishingSource.WISCONSIN,
        )

        val week = WisconsinDnrFishingReportParser.parseLakeMichigan(summary, html)
        val ids = week.locations.map { it.id }

        assertEquals(4, ids.size)
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(week.locations.any { it.name.contains("Kenosha") })
        assertTrue(week.locations.any { it.name.contains("Racine") })
    }

    @Test
    fun withDistances_sortsClosestWisconsinSpotFirst() {
        val html = """
            <h2>July 6, 2026: Southern Lake Michigan Fishing Report</h2>
            <h3>Kenosha County</h3>
            <p>Shore fishing was slow.</p>
            <h2>July 6, 2026: West Shore Green Bay Fishing Report</h2>
            <h3>Brown County</h3>
            <p>Catfish were active.</p>
        """.trimIndent()
        val summary = FishingReportWeekSummary(
            id = "wi-lm",
            title = "Lake Michigan",
            publishedLabel = "",
            bulletinUrl = WisconsinDnrFishingReportsClient.LAKE_MICHIGAN_URL,
            source = FishingSource.WISCONSIN,
        )
        val week = WisconsinDnrFishingReportParser.parseLakeMichigan(summary, html)

        // Near Green Bay / Brown County
        val ranked = week.withDistances(userLat = 44.52, userLon = -88.00)

        assertEquals("Brown County", ranked.locations.first().name)
    }
}
