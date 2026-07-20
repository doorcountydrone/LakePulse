package com.lakepulse.data.model

/**
 * One-line “hot bite” skim line from a DNR fishing report paragraph.
 * Example: "Chinook salmon, lake trout · slow"
 */
object FishingBiteSummary {
    private data class Species(
        val label: String,
        val patterns: List<Regex>,
    )

    private val speciesCatalog = listOf(
        Species(
            "Chinook salmon",
            listOf(
                Regex("""\bchinook(?:\s+salmon)?\b""", RegexOption.IGNORE_CASE),
                Regex("""\bkings?\b""", RegexOption.IGNORE_CASE),
            ),
        ),
        Species(
            "coho salmon",
            listOf(Regex("""\bcohos?(?:\s+salmon)?\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "Atlantic salmon",
            listOf(Regex("""\batlantic\s+salmon\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "steelhead",
            listOf(
                Regex("""\bsteelhead\b""", RegexOption.IGNORE_CASE),
                Regex("""\brainbow\s+trout\b""", RegexOption.IGNORE_CASE),
            ),
        ),
        Species(
            "lake trout",
            listOf(Regex("""\blake\s+trout\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "brown trout",
            listOf(Regex("""\bbrown\s+trout\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "splake",
            listOf(Regex("""\bsplake\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "walleye",
            listOf(Regex("""\bwalleyes?\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "yellow perch",
            listOf(
                Regex("""\byellow\s+perch\b""", RegexOption.IGNORE_CASE),
                Regex("""\bperch\b""", RegexOption.IGNORE_CASE),
            ),
        ),
        Species(
            "smallmouth bass",
            listOf(Regex("""\bsmallmouth(?:\s+bass)?\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "largemouth bass",
            listOf(Regex("""\blargemouth(?:\s+bass)?\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "northern pike",
            listOf(
                Regex("""\bnorthern\s+pike\b""", RegexOption.IGNORE_CASE),
                Regex("""\bpike\b""", RegexOption.IGNORE_CASE),
            ),
        ),
        Species(
            "muskie",
            listOf(Regex("""\bmusk(?:y|ie)s?\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "catfish",
            listOf(Regex("""\bcatfish\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "freshwater drum",
            listOf(
                Regex("""\bfreshwater\s+drum\b""", RegexOption.IGNORE_CASE),
                Regex("""\bsheepshead\b""", RegexOption.IGNORE_CASE),
            ),
        ),
        Species(
            "whitefish",
            listOf(Regex("""\bwhitefish\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "bass",
            listOf(Regex("""\bbass\b""", RegexOption.IGNORE_CASE)),
        ),
        Species(
            "salmon",
            listOf(Regex("""\bsalmon\b""", RegexOption.IGNORE_CASE)),
        ),
    )

    private val limitPatterns = listOf(
        Regex("""\blimit(?:s|ed)?\s+catche?s?\b""", RegexOption.IGNORE_CASE),
        Regex("""\blimit(?:s)?\s+(?:were|was|of)\b""", RegexOption.IGNORE_CASE),
        Regex("""\breaching\s+their\s+limits\b""", RegexOption.IGNORE_CASE),
        Regex("""\blimit\s+catches\b""", RegexOption.IGNORE_CASE),
    )

    private val hotPatterns = listOf(
        Regex("""\bexcellent\b""", RegexOption.IGNORE_CASE),
        Regex("""\bvery\s+good\b""", RegexOption.IGNORE_CASE),
        Regex("""\bhot\s+bite\b""", RegexOption.IGNORE_CASE),
        Regex("""\bincreased\s+catches\b""", RegexOption.IGNORE_CASE),
    )

    private val fairPatterns = listOf(
        Regex("""\bfair\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdecent\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmoderate\b""", RegexOption.IGNORE_CASE),
        Regex("""\bgood\s+(?:numbers|success|catches|action|bite)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bhad\s+success\b""", RegexOption.IGNORE_CASE),
    )

    private val slowPatterns = listOf(
        Regex("""\bslow\b""", RegexOption.IGNORE_CASE),
        Regex("""\blimited\s+(?:success|fishing|activity|pressure)\b""", RegexOption.IGNORE_CASE),
        Regex("""\blittle\s+to\s+no\b""", RegexOption.IGNORE_CASE),
        Regex("""\bunsuccessful\b""", RegexOption.IGNORE_CASE),
        Regex("""\bno\s+success\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnot\s+much\b""", RegexOption.IGNORE_CASE),
        Regex("""\btaper(?:ed|ing)\s+off\b""", RegexOption.IGNORE_CASE),
    )

    fun from(body: String): String {
        val text = body.trim()
        if (text.isEmpty()) return "No report details"

        val species = findSpecies(text)
        val pulse = findPulse(text)

        return when {
            species.isNotEmpty() && pulse != null ->
                "${species.joinToString(", ")} · $pulse"
            species.isNotEmpty() ->
                species.joinToString(", ")
            pulse != null ->
                "Bite $pulse"
            else ->
                fallbackSnippet(text)
        }
    }

    private fun findSpecies(text: String): List<String> {
        val found = linkedSetOf<String>()
        val hasSpecificSalmon = Regex(
            """\b(?:chinook|coho|atlantic)\b""",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(text)
        val hasNamedBass = Regex(
            """\b(?:smallmouth|largemouth)\b""",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(text)

        for (species in speciesCatalog) {
            if (species.label == "salmon" && hasSpecificSalmon) continue
            if (species.label == "bass" && hasNamedBass) continue
            if (species.patterns.any { it.containsMatchIn(text) }) {
                found += species.label
            }
            if (found.size >= 3) break
        }
        return found.take(3).toList()
    }

    private fun findPulse(text: String): String? {
        val hasLimits = limitPatterns.any { it.containsMatchIn(text) }
        val hot = hotPatterns.any { it.containsMatchIn(text) }
        val fair = fairPatterns.any { it.containsMatchIn(text) }
        val slow = slowPatterns.any { it.containsMatchIn(text) }

        return when {
            hasLimits && !slow -> "limits"
            hot && slow -> "mixed"
            hot -> "hot"
            slow && fair -> "mixed"
            slow -> "slow"
            fair -> "fair"
            else -> null
        }
    }

    private fun fallbackSnippet(text: String): String {
        val cleaned = text.replace(Regex("""\s+"""), " ").trim()
        if (cleaned.length <= 48) return cleaned
        val cut = cleaned.take(47)
        val lastSpace = cut.lastIndexOf(' ')
        return if (lastSpace > 24) cut.take(lastSpace) + "…" else "$cut…"
    }
}

fun FishingLocationReport.biteSummary(): String = FishingBiteSummary.from(body)
