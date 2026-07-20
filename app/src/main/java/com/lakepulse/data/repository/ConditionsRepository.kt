package com.lakepulse.data.repository

import com.lakepulse.data.location.UserLocation
import com.lakepulse.data.model.BuoySort
import com.lakepulse.data.model.GreatLake
import com.lakepulse.data.model.LakeBoard
import com.lakepulse.data.model.toLakeBoard
import com.lakepulse.data.remote.NdbcLatestObsClient
import com.lakepulse.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConditionsRepository(
    private val latestObsClient: NdbcLatestObsClient = NetworkModule.ndbcLatestObsClient,
) {
    suspend fun getLakeBoard(
        lake: GreatLake,
        sort: BuoySort,
        userLocation: UserLocation? = null,
        forceRefresh: Boolean = false,
    ): LakeBoard = withContext(Dispatchers.IO) {
        val buoys = latestObsClient.fetchGreatLakes(
            maxAgeMs = if (forceRefresh) 0L else 3 * 60 * 1000L,
        )
        buoys.toLakeBoard(lake, sort, userLocation).also { board ->
            if (board.buoys.isEmpty()) {
                error("No live buoy readings for ${lake.displayName} right now")
            }
        }
    }
}
