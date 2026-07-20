package com.lakepulse.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FishingBiteSummaryTest {

    @Test
    fun slowChinookReport() {
        val summary = FishingBiteSummary.from(
            "Boat anglers found salmon action to be slow. A few Chinook salmon were caught " +
                "60 to 100 feet down. Lake trout were caught near the bottom.",
        )
        assertTrue(summary.contains("Chinook salmon"))
        assertTrue(summary.contains("lake trout"))
        assertTrue(summary.endsWith("· slow") || summary.contains("· slow"))
    }

    @Test
    fun walleyeLimits() {
        val summary = FishingBiteSummary.from(
            "Walleye fishing was excellent over the weekend. Limit catches were common.",
        )
        assertEquals("walleye · limits", summary)
    }

    @Test
    fun perchFair() {
        val summary = FishingBiteSummary.from(
            "Anglers targeting yellow perch had fair success in 12 feet of water.",
        )
        assertEquals("yellow perch · fair", summary)
    }

    @Test
    fun mixedWhenHotAndSlowPresent() {
        val summary = FishingBiteSummary.from(
            "Chinook salmon fishing was excellent early in the week, but overall fishing was slow.",
        )
        assertTrue(summary.contains("Chinook salmon"))
        assertTrue(summary.contains("mixed"))
    }
}
