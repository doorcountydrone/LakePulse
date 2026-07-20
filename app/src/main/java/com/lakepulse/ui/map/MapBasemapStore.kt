package com.lakepulse.ui.map

import android.content.Context

/** Which map screen’s basemap preference to read/write. */
enum class MapBasemapTab(val prefsKey: String) {
    Buoys("basemap_buoys"),
    Hybrid("basemap_hybrid"),
}

/**
 * Remembers Map vs Chart per tab (Buoys / Hybrid).
 */
class MapBasemapStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(tab: MapBasemapTab): MapBasemap {
        val raw = prefs.getString(tab.prefsKey, MapBasemap.Street.name) ?: MapBasemap.Street.name
        return MapBasemap.entries.firstOrNull { it.name == raw } ?: MapBasemap.Street
    }

    fun set(tab: MapBasemapTab, basemap: MapBasemap) {
        prefs.edit().putString(tab.prefsKey, basemap.name).apply()
    }

    companion object {
        private const val PREFS = "lakepulse_basemap"
    }
}
