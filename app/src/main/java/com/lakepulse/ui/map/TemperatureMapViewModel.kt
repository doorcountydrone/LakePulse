package com.lakepulse.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.repository.TemperatureMapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemperatureMapUiState(
    val buoys: List<BuoyObservation> = emptyList(),
    val focusStationId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class TemperatureMapViewModel(
    private val repository: TemperatureMapRepository = TemperatureMapRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemperatureMapUiState(isLoading = true))
    val uiState: StateFlow<TemperatureMapUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun focusBuoy(stationId: String) {
        _uiState.update { it.copy(focusStationId = stationId) }
    }

    fun clearFocusRequest() {
        _uiState.update { it.copy(focusStationId = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.getBuoyTemperatures() }
                .onSuccess { buoys ->
                    _uiState.update {
                        it.copy(buoys = buoys, isLoading = false, errorMessage = null)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load temperature map",
                        )
                    }
                }
        }
    }
}

class TemperatureMapViewModelFactory(
    private val repository: TemperatureMapRepository = TemperatureMapRepository(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemperatureMapViewModel::class.java)) {
            return TemperatureMapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
