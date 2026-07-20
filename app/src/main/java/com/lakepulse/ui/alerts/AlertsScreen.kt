package com.lakepulse.ui.alerts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lakepulse.data.alerts.AlertSettings
import com.lakepulse.data.alerts.AlertWorkScheduler
import com.lakepulse.data.alerts.AlertsStore
import com.lakepulse.data.favorites.FavoritesStore
import com.lakepulse.data.model.BuoyNames
import com.lakepulse.ui.theme.SplashCyan
import com.lakepulse.ui.theme.SplashGold
import com.lakepulse.ui.theme.SplashScreenGradientColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class AlertsUiState(
    val settings: AlertSettings = AlertSettings(),
    val favoriteStations: List<String> = emptyList(),
    val statusMessage: String? = null,
)

class AlertsViewModel(
    private val alertsStore: AlertsStore,
    private val favoritesStore: FavoritesStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AlertsUiState(
            settings = alertsStore.snapshot(),
            favoriteStations = favoritesStore.favoriteBuoyIds().sorted(),
        ),
    )
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    fun refreshFavorites() {
        _uiState.update {
            it.copy(favoriteStations = favoritesStore.favoriteBuoyIds().sorted())
        }
    }

    fun update(transform: (AlertSettings) -> AlertSettings) {
        viewModelScope.launch {
            val next = transform(_uiState.value.settings)
            alertsStore.save(next)
            _uiState.update { it.copy(settings = alertsStore.snapshot(), statusMessage = null) }
        }
    }

    fun setStatus(message: String?) {
        _uiState.update { it.copy(statusMessage = message) }
    }
}

class AlertsViewModelFactory(
    private val alertsStore: AlertsStore,
    private val favoritesStore: FavoritesStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
            return AlertsViewModel(alertsStore, favoritesStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

@Composable
fun AlertsRoute(
    viewModel: AlertsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshFavorites() }
    AlertsScreen(
        state = state,
        onBack = onBack,
        onUpdate = viewModel::update,
        onStatus = viewModel::setStatus,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    state: AlertsUiState,
    onBack: () -> Unit,
    onUpdate: ((AlertSettings) -> AlertSettings) -> Unit,
    onStatus: (String?) -> Unit,
) {
    val context = LocalContext.current
    val settings = state.settings
    BackHandler(onBack = onBack)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            AlertWorkScheduler.schedule(context)
            AlertWorkScheduler.runNow(context)
            onStatus("Alerts on · checking in the background")
        } else {
            onUpdate { it.copy(masterEnabled = false) }
            onStatus("Notification permission needed for alerts")
        }
    }

    fun enableMaster(enabled: Boolean) {
        if (!enabled) {
            onUpdate { it.copy(masterEnabled = false) }
            onStatus("Alerts off")
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                onUpdate { it.copy(masterEnabled = true) }
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onUpdate { it.copy(masterEnabled = true) }
        AlertWorkScheduler.schedule(context)
        AlertWorkScheduler.runNow(context)
        onStatus("Alerts on · checking about every 2 hours")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🔔 Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(SplashScreenGradientColors))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AlertCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable alerts",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Checks every ~2 hours when online",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = settings.masterEnabled,
                        onCheckedChange = ::enableMaster,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SplashCyan,
                        ),
                    )
                }
                state.statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = SplashCyan,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            AlertCard {
                Text(
                    text = "Watch buoy",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (state.favoriteStations.isEmpty()) {
                        "Star a buoy on Conditions first — alerts use your favorites."
                    } else {
                        "Temp and wind alerts use this buoy (favorites shown)."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.favoriteStations.isEmpty()) {
                    Text(
                        text = settings.watchedStationId?.let { BuoyNames.displayName(it) }
                            ?: "No favorite yet",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.favoriteStations.take(8), key = { it }) { id ->
                            val selected = settings.watchedStationId == id ||
                                (settings.watchedStationId == null &&
                                    id == state.favoriteStations.first())
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onUpdate { it.copy(watchedStationId = id) }
                                },
                                label = {
                                    Text(
                                        text = BuoyNames.displayName(id),
                                        maxLines = 1,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    labelColor = Color.White,
                                    selectedContainerColor = SplashCyan.copy(alpha = 0.35f),
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                }
            }

            AlertCard {
                RuleHeader(
                    title = "🌡️ Water temp band",
                    checked = settings.tempBandEnabled,
                    enabled = settings.masterEnabled,
                    onCheckedChange = { on ->
                        onUpdate { it.copy(tempBandEnabled = on) }
                    },
                )
                Text(
                    text = "${settings.tempMinF.roundToInt()}–${settings.tempMaxF.roundToInt()}°F",
                    color = SplashGold,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Min °F",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelSmall,
                )
                Slider(
                    value = settings.tempMinF,
                    onValueChange = { v ->
                        onUpdate {
                            it.copy(tempMinF = v.coerceIn(32f, 85f))
                        }
                    },
                    valueRange = 32f..85f,
                    enabled = settings.masterEnabled && settings.tempBandEnabled,
                    colors = sliderColors(),
                )
                Text(
                    text = "Max °F",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelSmall,
                )
                Slider(
                    value = settings.tempMaxF,
                    onValueChange = { v ->
                        onUpdate {
                            it.copy(tempMaxF = v.coerceIn(32f, 85f))
                        }
                    },
                    valueRange = 32f..85f,
                    enabled = settings.masterEnabled && settings.tempBandEnabled,
                    colors = sliderColors(),
                )
            }

            AlertCard {
                RuleHeader(
                    title = "🌬️ Calm wind",
                    checked = settings.calmWindEnabled,
                    enabled = settings.masterEnabled,
                    onCheckedChange = { on ->
                        onUpdate { it.copy(calmWindEnabled = on) }
                    },
                )
                Text(
                    text = "Notify when wind ≤ ${settings.calmWindMaxMph.roundToInt()} mph",
                    color = SplashGold,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Slider(
                    value = settings.calmWindMaxMph,
                    onValueChange = { v ->
                        onUpdate { it.copy(calmWindMaxMph = v.coerceIn(3f, 25f)) }
                    },
                    valueRange = 3f..25f,
                    enabled = settings.masterEnabled && settings.calmWindEnabled,
                    colors = sliderColors(),
                )
            }

            AlertCard {
                Text(
                    text = "🎣 New DNR fishing week",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleHeader(
                    title = "Michigan",
                    checked = settings.fishingMiEnabled,
                    enabled = settings.masterEnabled,
                    onCheckedChange = { on ->
                        onUpdate { it.copy(fishingMiEnabled = on) }
                    },
                )
                RuleHeader(
                    title = "Wisconsin",
                    checked = settings.fishingWiEnabled,
                    enabled = settings.masterEnabled,
                    onCheckedChange = { on ->
                        onUpdate { it.copy(fishingWiEnabled = on) }
                    },
                )
            }

            TextButton(
                onClick = {
                    AlertWorkScheduler.runNow(context)
                    onStatus("Checking now…")
                },
                enabled = settings.masterEnabled,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Check now", color = SplashCyan)
            }

            Text(
                text = "Alerts are local on this device. Android may delay background checks to save battery.",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun AlertCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun RuleHeader(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SplashCyan,
            ),
        )
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = SplashCyan,
    activeTrackColor = SplashCyan,
    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
)
