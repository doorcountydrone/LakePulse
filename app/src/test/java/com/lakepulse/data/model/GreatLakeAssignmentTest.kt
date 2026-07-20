package com.lakepulse.data.model

import com.lakepulse.data.location.UserLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GreatLakeAssignmentTest {

    @Test
    fun assignsKnownBuoysToExpectedLakes() {
        assertEquals(GreatLake.SUPERIOR, GreatLake.containing(48.061, -87.793))
        assertEquals(GreatLake.MICHIGAN, GreatLake.containing(42.9, -86.272))
        assertEquals(GreatLake.HURON, GreatLake.containing(45.54, -81.02))
        assertEquals(GreatLake.ERIE, GreatLake.containing(41.677, -82.398))
        assertEquals(GreatLake.ONTARIO, GreatLake.containing(43.78, -76.87))
        assertNull(GreatLake.containing(50.8, -96.73))
    }

    @Test
    fun toLakeBoard_sortsAndSummarizes() {
        val buoys = listOf(
            BuoyObservation("a", 42.9, -86.3, 70.0, 20.0, 25.0, 200, 2.0, null),
            BuoyObservation("b", 43.1, -87.8, 60.0, 8.0, 10.0, 180, 0.5, null),
            BuoyObservation("c", 41.7, -82.4, 80.0, 12.0, 15.0, 220, 1.0, null),
        )
        val michigan = buoys.toLakeBoard(GreatLake.MICHIGAN, BuoySort.TEMP_COLD)
        assertEquals(2, michigan.buoyCount)
        assertEquals("b", michigan.buoys.first().stationId)
        assertEquals(60.0, michigan.minTempF!!, 0.01)
        assertEquals(70.0, michigan.maxTempF!!, 0.01)
        assertEquals("a", michigan.windiest?.stationId)
        assertEquals("a", michigan.biggestWaves?.stationId)
    }

    @Test
    fun toLakeBoard_sortsNearestFirst() {
        val buoys = listOf(
            BuoyObservation("far", 45.0, -86.5, 65.0, 8.0, 10.0, 180, 0.5, null),
            BuoyObservation("near", 42.9, -86.3, 70.0, 12.0, 14.0, 200, 1.0, null),
        )
        // Grand Rapids-ish
        val user = UserLocation(latitude = 42.96, longitude = -85.67)
        val board = buoys.toLakeBoard(GreatLake.MICHIGAN, BuoySort.NEAREST, user)
        assertEquals("near", board.buoys.first().stationId)
        assertTrue(board.sortedByDistance)
        assertTrue(board.buoys.first().distanceMiles!! < board.buoys.last().distanceMiles!!)
    }
}
