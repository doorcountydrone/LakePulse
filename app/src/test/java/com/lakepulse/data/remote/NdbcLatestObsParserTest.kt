package com.lakepulse.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NdbcLatestObsParserTest {

    @Test
    fun parse_filtersGreatLakesBuoysWithWaterTemp() {
        val sample = """
            #STN       LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE
            #text      deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     ft
            45001    48.061  -87.793 2026 07 14 22 50 250   2.0   2.0  0.6   5  3.9 228 1015.8    MM   7.7   3.7    MM   MM     MM
            45004    47.583  -86.586 2026 07 14 22 50 290   4.0   4.0  0.8   5  4.1 276 1016.3    MM   7.0    MM   7.0   MM     MM
            45140    50.800  -96.730 2026 07 14 23 00 270   3.0    MM  0.7  20   MM  MM 1019.3  -1.0  26.5  18.0    MM   MM     MM
            45005    41.677  -82.398 2026 07 14 22 50 230   5.0   6.0  0.3   3  2.5 230 1018.1    MM  28.2  26.3  25.1   MM     MM
        """.trimIndent()

        val buoys = NdbcLatestObsParser.parse(sample)

        assertEquals(2, buoys.size)
        assertTrue(buoys.any { it.stationId == "45001" })
        assertTrue(buoys.any { it.stationId == "45005" })

        val superior = buoys.first { it.stationId == "45001" }
        assertEquals(38.66, superior.waterTempF, 0.05)
        assertEquals(48.061, superior.latitude, 0.001)
    }
}
