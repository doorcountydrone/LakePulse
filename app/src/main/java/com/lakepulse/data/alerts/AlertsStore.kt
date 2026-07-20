package com.lakepulse.data.alerts

import android.content.Context
import com.lakepulse.data.model.FishingSource

/**
 * Local alert preferences (SharedPreferences, same pattern as FavoritesStore).
 */
class AlertsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun snapshot(): AlertSettings = AlertSettings(
        masterEnabled = prefs.getBoolean(KEY_MASTER, false),
        tempBandEnabled = prefs.getBoolean(KEY_TEMP_ENABLED, true),
        tempMinF = prefs.getFloat(KEY_TEMP_MIN, DEFAULT_TEMP_MIN),
        tempMaxF = prefs.getFloat(KEY_TEMP_MAX, DEFAULT_TEMP_MAX),
        calmWindEnabled = prefs.getBoolean(KEY_WIND_ENABLED, true),
        calmWindMaxMph = prefs.getFloat(KEY_WIND_MAX, DEFAULT_WIND_MAX),
        watchedStationId = prefs.getString(KEY_STATION, null),
        fishingMiEnabled = prefs.getBoolean(KEY_FISH_MI, true),
        fishingWiEnabled = prefs.getBoolean(KEY_FISH_WI, true),
        lastSeenMiWeekId = prefs.getString(KEY_LAST_MI_WEEK, null),
        lastSeenWiWeekId = prefs.getString(KEY_LAST_WI_WEEK, null),
        lastTempFiredKey = prefs.getString(KEY_LAST_TEMP_FIRE, null),
        lastWindFiredKey = prefs.getString(KEY_LAST_WIND_FIRE, null),
    )

    fun save(settings: AlertSettings) {
        prefs.edit()
            .putBoolean(KEY_MASTER, settings.masterEnabled)
            .putBoolean(KEY_TEMP_ENABLED, settings.tempBandEnabled)
            .putFloat(KEY_TEMP_MIN, settings.tempMinF)
            .putFloat(KEY_TEMP_MAX, settings.tempMaxF)
            .putBoolean(KEY_WIND_ENABLED, settings.calmWindEnabled)
            .putFloat(KEY_WIND_MAX, settings.calmWindMaxMph)
            .putString(KEY_STATION, settings.watchedStationId)
            .putBoolean(KEY_FISH_MI, settings.fishingMiEnabled)
            .putBoolean(KEY_FISH_WI, settings.fishingWiEnabled)
            .apply()
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER, enabled).apply()
    }

    fun setWatchedStationId(stationId: String?) {
        prefs.edit().putString(KEY_STATION, stationId).apply()
    }

    fun setLastSeenWeek(source: FishingSource, weekId: String) {
        val key = when (source) {
            FishingSource.MICHIGAN -> KEY_LAST_MI_WEEK
            FishingSource.WISCONSIN -> KEY_LAST_WI_WEEK
        }
        prefs.edit().putString(key, weekId).apply()
    }

    fun setLastTempFiredKey(key: String) {
        prefs.edit().putString(KEY_LAST_TEMP_FIRE, key).apply()
    }

    fun setLastWindFiredKey(key: String) {
        prefs.edit().putString(KEY_LAST_WIND_FIRE, key).apply()
    }

    companion object {
        private const val PREFS = "lakepulse_alerts"
        private const val KEY_MASTER = "master_enabled"
        private const val KEY_TEMP_ENABLED = "temp_enabled"
        private const val KEY_TEMP_MIN = "temp_min_f"
        private const val KEY_TEMP_MAX = "temp_max_f"
        private const val KEY_WIND_ENABLED = "wind_enabled"
        private const val KEY_WIND_MAX = "wind_max_mph"
        private const val KEY_STATION = "watched_station"
        private const val KEY_FISH_MI = "fish_mi"
        private const val KEY_FISH_WI = "fish_wi"
        private const val KEY_LAST_MI_WEEK = "last_mi_week"
        private const val KEY_LAST_WI_WEEK = "last_wi_week"
        private const val KEY_LAST_TEMP_FIRE = "last_temp_fire"
        private const val KEY_LAST_WIND_FIRE = "last_wind_fire"

        const val DEFAULT_TEMP_MIN = 55f
        const val DEFAULT_TEMP_MAX = 68f
        const val DEFAULT_WIND_MAX = 10f
    }
}

data class AlertSettings(
    val masterEnabled: Boolean = false,
    val tempBandEnabled: Boolean = true,
    val tempMinF: Float = AlertsStore.DEFAULT_TEMP_MIN,
    val tempMaxF: Float = AlertsStore.DEFAULT_TEMP_MAX,
    val calmWindEnabled: Boolean = true,
    val calmWindMaxMph: Float = AlertsStore.DEFAULT_WIND_MAX,
    val watchedStationId: String? = null,
    val fishingMiEnabled: Boolean = true,
    val fishingWiEnabled: Boolean = true,
    val lastSeenMiWeekId: String? = null,
    val lastSeenWiWeekId: String? = null,
    val lastTempFiredKey: String? = null,
    val lastWindFiredKey: String? = null,
)

enum class AlertKind {
    TEMP_BAND,
    CALM_WIND,
    NEW_DNR_WEEK,
}

data class AlertEvent(
    val kind: AlertKind,
    val title: String,
    val body: String,
    val notificationId: Int,
)
