package com.lakepulse.data.remote

object NetworkModule {
    val ndbcLatestObsClient: NdbcLatestObsClient by lazy { NdbcLatestObsClient() }
    val ndbcRealtimeHistoryClient: NdbcRealtimeHistoryClient by lazy {
        NdbcRealtimeHistoryClient()
    }
    val fishingReportsClient: MichiganDnrFishingReportsClient by lazy {
        MichiganDnrFishingReportsClient()
    }
    val wisconsinFishingReportsClient: WisconsinDnrFishingReportsClient by lazy {
        WisconsinDnrFishingReportsClient()
    }
}
