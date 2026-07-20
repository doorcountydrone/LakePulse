package com.lakepulse.data.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Relative “how old is this?” labels for buoy / satellite / report timestamps.
 */
object RelativeTime {
    fun parseInstant(raw: String): Instant? {
        return runCatching {
            val normalized = raw.trim()
                .replace(' ', 'T')
                .let { value ->
                    when {
                        value.endsWith("Z", ignoreCase = true) -> value
                        value.endsWith("UTC", ignoreCase = true) ->
                            value.dropLast(3).trimEnd() + "Z"
                        else -> value + "Z"
                    }
                }
                .let { value ->
                    // NDBC strings are often "yyyy-MM-ddTHH:mmZ" without seconds.
                    if (value.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}Z""", RegexOption.IGNORE_CASE))) {
                        value.dropLast(1) + ":00Z"
                    } else {
                        value
                    }
                }
            Instant.parse(normalized)
        }.getOrNull()
    }

    fun formatAge(
        from: Instant,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = formatAge(Duration.between(from, now), from, now, zone)

    fun formatAge(duration: Duration): String =
        formatAge(duration, from = null, now = Instant.now(), zone = ZoneId.systemDefault())

    /**
     * Human label for an NDBC-style `observedAt` string.
     * Prefers relative age; falls back to the raw string when unparseable.
     */
    fun observationAgeLabel(
        raw: String?,
        unknown: String = "Observation time unknown",
    ): String {
        if (raw.isNullOrBlank()) return unknown
        val instant = parseInstant(raw) ?: return raw
        return formatAge(instant)
    }

    /** Freshest (most recent) observation age across a set of raw timestamps, or null. */
    fun freshestAgeLabel(rawTimes: Iterable<String?>): String? {
        val newest = rawTimes
            .mapNotNull { raw -> raw?.let(::parseInstant) }
            .maxOrNull()
            ?: return null
        return formatAge(newest)
    }

    private fun formatAge(
        duration: Duration,
        from: Instant?,
        now: Instant,
        zone: ZoneId,
    ): String {
        val safe = if (duration.isNegative) Duration.ZERO else duration
        val minutes = safe.toMinutes()
        val hours = safe.toHours()
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 36 -> {
                if (from != null && isYesterday(from, now, zone)) {
                    "yesterday"
                } else {
                    "${hours}h ago"
                }
            }
            from != null && isYesterday(from, now, zone) -> "yesterday"
            else -> {
                val days = (hours / 24).coerceAtLeast(1)
                "${days}d ago"
            }
        }
    }

    private fun isYesterday(from: Instant, now: Instant, zone: ZoneId): Boolean {
        val fromDay = LocalDate.ofInstant(from, zone)
        val today = LocalDate.ofInstant(now, zone)
        return fromDay == today.minusDays(1)
    }
}

/** Convenience for call sites that already hold a nullable raw timestamp. */
fun String?.toObservationAgeLabel(
    unknown: String = "Observation time unknown",
): String = RelativeTime.observationAgeLabel(this, unknown)
