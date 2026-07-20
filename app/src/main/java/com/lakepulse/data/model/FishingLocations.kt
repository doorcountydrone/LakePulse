package com.lakepulse.data.model

/**
 * Approximate coordinates for DNR fishing report locations (MI + WI).
 * Used for closest-first sorting when GPS is available.
 */
object FishingLocations {
    data class Place(
        val latitude: Double,
        val longitude: Double,
        val aliases: Set<String> = emptySet(),
    )

    private val places: Map<String, Place> = listOf(
        "Lake Erie" to Place(41.85, -83.35),
        "Sebewaing" to Place(43.73, -83.45),
        "Lower Saginaw Bay" to Place(43.75, -83.75),
        "Bayport" to Place(43.85, -83.57),
        "Quanicassee" to Place(43.58, -83.69),
        "Saginaw River" to Place(43.58, -83.91),
        "Eastern Saginaw Bay" to Place(43.90, -83.50),
        "Tittabawassee River" to Place(43.58, -84.10),
        "Muskegon" to Place(43.23, -86.34),
        "Grand Haven" to Place(43.06, -86.23),
        "Holland" to Place(42.79, -86.13),
        "St. Joseph" to Place(42.11, -86.49, setOf("Saint Joseph", "St Joseph")),
        "South Haven" to Place(42.40, -86.27),
        "New Buffalo" to Place(41.79, -86.74),
        "Tawas/Au Gres" to Place(44.27, -83.58, setOf("Tawas", "Au Gres", "Tawas/AuGres")),
        "Alpena" to Place(45.06, -83.43),
        "Thunder Bay River" to Place(45.07, -83.44),
        "Rockport" to Place(45.20, -83.38),
        "Cheboygan" to Place(45.65, -84.48),
        "Oscoda/Au Sable" to Place(44.42, -83.33, setOf("Oscoda", "Au Sable")),
        "Harrisville" to Place(44.66, -83.29),
        "Rogers City" to Place(45.42, -83.82),
        "Presque Isle" to Place(45.34, -83.48),
        "Manistee" to Place(44.25, -86.32),
        "Ludington" to Place(43.96, -86.45),
        "Onekama/Lake Michigan" to Place(44.36, -86.25, setOf("Onekama")),
        "Charlevoix" to Place(45.32, -85.26),
        "Little Traverse Bay" to Place(45.40, -85.00),
        "West Grand Traverse Bay" to Place(44.80, -85.63),
        "East Grand Traverse Bay" to Place(44.85, -85.48),
        "Grand Traverse Bay" to Place(44.83, -85.55),
        "Leland" to Place(45.02, -85.76),
        "Frankfort/Lake Michigan" to Place(44.63, -86.23, setOf("Frankfort")),
        "Northport/Suttons Bay" to Place(45.13, -85.62, setOf("Northport", "Suttons Bay")),
        "Petoskey" to Place(45.37, -84.96),
        "Harbor Springs" to Place(45.43, -84.99),
        "Traverse City" to Place(44.76, -85.62),
        "Little Bay de Noc" to Place(45.75, -87.08, setOf("Bay de Noc")),
        "Munising" to Place(46.41, -86.65),
        "Keweenaw Bay/Huron Bay" to Place(46.85, -88.40, setOf("Keweenaw Bay", "Huron Bay")),
        "Traverse Bay/Portage Entry" to Place(47.07, -88.50, setOf("Portage Entry", "Traverse Bay")),
        "Ontonagon River" to Place(46.87, -89.32),
        "Ontonagon/Silver City/Union Bay" to Place(
            46.87,
            -89.32,
            setOf("Ontonagon", "Silver City", "Union Bay"),
        ),
        "Black River Harbor" to Place(46.67, -90.05),
        "St. Ignace" to Place(45.87, -84.73, setOf("Saint Ignace", "St Ignace")),
        "Grand Marais" to Place(46.67, -85.98),
        "Les Cheneaux Islands/DeTour" to Place(
            45.99,
            -84.30,
            setOf("Les Cheneaux", "Les Cheneaux Islands", "DeTour", "Detour", "Hessel"),
        ),
        "Manistique" to Place(45.96, -86.25),
        "Marquette" to Place(46.54, -87.40),
        "Au Train" to Place(46.43, -86.84),
        "Lake Independence, Big Bay" to Place(
            46.82,
            -87.73,
            setOf("Lake Independence", "Big Bay", "Lake Independence/Big Bay"),
        ),
        "Sault Ste. Marie" to Place(46.50, -84.35, setOf("Sault Sainte Marie")),
        "Escanaba" to Place(45.75, -87.06),
        // Wisconsin — Lake Michigan / Green Bay
        "Brown County" to Place(44.50, -88.00),
        "Marinette County" to Place(45.20, -87.70),
        "Oconto County" to Place(44.89, -87.90),
        "Door County" to Place(45.05, -87.12),
        "Kewaunee County" to Place(44.50, -87.50),
        "Manitowoc County" to Place(44.10, -87.66),
        "Kenosha County" to Place(42.58, -87.85),
        "Milwaukee County" to Place(43.04, -87.90),
        "Ozaukee County" to Place(43.38, -87.90),
        "Racine County" to Place(42.73, -87.80),
        "Sheboygan County" to Place(43.75, -87.75),
        "Fox River" to Place(44.53, -88.01),
        "Suamico River" to Place(44.63, -88.04),
        "Geano Beach" to Place(44.95, -87.78),
        "Bayshore Park" to Place(44.63, -87.77),
        "Eagle's Nest" to Place(44.58, -87.85, setOf("Eagles Nest")),
        "Chaudoir's Dock" to Place(44.68, -87.72, setOf("Chaudoirs Dock")),
        "Little Sturgeon Bay" to Place(44.78, -87.58),
        "Sawyer Harbor" to Place(44.85, -87.42),
        "Sturgeon Bay" to Place(44.83, -87.38),
        "Algoma" to Place(44.61, -87.43),
        "Kewaunee" to Place(44.46, -87.50),
        "Two Rivers" to Place(44.15, -87.57),
        "Manitowoc" to Place(44.09, -87.66),
        "Sheboygan" to Place(43.75, -87.71),
        "Port Washington" to Place(43.39, -87.87),
        "Milwaukee" to Place(43.04, -87.91),
        "Racine" to Place(42.73, -87.78),
        "Kenosha" to Place(42.58, -87.82),
        "Green Bay" to Place(44.52, -88.00),
        "Marinette" to Place(45.10, -87.63),
        "Oconto" to Place(44.89, -87.86),
        // Wisconsin — Lake Superior
        "Apostle Islands" to Place(46.95, -90.65),
        "Chequamegon Bay" to Place(46.65, -90.85),
        "Cornucopia/Port Wing" to Place(46.85, -91.20, setOf("Cornucopia", "Port Wing")),
        "Saxon" to Place(46.55, -90.45),
        "Superior" to Place(46.72, -92.10),
        "Ashland" to Place(46.59, -90.88),
        "Bayfield" to Place(46.81, -90.81),
    ).associate { (name, place) -> name.lowercase() to place }

    private val regionCenters = mapOf(
        FishingRegion.SOUTHEAST_LOWER to Place(42.60, -83.40),
        FishingRegion.SOUTHWEST_LOWER to Place(42.70, -86.20),
        FishingRegion.NORTHEAST_LOWER to Place(44.80, -83.50),
        FishingRegion.NORTHWEST_LOWER to Place(44.70, -86.00),
        FishingRegion.UPPER_PENINSULA to Place(46.50, -87.50),
        FishingRegion.WEST_SHORE_GREEN_BAY to Place(44.80, -87.90),
        FishingRegion.EAST_SHORE_GREEN_BAY to Place(44.70, -87.70),
        FishingRegion.NORTHERN_LAKE_MICHIGAN to Place(44.50, -87.40),
        FishingRegion.SOUTHERN_LAKE_MICHIGAN to Place(43.20, -87.80),
        FishingRegion.LAKE_SUPERIOR to Place(46.75, -90.90),
        FishingRegion.UNKNOWN to Place(44.00, -85.00),
    )

    fun resolve(name: String, region: FishingRegion): Place {
        val key = normalize(name)
        places[key]?.let { return it }

        for ((canonical, place) in places) {
            if (canonical == key) return place
            if (place.aliases.any { normalize(it) == key }) return place
            if (key.contains(canonical) || canonical.contains(key)) return place
            if (place.aliases.any { alias ->
                    val a = normalize(alias)
                    key.contains(a) || a.contains(key)
                }
            ) {
                return place
            }
        }

        return regionCenters.getValue(region)
    }

    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .removeSuffix(":")
}
