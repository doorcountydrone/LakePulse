package com.lakepulse.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.lakepulse.R
import com.lakepulse.data.location.DeviceLocationClient
import com.lakepulse.data.location.isNearGreatLakesShore
import com.lakepulse.ui.alerts.AlertsRoute
import com.lakepulse.ui.alerts.AlertsViewModel
import com.lakepulse.ui.fishing.FishingReportsRoute
import com.lakepulse.ui.fishing.FishingReportsViewModel
import com.lakepulse.ui.help.HelpScreen
import com.lakepulse.ui.home.HomeRoute
import com.lakepulse.ui.home.HomeViewModel
import com.lakepulse.ui.map.FishingSpotFocus
import com.lakepulse.ui.map.SatelliteThermalMapRoute
import com.lakepulse.ui.map.TemperatureMapRoute
import com.lakepulse.ui.map.TemperatureMapViewModel

enum class AppTab(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    Map("Buoys", R.drawable.ic_nav_buoys),
    Satellite("Hybrid", R.drawable.ic_nav_satellite),
    Conditions("Conditions", R.drawable.ic_nav_conditions),
    Fishing("Fishing", R.drawable.ic_nav_fishing),
}

suspend fun resolveDefaultTab(locationClient: DeviceLocationClient): AppTab {
    if (!locationClient.hasLocationPermission()) return AppTab.Map
    val location = runCatching { locationClient.getLocation() }.getOrNull()
        ?: return AppTab.Map
    return if (location.isNearGreatLakesShore()) AppTab.Fishing else AppTab.Map
}

@Composable
fun LakePulseApp(
    mapViewModel: TemperatureMapViewModel,
    homeViewModel: HomeViewModel,
    fishingReportsViewModel: FishingReportsViewModel,
    alertsViewModel: AlertsViewModel,
    initialTab: AppTab = AppTab.Map,
) {
    var selectedTab by rememberSaveable(initialTab.name) { mutableStateOf(initialTab.name) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAlerts by rememberSaveable { mutableStateOf(false) }
    var spotFocus by remember { mutableStateOf<FishingSpotFocus?>(null) }
    val currentTab = AppTab.entries.firstOrNull { it.name == selectedTab } ?: AppTab.Map
    val openHelp = { showHelp = true }
    val openAlerts = { showAlerts = true }

    if (showAlerts) {
        AlertsRoute(
            viewModel = alertsViewModel,
            onBack = { showAlerts = false },
        )
        return
    }

    if (showHelp) {
        HelpScreen(
            onBack = { showHelp = false },
            onOpenAlerts = {
                showHelp = false
                showAlerts = true
            },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { selectedTab = tab.name },
                        icon = {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (currentTab) {
                AppTab.Map -> TemperatureMapRoute(
                    viewModel = mapViewModel,
                    onOpenHelp = openHelp,
                )
                AppTab.Satellite -> SatelliteThermalMapRoute(
                    buoyViewModel = mapViewModel,
                    spotFocus = spotFocus,
                    onSpotFocusConsumed = { spotFocus = null },
                    onOpenHelp = openHelp,
                )
                AppTab.Conditions -> HomeRoute(
                    viewModel = homeViewModel,
                    onBuoySelected = { stationId ->
                        mapViewModel.focusBuoy(stationId)
                        selectedTab = AppTab.Map.name
                    },
                    onOpenHelp = openHelp,
                    onOpenAlerts = openAlerts,
                )
                AppTab.Fishing -> FishingReportsRoute(
                    viewModel = fishingReportsViewModel,
                    onOpenHelp = openHelp,
                    onOpenAlerts = openAlerts,
                    onShowOnChart = { location ->
                        spotFocus = FishingSpotFocus(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            label = location.name,
                        )
                        selectedTab = AppTab.Satellite.name
                    },
                )
            }
        }
    }
}
