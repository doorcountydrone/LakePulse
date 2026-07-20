package com.lakepulse.data.remote

import com.lakepulse.data.model.FishingLocationReport
import com.lakepulse.data.model.FishingLocations
import com.lakepulse.data.model.FishingRegion
import com.lakepulse.data.model.FishingRegionReport
import com.lakepulse.data.model.FishingReportWeek
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource
import com.lakepulse.data.model.ensuringUniqueIds
import java.util.Locale

object WisconsinDnrFishingReportParser {
    private val h2Regex = Regex(
        """<h2[^>]*>(.*?)</h2>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val h3Regex = Regex(
        """<h3[^>]*>(.*?)</h3>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val strongLocationRegex = Regex(
        """<(?:strong|b)[^>]*>\s*([^<]+?)\s*:?\s*</(?:strong|b)>\s*:?\s*(.*?)(?=</p>|$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val titleRegex = Regex(
        """<title>\s*([^<]+?)\s*</title>""",
        RegexOption.IGNORE_CASE,
    )
    private val dateInTitleRegex = Regex(
        """((?:January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},\s+\d{4})""",
        RegexOption.IGNORE_CASE,
    )
    private val tagRegex = Regex("""<[^>]+>""")
    private val whitespaceRegex = Regex("""\s+""")

    private val superiorLocationHeadings = listOf(
        "Apostle Islands",
        "Chequamegon Bay",
        "Cornucopia/Port Wing",
        "Saxon",
        "Superior",
    )

    fun extractReportDate(html: String): String {
        val title = titleRegex.find(html)?.groupValues?.get(1).orEmpty()
        return dateInTitleRegex.find(title)?.groupValues?.get(1)?.trim().orEmpty()
    }

    fun parseLakeMichigan(
        summary: FishingReportWeekSummary,
        html: String,
    ): FishingReportWeek {
        val reportSections = h2Regex.findAll(html).mapNotNull { match ->
            val heading = decodeHtml(stripTags(match.groupValues[1]))
            val region = FishingRegion.fromHeading(heading)
            if (!isLakeMichiganRegionHeading(heading, region)) return@mapNotNull null
            match to region
        }.toList()

        val regions = mutableListOf<FishingRegionReport>()
        val allLocations = mutableListOf<FishingLocationReport>()

        reportSections.forEachIndexed { index, (match, region) ->
            val sectionStart = match.range.last + 1
            val sectionEnd = reportSections.getOrNull(index + 1)?.first?.range?.first
                ?: nextNoiseBoundary(html, sectionStart)
            val sectionHtml = html.substring(sectionStart, sectionEnd)
            val locations = parseMichiganSection(sectionHtml, region, summary.id)
            if (locations.isNotEmpty()) {
                regions += FishingRegionReport(region, locations)
                allLocations += locations
            }
        }

        return FishingReportWeek(
            summary = summary,
            regions = regions,
            locations = allLocations,
        ).ensuringUniqueIds()
    }

    fun parseLakeSuperior(
        summary: FishingReportWeekSummary,
        html: String,
    ): FishingReportWeek {
        val locations = mutableListOf<FishingLocationReport>()
        val h2Matches = h2Regex.findAll(html).toList()

        h2Matches.forEachIndexed { index, match ->
            val heading = decodeHtml(stripTags(match.groupValues[1]))
            val placeName = superiorLocationHeadings.firstOrNull {
                normalize(heading) == normalize(it)
            } ?: return@forEachIndexed

            val sectionStart = match.range.last + 1
            val sectionEnd = h2Matches.getOrNull(index + 1)?.range?.first ?: html.length
            val body = decodeHtml(stripTags(html.substring(sectionStart, sectionEnd))).trim()
            if (body.isBlank()) return@forEachIndexed

            val place = FishingLocations.resolve(placeName, FishingRegion.LAKE_SUPERIOR)
            locations += FishingLocationReport(
                id = "${summary.id}:${slug(placeName)}",
                name = placeName,
                region = FishingRegion.LAKE_SUPERIOR,
                body = body,
                latitude = place.latitude,
                longitude = place.longitude,
                source = FishingSource.WISCONSIN,
            )
        }

        return FishingReportWeek(
            summary = summary,
            regions = listOf(
                FishingRegionReport(FishingRegion.LAKE_SUPERIOR, locations),
            ),
            locations = locations,
        ).ensuringUniqueIds()
    }

    private fun parseMichiganSection(
        sectionHtml: String,
        region: FishingRegion,
        weekId: String,
    ): List<FishingLocationReport> {
        val countyMatches = h3Regex.findAll(sectionHtml).toList()
        if (countyMatches.isEmpty()) {
            return parseStrongLocations(sectionHtml, region, weekId, areaContext = null)
        }

        val locations = mutableListOf<FishingLocationReport>()
        countyMatches.forEachIndexed { index, match ->
            val county = decodeHtml(stripTags(match.groupValues[1])).trim()
            if (county.isBlank() || looksLikeNonLocation(county)) return@forEachIndexed
            val start = match.range.last + 1
            val end = countyMatches.getOrNull(index + 1)?.range?.first ?: sectionHtml.length
            val countyHtml = sectionHtml.substring(start, end)
            val strongLocations = parseStrongLocations(
                html = countyHtml,
                region = region,
                weekId = weekId,
                areaContext = county,
            )
            if (strongLocations.isNotEmpty()) {
                locations += strongLocations
            } else {
                val body = decodeHtml(stripTags(countyHtml)).trim()
                if (body.isNotBlank()) {
                    val place = FishingLocations.resolve(county, region)
                    locations += FishingLocationReport(
                        id = "$weekId:${region.name}:${slug(county)}",
                        name = county,
                        region = region,
                        body = body,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        source = FishingSource.WISCONSIN,
                    )
                }
            }
        }
        return locations
    }

    private fun parseStrongLocations(
        html: String,
        region: FishingRegion,
        weekId: String,
        areaContext: String?,
    ): List<FishingLocationReport> =
        strongLocationRegex.findAll(html).mapNotNull { match ->
            val rawName = decodeHtml(stripTags(match.groupValues[1])).trim().trimEnd(':')
            val body = decodeHtml(stripTags(match.groupValues[2])).trim()
            if (rawName.isBlank() || body.isBlank()) return@mapNotNull null
            if (looksLikeNonLocation(rawName)) return@mapNotNull null
            val place = FishingLocations.resolve(
                areaContext?.takeIf { isGenericSpotName(rawName) } ?: rawName,
                region,
            )
            val displayName = when {
                areaContext != null && isGenericSpotName(rawName) ->
                    "$rawName · ${areaContext.removeSuffix(" County")}"
                else -> rawName
            }
            val id = buildString {
                append(weekId)
                append(':')
                append(region.name)
                if (!areaContext.isNullOrBlank()) {
                    append(':')
                    append(slug(areaContext))
                }
                append(':')
                append(slug(rawName))
            }
            FishingLocationReport(
                id = id,
                name = displayName,
                region = region,
                body = body,
                latitude = place.latitude,
                longitude = place.longitude,
                source = FishingSource.WISCONSIN,
            )
        }.toList()

    private fun isGenericSpotName(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower == "pier/shore" ||
            lower == "ramp" ||
            lower == "harbor" ||
            lower == "marina" ||
            lower == "shore" ||
            lower == "pier"
    }

    private fun isLakeMichiganRegionHeading(heading: String, region: FishingRegion): Boolean {
        if (region == FishingRegion.WEST_SHORE_GREEN_BAY ||
            region == FishingRegion.EAST_SHORE_GREEN_BAY ||
            region == FishingRegion.NORTHERN_LAKE_MICHIGAN ||
            region == FishingRegion.SOUTHERN_LAKE_MICHIGAN
        ) {
            return true
        }
        val lower = heading.lowercase(Locale.US)
        return lower.contains("fishing report") &&
            (lower.contains("green bay") || lower.contains("lake michigan"))
    }

    private fun nextNoiseBoundary(html: String, from: Int): Int {
        val markers = listOf(
            "Fishing_Topic Contact",
            "For more information, contact",
            "id=\"footer\"",
            "class=\"footer\"",
        )
        val tail = html.substring(from)
        return markers.mapNotNull { marker ->
            val relative = tail.indexOf(marker, ignoreCase = true)
            if (relative >= 0) from + relative else null
        }.minOrNull() ?: html.length
    }

    private fun looksLikeNonLocation(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower.contains("fishing tip") ||
            lower.contains("more information") ||
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
            .replace("&rsquo;", "'")

    private fun slug(value: String): String =
        value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    private fun normalize(value: String): String =
        value.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
}
