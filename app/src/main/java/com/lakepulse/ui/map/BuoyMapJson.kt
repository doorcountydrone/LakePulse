package com.lakepulse.ui.map

import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.model.RelativeTime
import com.lakepulse.data.model.windDirectionLabel
import org.json.JSONArray
import org.json.JSONObject

object BuoyMapJson {
    fun toJson(buoys: List<BuoyObservation>): String {
        val array = JSONArray()
        buoys.forEach { buoy ->
            array.put(
                JSONObject().apply {
                    put("id", buoy.stationId)
                    put("lat", buoy.latitude)
                    put("lon", buoy.longitude)
                    put("tempF", buoy.waterTempF)
                    put("windMph", buoy.windSpeedMph ?: JSONObject.NULL)
                    put("windGustMph", buoy.windGustMph ?: JSONObject.NULL)
                    put("windDirLabel", windDirectionLabel(buoy.windDirectionDeg))
                    put("waveHeightFt", buoy.waveHeightFt ?: JSONObject.NULL)
                    put(
                        "observedAt",
                        RelativeTime.observationAgeLabel(
                            buoy.observedAt,
                            unknown = "Unknown time",
                        ),
                    )
                },
            )
        }
        return array.toString()
    }
}
