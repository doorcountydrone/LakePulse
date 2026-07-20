package com.lakepulse.data.favorites

import android.content.Context
import com.lakepulse.data.model.FishingLocationReport
import com.lakepulse.data.model.FishingSource

/**
 * Local favorites for buoys (NDBC station id) and fishing spots
 * (stable key so favorites survive week-to-week report changes).
 */
class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun favoriteBuoyIds(): Set<String> =
        prefs.getStringSet(KEY_BUOYS, emptySet())?.toSet().orEmpty()

    fun favoriteFishingKeys(): Set<String> =
        prefs.getStringSet(KEY_FISHING, emptySet())?.toSet().orEmpty()

    fun isFavoriteBuoy(stationId: String): Boolean =
        stationId in favoriteBuoyIds()

    fun isFavoriteFishing(location: FishingLocationReport): Boolean =
        fishingKey(location) in favoriteFishingKeys()

    fun toggleBuoy(stationId: String): Set<String> {
        val next = favoriteBuoyIds().toMutableSet()
        if (!next.add(stationId)) next.remove(stationId)
        prefs.edit().putStringSet(KEY_BUOYS, next).apply()
        return next
    }

    fun toggleFishing(location: FishingLocationReport): Set<String> {
        val key = fishingKey(location)
        val next = favoriteFishingKeys().toMutableSet()
        if (!next.add(key)) next.remove(key)
        prefs.edit().putStringSet(KEY_FISHING, next).apply()
        return next
    }

    companion object {
        private const val PREFS = "lakepulse_favorites"
        private const val KEY_BUOYS = "favorite_buoys"
        private const val KEY_FISHING = "favorite_fishing"

        fun fishingKey(location: FishingLocationReport): String =
            fishingKey(location.source, location.region.name, location.name)

        fun fishingKey(source: FishingSource, regionName: String, name: String): String =
            listOf(source.name, regionName, name.trim().lowercase())
                .joinToString("|")
    }
}
