package com.lakepulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.lakepulse.data.alerts.AlertWorkScheduler
import com.lakepulse.data.alerts.AlertsStore
import com.lakepulse.data.favorites.FavoritesStore
import com.lakepulse.data.location.DeviceLocationClient
import com.lakepulse.ui.AppTab
import com.lakepulse.ui.LakePulseApp
import com.lakepulse.ui.SplashScreen
import com.lakepulse.ui.alerts.AlertsViewModel
import com.lakepulse.ui.alerts.AlertsViewModelFactory
import com.lakepulse.ui.fishing.FishingReportsViewModel
import com.lakepulse.ui.fishing.FishingReportsViewModelFactory
import com.lakepulse.ui.home.HomeViewModel
import com.lakepulse.ui.home.HomeViewModelFactory
import com.lakepulse.ui.map.TemperatureMapViewModel
import com.lakepulse.ui.map.TemperatureMapViewModelFactory
import com.lakepulse.ui.resolveDefaultTab
import com.lakepulse.ui.theme.LakePulseTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val locationClient = DeviceLocationClient(applicationContext)
        val favoritesStore = FavoritesStore(applicationContext)
        val alertsStore = AlertsStore(applicationContext)
        AlertWorkScheduler.schedule(applicationContext)

        val mapViewModel = ViewModelProvider(
            this,
            TemperatureMapViewModelFactory(),
        )[TemperatureMapViewModel::class.java]

        val homeViewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(locationClient, favoritesStore),
        )[HomeViewModel::class.java]

        val fishingReportsViewModel = ViewModelProvider(
            this,
            FishingReportsViewModelFactory(locationClient, favoritesStore),
        )[FishingReportsViewModel::class.java]

        val alertsViewModel = ViewModelProvider(
            this,
            AlertsViewModelFactory(alertsStore, favoritesStore),
        )[AlertsViewModel::class.java]

        setContent {
            LakePulseTheme {
                var showSplash by remember { mutableStateOf(true) }
                var initialTab by remember { mutableStateOf<AppTab?>(null) }

                LaunchedEffect(Unit) {
                    val tabDeferred = async { resolveDefaultTab(locationClient) }
                    delay(SPLASH_HOLD_MS)
                    initialTab = tabDeferred.await()
                    showSplash = false
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        initialTab?.let { tab ->
                            LakePulseApp(
                                mapViewModel = mapViewModel,
                                homeViewModel = homeViewModel,
                                fishingReportsViewModel = fishingReportsViewModel,
                                alertsViewModel = alertsViewModel,
                                initialTab = tab,
                            )
                        }

                        AnimatedVisibility(
                            visible = showSplash,
                            enter = EnterTransition.None,
                            exit = fadeOut(animationSpec = tween(SPLASH_FADE_MS)),
                        ) {
                            SplashScreen()
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** How long the splash stays on screen. Change this value to adjust timing. */
        private const val SPLASH_HOLD_MS = 3_000L
        private const val SPLASH_FADE_MS = 500
    }
}
