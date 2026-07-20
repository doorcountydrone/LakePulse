package com.lakepulse.data.remote

import com.lakepulse.data.model.BuoyTempSample
import com.lakepulse.data.model.BuoyTempTrend
import com.lakepulse.data.model.celsiusToFahrenheit
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object NdbcRealtimeHistoryParser {
    fun parseTempTrend(
        stationId: String,
        raw: String,
        window: Duration = Duration.ofHours(24),
        now: Instant = Instant.now(),
    ): BuoyTempTrend {
        val cutoff = now.minus(window)
        val samples = ArrayList<BuoyTempSample>()

        for (line in raw.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val sample = parseLine(trimmed) ?: continue
            if (sample.observedAt.isBefore(cutoff)) break // file is newest-first
            samples.add(sample)
        }

        // Oldest → newest for charting
        samples.reverse()
        return BuoyTempTrend(stationId = stationId, samples = samples)
    }

    private fun parseLine(line: String): BuoyTempSample? {
        val parts = line.split(Regex("\\s+"))
        if (parts.size < 15) return null

        val waterC = parts[14].toDoubleOrNull() ?: return null
        val observedAt = parseUtcInstant(parts) ?: return null
        return BuoyTempSample(
            observedAt = observedAt,
            waterTempF = waterC.celsiusToFahrenheit(),
        )
    }

    private fun parseUtcInstant(parts: List<String>): Instant? {
        val yearRaw = parts[0].toIntOrNull() ?: return null
        val year = if (yearRaw < 100) 2000 + yearRaw else yearRaw
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        val hour = parts[3].toIntOrNull() ?: return null
        val minute = parts[4].toIntOrNull() ?: return null
        return runCatching {
            LocalDateTime.of(year, month, day, hour, minute)
                .toInstant(ZoneOffset.UTC)
        }.getOrNull()
    }
}
