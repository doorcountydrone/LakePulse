package com.lakepulse.ui.map

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.model.RelativeTime
import com.lakepulse.data.remote.HttpClient
import com.lakepulse.data.remote.NetworkModule
import com.lakepulse.ui.components.CalmEmptyState
import com.lakepulse.ui.theme.DeepWater
import com.lakepulse.ui.theme.Mist
import com.lakepulse.ui.theme.SunsetAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val OBSERVED_LOCAL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a z", Locale.getDefault())

/** Prefer nearest buoy within this range when showing "live" context on a satellite tap. */
private const val NEARBY_BUOY_MILES = 35.0

private const val DEFAULT_MAP_THERMAL_OPACITY = 0.95f
private const val DEFAULT_CHART_THERMAL_OPACITY = 0.55f

private const val THERMAL_IMAGE_URL =
    "https://apps.glerl.noaa.gov/erddap/griddap/GLSEA_ACSPO_GCS.transparentPng" +
        "?sst%5B(last)%5D%5B(41.0):(49.0)%5D%5B(-92.4):(-76.0)%5D" +
        "&.colorBar=KT_thermal|C|False|5|28|" +
        "&.size=1200|800"

private const val THERMAL_POINT_URL_TEMPLATE =
    "https://apps.glerl.noaa.gov/erddap/griddap/GLSEA_ACSPO_GCS.json" +
        "?sst[(last)][(%s)][(%s)]"

/** Lake Michigan mid-lake sample used only to read the latest analysis timestamp. */
private const val THERMAL_LATEST_META_URL =
    "https://apps.glerl.noaa.gov/erddap/griddap/GLSEA_ACSPO_GCS.json" +
        "?sst%5B(last)%5D%5B(43.5)%5D%5B(-87.0)%5D"

private data class ThermalLayerPayload(
    val fileUrl: String,
    val analysisSubtitle: String,
)

private val ANALYSIS_DAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@Composable
fun SatelliteThermalMapRoute(
    buoyViewModel: TemperatureMapViewModel,
    spotFocus: FishingSpotFocus? = null,
    onSpotFocusConsumed: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
) {
    val buoyState by buoyViewModel.uiState.collectAsState()
    SatelliteThermalMapScreen(
        buoys = buoyState.buoys,
        focusStationId = buoyState.focusStationId,
        spotFocus = spotFocus,
        onRefreshBuoys = buoyViewModel::refresh,
        onFocusConsumed = buoyViewModel::clearFocusRequest,
        onSpotFocusConsumed = onSpotFocusConsumed,
        onOpenHelp = onOpenHelp,
    )
}

@Composable
fun SatelliteThermalMapScreen(
    buoys: List<BuoyObservation> = emptyList(),
    focusStationId: String? = null,
    spotFocus: FishingSpotFocus? = null,
    onRefreshBuoys: () -> Unit = {},
    onFocusConsumed: () -> Unit = {},
    onSpotFocusConsumed: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val basemapStore = remember { MapBasemapStore(context) }
    var basemap by remember {
        mutableStateOf(basemapStore.get(MapBasemapTab.Hybrid))
    }
    var chartThermalOpacity by remember { mutableFloatStateOf(DEFAULT_CHART_THERMAL_OPACITY) }
    var sliderPos by remember { mutableStateOf(Offset.Unspecified) }
    var sliderCardSize by remember { mutableStateOf(IntSize.Zero) }
    var mapAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var subtitle by remember {
        mutableStateOf("NOAA GLSEA · loading latest analysis…")
    }
    var analysisSubtitle by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val sidePadPx = with(density) { 16.dp.toPx() }
    val minSliderYPx = with(density) { 8.dp.toPx() }
    val defaultSliderYPx = with(density) { 148.dp.toPx() }
    val bottomPadPx = with(density) { 8.dp.toPx() }

    val effectiveThermalOpacity =
        if (basemap == MapBasemap.Chart) chartThermalOpacity else DEFAULT_MAP_THERMAL_OPACITY

    LaunchedEffect(pageReady, refreshKey, webView) {
        if (!pageReady) return@LaunchedEffect
        val view = webView ?: return@LaunchedEffect

        isLoading = true
        errorMessage = null
        subtitle = "NOAA GLSEA · loading latest analysis…"
        analysisSubtitle = null
        onRefreshBuoys()

        val result = withContext(Dispatchers.IO) {
            downloadThermalToCache(view.context.cacheDir)
        }

        // Always clear spinner from Compose, independent of WebView.
        isLoading = false

        result.fold(
            onSuccess = { payload ->
                analysisSubtitle = payload.analysisSubtitle
                subtitle = displaySubtitle(payload.analysisSubtitle, basemap, buoys.size)
                view.evaluateJavascript(
                    "applyThermalUrl(${JSONObject.quote(payload.fileUrl)});",
                    null,
                )
                view.evaluateJavascript(
                    "setThermalOpacity($effectiveThermalOpacity);",
                    null,
                )
            },
            onFailure = { error ->
                analysisSubtitle = null
                subtitle = displaySubtitle("NOAA GLSEA · unavailable", basemap, buoys.size)
                errorMessage = error.message ?: "Satellite layer failed to load"
            },
        )
    }

    LaunchedEffect(basemap, pageReady, webView, buoys.size, analysisSubtitle, errorMessage) {
        if (!pageReady) return@LaunchedEffect
        val view = webView ?: return@LaunchedEffect
        view.evaluateJavascript("setBasemap('${basemap.jsMode}');", null)
        analysisSubtitle?.let { subtitle = displaySubtitle(it, basemap, buoys.size) }
            ?: run {
                if (errorMessage != null) {
                    subtitle = displaySubtitle("NOAA GLSEA · unavailable", basemap, buoys.size)
                }
            }
    }

    LaunchedEffect(effectiveThermalOpacity, pageReady, webView) {
        if (!pageReady) return@LaunchedEffect
        val view = webView ?: return@LaunchedEffect
        view.evaluateJavascript("setThermalOpacity($effectiveThermalOpacity);", null)
    }

    LaunchedEffect(pageReady, webView, buoys, focusStationId) {
        if (!pageReady) return@LaunchedEffect
        val view = webView ?: return@LaunchedEffect
        if (buoys.isEmpty()) return@LaunchedEffect
        val json = BuoyMapJson.toJson(buoys)
        val focusArg = if (spotFocus != null) {
            "null"
        } else {
            focusStationId?.let { JSONObject.quote(it) } ?: "null"
        }
        view.evaluateJavascript("setObservations($json, $focusArg);", null)
    }

    LaunchedEffect(spotFocus, pageReady, webView) {
        val focus = spotFocus ?: return@LaunchedEffect
        basemap = MapBasemap.Chart
        basemapStore.set(MapBasemapTab.Hybrid, MapBasemap.Chart)
        if (!pageReady) return@LaunchedEffect
        val view = webView ?: return@LaunchedEffect
        view.evaluateJavascript("setBasemap('chart');", null)
        view.evaluateJavascript(
            "focusSpot(${focus.latitude}, ${focus.longitude}, ${JSONObject.quote(focus.label)});",
            null,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepWater)
            .onSizeChanged { mapAreaSize = it },
    ) {
        SatelliteThermalWebMap(
            onReady = { view ->
                webView = view
            },
            onPageReady = {
                pageReady = true
            },
            onBuoyFocused = onFocusConsumed,
            onSpotFocused = onSpotFocusConsumed,
            modifier = Modifier.fillMaxSize(),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            MapTopChrome(
                title = "🌡️ Surface + Buoys",
                subtitle = subtitle,
                onRefresh = {
                    refreshKey += 1
                },
                onOpenHelp = onOpenHelp,
                topTrailing = {
                    MapBasemapToggle(
                        selected = basemap,
                        onSelected = { next ->
                            basemap = next
                            basemapStore.set(MapBasemapTab.Hybrid, next)
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            MapBottomScrim(
                modifier = Modifier.height(96.dp),
            )
        }

        if (basemap == MapBasemap.Chart) {
            val cardW = sliderCardSize.width.takeIf { it > 0 }
                ?: with(density) { 360.dp.roundToPx() }
            val cardH = sliderCardSize.height.takeIf { it > 0 }
                ?: with(density) { 88.dp.roundToPx() }
            val maxX = (mapAreaSize.width - cardW - sidePadPx).coerceAtLeast(sidePadPx)
            val maxY = (mapAreaSize.height - cardH - bottomPadPx)
                .coerceAtLeast(minSliderYPx)
            val resolvedPos = if (sliderPos == Offset.Unspecified) {
                Offset(
                    x = sidePadPx,
                    y = defaultSliderYPx.coerceIn(minSliderYPx, maxY),
                )
            } else {
                Offset(
                    x = sliderPos.x.coerceIn(sidePadPx, maxX),
                    y = sliderPos.y.coerceIn(minSliderYPx, maxY),
                )
            }

            ThermalOpacitySlider(
                opacity = chartThermalOpacity,
                onOpacityChange = { chartThermalOpacity = it },
                onDragBy = { delta ->
                    val base = if (sliderPos == Offset.Unspecified) resolvedPos else sliderPos
                    sliderPos = Offset(
                        x = (base.x + delta.x).coerceIn(sidePadPx, maxX),
                        y = (base.y + delta.y).coerceIn(minSliderYPx, maxY),
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            x = resolvedPos.x.roundToInt(),
                            y = resolvedPos.y.roundToInt(),
                        )
                    }
                    .padding(end = 16.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .onSizeChanged { sliderCardSize = it },
            )
        }

        SatelliteLegend(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )

        if (isLoading && errorMessage == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Mist,
            )
        }

        errorMessage?.let { message ->
            CalmEmptyState(
                title = "Satellite unavailable",
                message = message,
                actionLabel = "Try again",
                onAction = {
                    refreshKey += 1
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun ThermalOpacitySlider(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    onDragBy: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragBy(dragAmount)
                    }
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "⠿",
                color = Color.Black.copy(alpha = 0.45f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Thermal vs chart depths",
                color = Color.Black,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Drag",
                color = Color.Black.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Depths",
                color = Color.Black.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
            )
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0.15f..0.95f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = SunsetAccent,
                    activeTrackColor = SunsetAccent,
                    inactiveTrackColor = Color.Black.copy(alpha = 0.18f),
                ),
            )
            Text(
                text = "Temp",
                color = Color.Black.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun displaySubtitle(
    base: String,
    basemap: MapBasemap,
    buoyCount: Int,
): String {
    val withBuoys = if (buoyCount > 0) {
        "$base · $buoyCount live buoys"
    } else {
        base
    }
    return if (basemap == MapBasemap.Chart) {
        "$withBuoys · NOAA chart (not for navigation)"
    } else {
        withBuoys
    }
}

private fun downloadThermalToCache(cacheDir: File): Result<ThermalLayerPayload> {
    var lastError: Exception? = null
    repeat(3) { attempt ->
        try {
            val analysisTime = runCatching {
                val meta = HttpClient.get(THERMAL_LATEST_META_URL)
                JSONObject(meta)
                    .getJSONObject("table")
                    .getJSONArray("rows")
                    .optJSONArray(0)
                    ?.optString(0)
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()

            val bytes = HttpClient.getBytes(
                url = THERMAL_IMAGE_URL,
                accept = "image/png,*/*",
                readTimeoutMs = 60_000,
            )
            require(bytes.size > 1_000) { "Empty satellite image" }
            val file = File(cacheDir, "glsea_sst_thermal.png")
            file.writeBytes(bytes)
            return Result.success(
                ThermalLayerPayload(
                    fileUrl = Uri.fromFile(file).toString(),
                    analysisSubtitle = formatAnalysisSubtitle(analysisTime),
                ),
            )
        } catch (error: Exception) {
            lastError = error
            if (attempt < 2) {
                Thread.sleep(700L * (attempt + 1))
            }
        }
    }
    return Result.failure(lastError ?: IllegalStateException("Satellite layer failed to load"))
}

private fun formatAnalysisSubtitle(rawTime: String?): String {
    if (rawTime.isNullOrBlank()) {
        return "NOAA GLSEA · latest available analysis · tap for temp"
    }
    return runCatching {
        val instant = RelativeTime.parseInstant(rawTime) ?: error("unparseable")
        val localDay = instant.atZone(ZoneId.systemDefault()).format(ANALYSIS_DAY_FORMAT)
        val age = RelativeTime.formatAge(instant)
        "NOAA GLSEA · $localDay analysis ($age) · tap for temp"
    }.getOrElse {
        "NOAA GLSEA · latest available analysis · tap for temp"
    }
}

@Composable
private fun SatelliteLegend(modifier: Modifier = Modifier) {
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
            text = "🌡️ Satellite + live buoy pins (°F)",
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
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Colder",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
            )
            Text(
                text = "Warmer",
                style = MaterialTheme.typography.labelSmall,
                color = SunsetAccent,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SatelliteThermalWebMap(
    onReady: (WebView) -> Unit,
    onPageReady: () -> Unit,
    onBuoyFocused: () -> Unit,
    onSpotFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onPageReadyRef = remember { AtomicReference(onPageReady) }
    onPageReadyRef.set(onPageReady)
    val onBuoyFocusedRef = remember { AtomicReference(onBuoyFocused) }
    onBuoyFocusedRef.set(onBuoyFocused)
    val onSpotFocusedRef = remember { AtomicReference(onSpotFocused) }
    onSpotFocusedRef.set(onSpotFocused)

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
                settings.allowFileAccess = true
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = true
                setBackgroundColor(android.graphics.Color.parseColor("#D9E6E4"))

                addJavascriptInterface(
                    SatelliteMapBridge(
                        webView = this,
                        notifyBuoyFocused = { onBuoyFocusedRef.get().invoke() },
                        notifySpotFocused = { onSpotFocusedRef.get().invoke() },
                    ),
                    "SatelliteBridge",
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageReadyRef.get().invoke()
                    }
                }
                loadUrl("file:///android_asset/map/satellite_thermal_map.html")
                onReady(this)
            }
        },
    )
}

private class SatelliteMapBridge(
    private val webView: WebView,
    private val notifyBuoyFocused: () -> Unit,
    private val notifySpotFocused: () -> Unit,
) {
    @JavascriptInterface
    fun onBuoyFocused(stationId: String) {
        webView.post { notifyBuoyFocused() }
    }

    @JavascriptInterface
    fun onSpotFocused() {
        webView.post { notifySpotFocused() }
    }

    @JavascriptInterface
    fun fetchPointTemp(lat: Double, lon: Double, requestId: Int) {
        Thread {
            val resultJs = runCatching {
                val url = THERMAL_POINT_URL_TEMPLATE.format(
                    "%.4f".format(Locale.US, lat),
                    "%.4f".format(Locale.US, lon),
                )
                val body = HttpClient.get(url)
                val row = JSONObject(body)
                    .getJSONObject("table")
                    .getJSONArray("rows")
                    .optJSONArray(0)

                if (row == null) {
                    "onPointTempResult($requestId, null, null, null, null);"
                } else {
                    val observedAtRaw = row.optString(0, "").takeIf { it.isNotBlank() }
                    val observedAtLocal = observedAtRaw?.let(::formatSatelliteAnalysisTime)
                    // GLSEA JSON columns: time, latitude, longitude, sst
                    val tempC = if (row.isNull(3)) null else row.optDouble(3)
                    val tempLiteral = tempC?.takeUnless { it.isNaN() }?.toString() ?: "null"
                    val timeLiteral = if (observedAtLocal == null) {
                        "null"
                    } else {
                        JSONObject.quote(observedAtLocal)
                    }
                    val liveBuoyLiteral = JSONObject.quote(
                        nearestLiveBuoyLine(lat, lon).orEmpty(),
                    )
                    "onPointTempResult($requestId, $tempLiteral, $timeLiteral, $liveBuoyLiteral, null);"
                }
            }.getOrElse { error ->
                val message = JSONObject.quote(error.message ?: "request failed")
                "onPointTempResult($requestId, null, null, null, $message);"
            }

            webView.post {
                webView.evaluateJavascript(resultJs, null)
            }
        }.start()
    }
}

/**
 * GLSEA is a daily Great Lakes SST analysis — usually fresher than global MUR,
 * but still not minute-by-minute live buoy data.
 */
private fun formatSatelliteAnalysisTime(raw: String): String {
    return runCatching {
        val instant = RelativeTime.parseInstant(raw) ?: error("unparseable")
        val local = instant.atZone(ZoneId.systemDefault()).format(OBSERVED_LOCAL_FORMAT)
        val age = RelativeTime.formatAge(instant)
        "$local · latest analysis ($age)"
    }.getOrElse { raw }
}

private fun nearestLiveBuoyLine(lat: Double, lon: Double): String? {
    return runCatching {
        val buoys = NetworkModule.ndbcLatestObsClient.fetchGreatLakes()
        val nearest = buoys
            .map { buoy ->
                buoy to haversineMiles(lat, lon, buoy.latitude, buoy.longitude)
            }
            .minByOrNull { it.second }
            ?: return null
        if (nearest.second > NEARBY_BUOY_MILES) return null
        val buoy = nearest.first
        val miles = nearest.second
        val distance = if (miles < 10) {
            String.format(Locale.US, "%.1f mi", miles)
        } else {
            String.format(Locale.US, "%.0f mi", miles)
        }
        val temp = Math.round(buoy.waterTempF)
        val whenLocal = buoy.observedAt
            ?.let { raw ->
                runCatching {
                    val instant = RelativeTime.parseInstant(raw) ?: return@runCatching null
                    val local = instant.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("h:mm a z", Locale.getDefault()))
                    val age = RelativeTime.formatAge(instant)
                    "$local ($age)"
                }.getOrNull()
            }
        buildString {
            append("Live buoy ${buoy.stationId}: ${temp}°F · $distance away")
            if (whenLocal != null) append(" · $whenLocal")
        }
    }.getOrNull()
}

private fun haversineMiles(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val earthRadiusMiles = 3958.8
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusMiles * c
}
