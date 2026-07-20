package com.lakepulse.data.repository

import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.remote.NdbcLatestObsClient
import com.lakepulse.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TemperatureMapRepository(
    private val latestObsClient: NdbcLatestObsClient = NetworkModule.ndbcLatestObsClient,
) {
    suspend fun getBuoyTemperatures(): List<BuoyObservation> = withContext(Dispatchers.IO) {
        latestObsClient.fetchGreatLakes()
            .sortedBy { it.stationId }
            .also { buoys ->
                if (buoys.isEmpty()) {
                    error("No Great Lakes buoy temperatures available right now")
                }
            }
    }
}
