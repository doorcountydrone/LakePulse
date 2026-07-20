package com.lakepulse.ui.map

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.model.RelativeTime
import com.lakepulse.ui.components.CalmEmptyState
import com.lakepulse.ui.theme.DeepWater
import com.lakepulse.ui.theme.Mist
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TemperatureMapRoute(
    viewModel: TemperatureMapViewModel,
    onOpenHelp: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    TemperatureMapScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onFocusConsumed = viewModel::clearFocusRequest,
        onOpenHelp = onOpenHelp,
    )
}

@Composable
fun TemperatureMapScreen(
    state: TemperatureMapUiState,
    onRefresh: () -> Unit,
    onFocusConsumed: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
) {
    val context = LocalContext.current
    val basemapStore = remember { MapBasemapStore(context) }
    var basemap by remember {
        mutableStateOf(basemapStore.get(MapBasemapTab.Buoys))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepWater),
    ) {
        if (state.buoys.isNotEmpty()) {
            BuoyTemperatureWebMap(
                buoys = state.buoys,
                focusStationId = state.focusStationId,
                basemap = basemap,
                onFocusConsumed = onFocusConsumed,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            MapTopChrome(
                title = "🌡️ Buoy Temperatures",
                subtitle = mapSubtitle(state, basemap),
                onRefresh = onRefresh,
                refreshEnabled = !state.isLoading,
                onOpenHelp = onOpenHelp,
                topTrailing = {
                    MapBasemapToggle(
                        selected = basemap,
                        onSelected = { next ->
                            basemap = next
                            basemapStore.set(MapBasemapTab.Buoys, next)
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            MapBottomScrim(
                modifier = Modifier.height(100.dp),
            )
        }

        TemperatureLegend(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        when {
            state.isLoading && state.buoys.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Mist,
                )
            }

            state.errorMessage != null && state.buoys.isEmpty() -> {
                CalmEmptyState(
                    title = "Map unavailable",
                    message = state.errorMessage,
                    actionLabel = "Try again",
                    onAction = onRefresh,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun TemperatureLegend(modifier: Modifier = Modifier) {
    val thermalColors = listOf(
        Color(0xFF042333),
        Color(0xFF2C1E5B),
        Color(0xFF6B1D5A),
        Color(0xFFB22A4A),
        Color(0xFFE45D2B),
        Color(0xFFF4A261),
        Color(0xFFF7E7A1),
    )
    val tickLabels = listOf("41", "50", "59", "68", "75", "82")

    MapLegendCard(modifier = modifier) {
        Text(
            text = "🌡️ Observed buoy water temp (°F)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    brush = Brush.horizontalGradient(colors = thermalColors),
                    shape = RoundedCornerShape(6.dp),
                ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            tickLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.90f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap a buoy for wind and waves",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BuoyTemperatureWebMap(
    buoys: List<BuoyObservation>,
    focusStationId: String?,
    basemap: MapBasemap,
    onFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    val json = remember(buoys) { BuoyMapJson.toJson(buoys) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.parseColor("#D9E6E4"))
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onFocused(stationId: String) {
                            post { onFocusConsumed() }
                        }
                    },
                    "BuoyMapBridge",
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageReady = true
                    }
                }
                loadUrl("file:///android_asset/map/temperature_map.html")
                webView = this
            }
        },
        update = { view ->
            webView = view
        },
    )

    LaunchedEffect(json, pageReady, focusStationId) {
        if (!pageReady) return@LaunchedEffect
        val focusArg = focusStationId?.let { "\"$it\"" } ?: "null"
        webView?.evaluateJavascript("setObservations($json, $focusArg);", null)
    }

    LaunchedEffect(basemap, pageReady) {
        if (!pageReady) return@LaunchedEffect
        webView?.evaluateJavascript("setBasemap('${basemap.jsMode}');", null)
    }
}

private fun mapSubtitle(state: TemperatureMapUiState, basemap: MapBasemap): String {
    if (state.isLoading && state.buoys.isEmpty()) return "Loading NDBC buoy observations…"
    if (state.buoys.isEmpty()) return "No live buoy readings"
    val temps = state.buoys.map { it.waterTempF }
    val min = temps.minOrNull()?.roundToInt() ?: return ""
    val max = temps.maxOrNull()?.roundToInt() ?: return ""
    val freshest = RelativeTime.freshestAgeLabel(state.buoys.map { it.observedAt })
    val readings = buildString {
        append(
            String.format(
                Locale.US,
                "%d buoy readings · %d°F to %d°F",
                state.buoys.size,
                min,
                max,
            ),
        )
        if (freshest != null) append(" · updated $freshest")
    }
    return if (basemap == MapBasemap.Chart) {
        "$readings · NOAA chart (not for navigation)"
    } else {
        readings
    }
}
