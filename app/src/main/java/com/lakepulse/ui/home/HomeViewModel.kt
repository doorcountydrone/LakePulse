package com.lakepulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lakepulse.data.favorites.FavoritesStore
import com.lakepulse.data.location.DeviceLocationClient
import com.lakepulse.data.location.UserLocation
import com.lakepulse.data.model.BuoySort
import com.lakepulse.data.model.BuoyTempTrend
import com.lakepulse.data.model.GreatLake
import com.lakepulse.data.model.LakeBoard
import com.lakepulse.data.remote.NetworkModule
import com.lakepulse.data.repository.ConditionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BuoyTrendUi(
    val trend: BuoyTempTrend? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class HomeUiState(
    val selectedLake: GreatLake = GreatLake.MICHIGAN,
    val sort: BuoySort = BuoySort.NEAREST,
    val board: LakeBoard? = null,
    val favoriteBuoyIds: Set<String> = emptySet(),
    val expandedTrendStationId: String? = null,
    val trendsByStationId: Map<String, BuoyTrendUi> = emptyMap(),
    val userLocation: UserLocation? = null,
    val locationStatus: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasAutoSelectedLake: Boolean = false,
)

class HomeViewModel(
    private val repository: ConditionsRepository = ConditionsRepository(),
    private val locationClient: DeviceLocationClient,
    private val favoritesStore: FavoritesStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoading = true,
            favoriteBuoyIds = favoritesStore.favoriteBuoyIds(),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh(force = false)
    }

    fun toggleFavoriteBuoy(stationId: String) {
        val next = favoritesStore.toggleBuoy(stationId)
        _uiState.update { state ->
            state.copy(
                favoriteBuoyIds = next,
                board = state.board?.withFavoritesFirst(next),
            )
        }
    }

    fun toggleTrendExpanded(stationId: String) {
        val current = _uiState.value.expandedTrendStationId
        if (current == stationId) {
            _uiState.update { it.copy(expandedTrendStationId = null) }
            return
        }
        _uiState.update { it.copy(expandedTrendStationId = stationId) }
        ensureTrendLoaded(stationId)
    }

    fun retryTrend(stationId: String) {
        ensureTrendLoaded(stationId, force = true)
    }

    fun selectLake(lake: GreatLake) {
        if (lake == _uiState.value.selectedLake && _uiState.value.board != null) return
        _uiState.update { it.copy(selectedLake = lake, hasAutoSelectedLake = true) }
        refresh(force = false)
    }

    fun selectSort(sort: BuoySort) {
        val current = _uiState.value
        if (sort == current.sort) return
        _uiState.update { it.copy(sort = sort) }
        refresh(force = false)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.update {
                it.copy(locationStatus = "Location off · sorting without distance")
            }
            refresh(force = false)
            return
        }
        resolveLocationAndRefresh()
    }

    fun ensureLocation() {
        if (!locationClient.hasLocationPermission()) return
        if (_uiState.value.userLocation != null) return
        resolveLocationAndRefresh()
    }

    fun refresh(force: Boolean = true) {
        val lake = _uiState.value.selectedLake
        val sort = _uiState.value.sort
        val location = _uiState.value.userLocation
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (force && locationClient.hasLocationPermission()) {
                runCatching { locationClient.getLocation() }.getOrNull()?.let { fresh ->
                    _uiState.update { state ->
                        state.copy(
                            userLocation = fresh,
                            locationStatus = null,
                            selectedLake = if (!state.hasAutoSelectedLake) {
                                GreatLake.nearestTo(fresh.latitude, fresh.longitude)
                            } else {
                                state.selectedLake
                            },
                            hasAutoSelectedLake = true,
                        )
                    }
                }
            }
            val state = _uiState.value
            runCatching {
                repository.getLakeBoard(
                    lake = state.selectedLake,
                    sort = state.sort,
                    userLocation = state.userLocation,
                    forceRefresh = force,
                )
            }.onSuccess { board ->
                val favorites = _uiState.value.favoriteBuoyIds
                _uiState.update {
                    it.copy(
                        board = board.withFavoritesFirst(favorites),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Something went wrong",
                    )
                }
            }
        }
    }

    private fun resolveLocationAndRefresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, locationStatus = "Finding your location…")
            }
            val location = runCatching { locationClient.getLocation() }.getOrNull()
            if (location == null) {
                _uiState.update {
                    it.copy(locationStatus = "Couldn't get location · showing lake order")
                }
                refresh(force = false)
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    userLocation = location,
                    locationStatus = null,
                    selectedLake = if (!state.hasAutoSelectedLake) {
                        GreatLake.nearestTo(location.latitude, location.longitude)
                    } else {
                        state.selectedLake
                    },
                    hasAutoSelectedLake = true,
                    sort = BuoySort.NEAREST,
                )
            }
            refresh(force = false)
        }
    }

    private fun ensureTrendLoaded(stationId: String, force: Boolean = false) {
        val existing = _uiState.value.trendsByStationId[stationId]
        if (!force && (existing?.trend != null || existing?.isLoading == true)) return

        _uiState.update { state ->
            state.copy(
                trendsByStationId = state.trendsByStationId + (
                    stationId to BuoyTrendUi(isLoading = true)
                    ),
            )
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    NetworkModule.ndbcRealtimeHistoryClient.fetchTempTrend(
                        stationId = stationId,
                        maxAgeMs = if (force) 0L else 10 * 60 * 1000L,
                    )
                }
            }
            _uiState.update { state ->
                val ui = result.fold(
                    onSuccess = { trend ->
                        if (trend.samples.isEmpty()) {
                            BuoyTrendUi(errorMessage = "No water temp history for this buoy")
                        } else {
                            BuoyTrendUi(trend = trend)
                        }
                    },
                    onFailure = { error ->
                        val message = when {
                            error.message?.contains("404") == true ->
                                "No 24h history file for this station"
                            else -> error.message ?: "Couldn't load 24h trend"
                        }
                        BuoyTrendUi(errorMessage = message)
                    },
                )
                state.copy(
                    trendsByStationId = state.trendsByStationId + (stationId to ui),
                )
            }
        }
    }
}

private fun LakeBoard.withFavoritesFirst(favoriteIds: Set<String>): LakeBoard {
    if (favoriteIds.isEmpty()) return this
    val (favorites, rest) = buoys.partition { it.stationId in favoriteIds }
    return copy(buoys = favorites + rest)
}

class HomeViewModelFactory(
    private val locationClient: DeviceLocationClient,
    private val favoritesStore: FavoritesStore,
    private val repository: ConditionsRepository = ConditionsRepository(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository, locationClient, favoritesStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
