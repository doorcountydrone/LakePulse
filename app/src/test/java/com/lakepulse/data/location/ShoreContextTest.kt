package com.lakepulse.data.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoreContextTest {

    @Test
    fun sturgeonBay_isNearShore() {
        // Door County / Sturgeon Bay
        assertTrue(isNearGreatLakesShore(44.83, -87.38))
    }

    @Test
    fun chicagoLoop_isNearShore() {
        assertTrue(isNearGreatLakesShore(41.88, -87.63))
    }

    @Test
    fun inlandMadison_isNotNearShore() {
        assertFalse(isNearGreatLakesShore(43.07, -89.40))
    }

    @Test
    fun kansasCity_isNotNearShore() {
        assertFalse(isNearGreatLakesShore(39.10, -94.58))
    }
}
