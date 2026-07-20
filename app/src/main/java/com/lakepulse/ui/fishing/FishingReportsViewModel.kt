package com.lakepulse.ui.fishing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lakepulse.data.favorites.FavoritesStore
import com.lakepulse.data.location.DeviceLocationClient
import com.lakepulse.data.location.UserLocation
import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.model.FishingLocationReport
import com.lakepulse.data.model.FishingReportWeek
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource
import com.lakepulse.data.model.NearestBuoyConditions
import com.lakepulse.data.model.findNearestBuoy
import com.lakepulse.data.remote.NetworkModule
import com.lakepulse.data.repository.FishingReportsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FishingReportsUiState(
    val source: FishingSource = FishingSource.WISCONSIN,
    val weeks: List<FishingReportWeekSummary> = emptyList(),
    val selectedWeekId: String? = null,
    val week: FishingReportWeek? = null,
    val selectedLocation: FishingLocationReport? = null,
    val nearestBuoyByLocationId: Map<String, NearestBuoyConditions> = emptyMap(),
    val favoriteFishingKeys: Set<String> = emptySet(),
    val userLocation: UserLocation? = null,
    val locationStatus: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class FishingReportsViewModel(
    private val repository: FishingReportsRepository = FishingReportsRepository(),
    private val locationClient: DeviceLocationClient,
    private val favoritesStore: FavoritesStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FishingReportsUiState(
            isLoading = true,
            favoriteFishingKeys = favoritesStore.favoriteFishingKeys(),
        ),
    )
    val uiState: StateFlow<FishingReportsUiState> = _uiState.asStateFlow()

    private var cachedBuoys: List<BuoyObservation> = emptyList()

    init {
        refresh(force = false)
    }

    fun toggleFavoriteFishing(location: FishingLocationReport) {
        val next = favoritesStore.toggleFishing(location)
        _uiState.update { state ->
            state.copy(
                favoriteFishingKeys = next,
                week = state.week?.withFavoritesFirst(next),
            )
        }
    }

    fun selectSource(source: FishingSource) {
        if (source == _uiState.value.source && _uiState.value.week != null) return
        _uiState.update {
            it.copy(
                source = source,
                selectedWeekId = null,
                week = null,
                selectedLocation = null,
                nearestBuoyByLocationId = emptyMap(),
                weeks = emptyList(),
                errorMessage = null,
            )
        }
        refresh(force = false)
    }

    fun selectWeek(weekId: String) {
        if (weekId == _uiState.value.selectedWeekId && _uiState.value.week != null) return
        val summary = _uiState.value.weeks.firstOrNull { it.id == weekId } ?: return
        _uiState.update {
            it.copy(selectedWeekId = weekId, selectedLocation = null)
        }
        loadWeek(summary, force = false)
    }

    fun selectLocation(location: FishingLocationReport) {
        _uiState.update { it.copy(selectedLocation = location) }
    }

    fun clearSelectedLocation() {
        _uiState.update { it.copy(selectedLocation = null) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.update {
                it.copy(locationStatus = "Location off · alphabetical order")
            }
            reloadSelectedWeek()
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (force && locationClient.hasLocationPermission()) {
                runCatching { locationClient.getLocation() }.getOrNull()?.let { location ->
                    _uiState.update {
                        it.copy(userLocation = location, locationStatus = null)
                    }
                }
            }
            loadBuoys(force = force)
            val source = _uiState.value.source
            runCatching { repository.getRecentWeeks(source, forceRefresh = force) }
                .onSuccess { weeks ->
                    val selectedId = _uiState.value.selectedWeekId
                        ?.takeIf { id -> weeks.any { it.id == id } }
                        ?: weeks.firstOrNull()?.id
                    val summary = weeks.firstOrNull { it.id == selectedId }
                        ?: weeks.firstOrNull()
                    _uiState.update {
                        it.copy(
                            weeks = weeks,
                            selectedWeekId = summary?.id,
                            isLoading = summary == null,
                            errorMessage = if (summary == null) {
                                "No fishing reports available"
                            } else {
                                null
                            },
                        )
                    }
                    if (summary != null) {
                        loadWeek(summary, force = force)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Couldn't load fishing reports",
                        )
                    }
                }
        }
    }

    private fun reloadSelectedWeek() {
        val summary = _uiState.value.weeks.firstOrNull {
            it.id == _uiState.value.selectedWeekId
        } ?: return
        loadWeek(summary, force = false)
    }

    private fun loadWeek(summary: FishingReportWeekSummary, force: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getWeek(
                    summary = summary,
                    userLocation = _uiState.value.userLocation,
                    forceRefresh = force,
                )
            }.onSuccess { week ->
                val sortedWeek = if (_uiState.value.userLocation == null) {
                    week.copy(
                        locations = week.locations.sortedBy { it.name.lowercase() },
                    )
                } else {
                    week
                }
                if (cachedBuoys.isEmpty()) {
                    loadBuoys(force = false)
                }
                val favorites = _uiState.value.favoriteFishingKeys
                val orderedWeek = sortedWeek.withFavoritesFirst(favorites)
                val nearest = nearestBuoyMap(orderedWeek)
                _uiState.update {
                    it.copy(
                        week = orderedWeek,
                        nearestBuoyByLocationId = nearest,
                        isLoading = false,
                        errorMessage = null,
                        selectedLocation = it.selectedLocation?.let { selected ->
                            orderedWeek.locations.firstOrNull { location ->
                                location.id == selected.id
                            }
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Couldn't load this report",
                    )
                }
            }
        }
    }

    private suspend fun loadBuoys(force: Boolean) {
        if (!force && cachedBuoys.isNotEmpty()) return
        val buoys = withContext(Dispatchers.IO) {
            runCatching {
                NetworkModule.ndbcLatestObsClient.fetchGreatLakes(
                    maxAgeMs = if (force) 0L else 3 * 60 * 1000L,
                )
            }.getOrDefault(emptyList())
        }
        if (buoys.isNotEmpty()) {
            cachedBuoys = buoys
            _uiState.value.week?.let { week ->
                _uiState.update {
                    it.copy(nearestBuoyByLocationId = nearestBuoyMap(week))
                }
            }
        }
    }

    private fun nearestBuoyMap(week: FishingReportWeek): Map<String, NearestBuoyConditions> {
        if (cachedBuoys.isEmpty()) return emptyMap()
        return week.locations.mapNotNull { location ->
            findNearestBuoy(
                latitude = location.latitude,
                longitude = location.longitude,
                buoys = cachedBuoys,
            )?.let { location.id to it }
        }.toMap()
    }

    private fun resolveLocationAndRefresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, locationStatus = "Finding your location…")
            }
            val location = runCatching { locationClient.getLocation() }.getOrNull()
            if (location == null) {
                _uiState.update {
                    it.copy(locationStatus = "Couldn't get location · alphabetical order")
                }
                reloadSelectedWeek()
                return@launch
            }
            _uiState.update {
                it.copy(userLocation = location, locationStatus = null)
            }
            reloadSelectedWeek()
        }
    }
}

private fun FishingReportWeek.withFavoritesFirst(favoriteKeys: Set<String>): FishingReportWeek {
    if (favoriteKeys.isEmpty()) return this
    val (favorites, rest) = locations.partition {
        FavoritesStore.fishingKey(it) in favoriteKeys
    }
    return copy(locations = favorites + rest)
}

class FishingReportsViewModelFactory(
    private val locationClient: DeviceLocationClient,
    private val favoritesStore: FavoritesStore,
    private val repository: FishingReportsRepository = FishingReportsRepository(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FishingReportsViewModel::class.java)) {
            return FishingReportsViewModel(repository, locationClient, favoritesStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
