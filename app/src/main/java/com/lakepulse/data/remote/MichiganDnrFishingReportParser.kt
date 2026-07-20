package com.lakepulse.data.remote

import com.lakepulse.data.model.FishingLocationReport
import com.lakepulse.data.model.FishingLocations
import com.lakepulse.data.model.FishingRegion
import com.lakepulse.data.model.FishingRegionReport
import com.lakepulse.data.model.FishingReportWeek
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource
import com.lakepulse.data.model.ensuringUniqueIds
import com.lakepulse.data.model.withDistances
import java.util.Locale

object MichiganDnrFishingReportParser {
    private val strongLocationRegex = Regex(
        """<(?:strong|b)[^>]*>\s*([^<]+?)\s*:?\s*</(?:strong|b)>\s*(.*?)(?=</p>|$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val h2Regex = Regex(
        """<h2[^>]*>(.*?)</h2>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val weekItemRegex = Regex(
        """\{[^{}]*"subject"\s*:\s*"((?:\\.|[^"\\])*)"[^{}]*\}""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val jsonStringFieldRegex = { field: String ->
        Regex(""""$field"\s*:\s*"((?:\\.|[^"\\])*)"""")
    }
    private val tagRegex = Regex("""<[^>]+>""")
    private val whitespaceRegex = Regex("""\s+""")

    fun parseWeekList(jsonp: String): List<FishingReportWeekSummary> {
        val start = jsonp.indexOf('[')
        val end = jsonp.lastIndexOf(']')
        require(start >= 0 && end > start) { "Unexpected fishing report widget payload" }
        val arrayBody = jsonp.substring(start, end + 1)
        return weekItemRegex.findAll(arrayBody).map { match ->
            val obj = match.value
            val subject = jsonString(obj, "subject")
                ?: error("Missing subject in fishing report widget item")
            val href = jsonString(obj, "href")
                ?: error("Missing href in fishing report widget item")
            val id = bulletinIdFromUrl(href)
            FishingReportWeekSummary(
                id = id,
                title = subject.removePrefix("Weekly Fishing Report: ").trim(),
                publishedLabel = jsonString(obj, "pub_date").orEmpty().trim(),
                bulletinUrl = normalizeBulletinUrl(href, id),
                source = FishingSource.MICHIGAN,
            )
        }.toList()
    }

    private fun jsonString(obj: String, field: String): String? =
        jsonStringFieldRegex(field).find(obj)?.groupValues?.get(1)?.unescapeJson()

    private fun String.unescapeJson(): String =
        replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")


    fun parseBulletin(
        summary: FishingReportWeekSummary,
        html: String,
    ): FishingReportWeek {
        val h2Matches = h2Regex.findAll(html).toList()
        val regions = mutableListOf<FishingRegionReport>()
        val allLocations = mutableListOf<FishingLocationReport>()

        h2Matches.forEachIndexed { index, match ->
            val heading = decodeHtml(stripTags(match.groupValues[1]))
            val region = FishingRegion.fromHeading(heading)
            if (region == FishingRegion.UNKNOWN &&
                !heading.contains("peninsula", ignoreCase = true)
            ) {
                return@forEachIndexed
            }
            val sectionStart = match.range.last + 1
            val sectionEnd = h2Matches.getOrNull(index + 1)?.range?.first ?: html.length
            val sectionHtml = html.substring(sectionStart, sectionEnd)
            val locations = parseLocations(sectionHtml, region, summary.id)
            if (locations.isNotEmpty()) {
                regions += FishingRegionReport(region = region, locations = locations)
                allLocations += locations
            }
        }

        if (allLocations.isEmpty()) {
            // Fallback: parse whole document when region headings are missing.
            val fallback = parseLocations(html, FishingRegion.UNKNOWN, summary.id)
            if (fallback.isNotEmpty()) {
                regions += FishingRegionReport(FishingRegion.UNKNOWN, fallback)
                allLocations += fallback
            }
        }

        return FishingReportWeek(
            summary = summary,
            regions = regions,
            locations = allLocations,
        ).ensuringUniqueIds()
    }

    fun withDistances(
        week: FishingReportWeek,
        userLat: Double?,
        userLon: Double?,
    ): FishingReportWeek = week.withDistances(userLat, userLon)

    private fun parseLocations(
        sectionHtml: String,
        region: FishingRegion,
        weekId: String,
    ): List<FishingLocationReport> {
        return strongLocationRegex.findAll(sectionHtml).mapNotNull { match ->
            val rawName = decodeHtml(stripTags(match.groupValues[1])).trim().trimEnd(':')
            val body = decodeHtml(stripTags(match.groupValues[2])).trim()
            if (rawName.isBlank() || body.isBlank()) return@mapNotNull null
            if (looksLikeNonLocation(rawName)) return@mapNotNull null

            val place = FishingLocations.resolve(rawName, region)
            FishingLocationReport(
                id = "$weekId:${region.name}:${slug(rawName)}",
                name = rawName,
                region = region,
                body = body,
                latitude = place.latitude,
                longitude = place.longitude,
                source = FishingSource.MICHIGAN,
            )
        }.toList()
    }

    private fun looksLikeNonLocation(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower.contains("fishing tip") ||
            lower.contains("back to top") ||
            lower.contains("subscribe") ||
            lower.length > 60
    }

    private fun stripTags(value: String): String =
        value.replace(tagRegex, " ")
            .replace("&nbsp;", " ")
            .replace(whitespaceRegex, " ")
            .trim()

    private fun decodeHtml(value: String): String =
        value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&rsquo;", "'")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")

    private fun slug(value: String): String =
        value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    internal fun bulletinIdFromUrl(url: String): String {
        Regex("""MIDNR-([a-z0-9]+)""", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1)
            ?.let { return it }
        Regex("""/bulletins/([a-z0-9]+)""", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1)
            ?.let { return it }
        return url.hashCode().toString()
    }

    internal fun normalizeBulletinUrl(href: String, id: String): String {
        if (href.contains("/accounts/MIDNR/bulletins/")) return href.substringBefore('?')
        return "https://content.govdelivery.com/accounts/MIDNR/bulletins/$id"
    }
}
