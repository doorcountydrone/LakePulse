package com.lakepulse.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lakepulse.data.model.BuoyNames
import com.lakepulse.data.model.BuoyObservation
import com.lakepulse.data.model.BuoySort
import com.lakepulse.data.model.BuoyTempTrend
import com.lakepulse.data.model.GreatLake
import com.lakepulse.data.model.LakeBoard
import com.lakepulse.data.model.RankedBuoy
import com.lakepulse.data.model.RelativeTime
import com.lakepulse.data.model.toObservationAgeLabel
import com.lakepulse.data.model.windDirectionLabel
import com.lakepulse.ui.components.CalmEmptyState
import com.lakepulse.ui.components.SkeletonCardList
import com.lakepulse.ui.theme.LakePulseTheme
import com.lakepulse.ui.theme.SplashCyan
import com.lakepulse.ui.theme.SplashGold
import com.lakepulse.ui.theme.SplashNavy
import com.lakepulse.ui.theme.SplashScreenGradientColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onBuoySelected: (String) -> Unit = {},
    onOpenHelp: () -> Unit = {},
    onOpenAlerts: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        when {
            fine || coarse -> viewModel.ensureLocation()
            else -> permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    HomeScreen(
        state = state,
        onLakeSelected = viewModel::selectLake,
        onSortSelected = viewModel::selectSort,
        onRefresh = { viewModel.refresh(force = true) },
        onBuoySelected = onBuoySelected,
        onToggleFavorite = viewModel::toggleFavoriteBuoy,
        onToggleTrend = viewModel::toggleTrendExpanded,
        onRetryTrend = viewModel::retryTrend,
        onOpenHelp = onOpenHelp,
        onOpenAlerts = onOpenAlerts,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onLakeSelected: (GreatLake) -> Unit,
    onSortSelected: (BuoySort) -> Unit,
    onRefresh: () -> Unit,
    onBuoySelected: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onToggleTrend: (String) -> Unit = {},
    onRetryTrend: (String) -> Unit = {},
    onOpenHelp: () -> Unit = {},
    onOpenAlerts: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🌊🌡️🌬️ Conditions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = when {
                                state.board?.sortedByDistance == true ->
                                    "Closest buoys first"
                                state.locationStatus != null ->
                                    state.locationStatus
                                else -> "Live buoy readings by lake"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAlerts) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Alerts")
                    }
                    IconButton(onClick = onOpenHelp) {
                        Icon(Icons.Filled.Info, contentDescription = "Help")
                    }
                    IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        val isRefreshing = state.isLoading && state.board != null
        val pullState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(SplashScreenGradientColors))
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    LakeSelector(
                        selected = state.selectedLake,
                        onSelected = onLakeSelected,
                    )
                }

                item {
                    SortSelector(
                        selected = state.sort,
                        onSelected = onSortSelected,
                    )
                }

                when {
                    state.isLoading && state.board == null -> {
                        item {
                            SkeletonCardList(count = 5)
                        }
                    }

                    state.errorMessage != null && state.board == null -> {
                        item {
                            CalmEmptyState(
                                title = "Couldn't load conditions",
                                message = state.errorMessage,
                                actionLabel = "Try again",
                                onAction = onRefresh,
                            )
                        }
                    }

                    state.board != null && state.board.buoys.isEmpty() -> {
                        item {
                            CalmEmptyState(
                                title = "No buoy readings",
                                message = "Nothing live for ${state.selectedLake.displayName} right now. Try another lake or refresh.",
                                actionLabel = "Refresh",
                                onAction = onRefresh,
                            )
                        }
                    }

                    state.board != null -> {
                        item {
                            LakeSummaryCard(
                                board = state.board,
                                isRefreshing = state.isLoading,
                                errorMessage = state.errorMessage,
                                locationStatus = state.locationStatus,
                            )
                        }

                        items(state.board.buoys, key = { it.stationId }) { buoy ->
                            BuoyConditionCard(
                                buoy = buoy,
                                isFavorite = buoy.stationId in state.favoriteBuoyIds,
                                trendExpanded = state.expandedTrendStationId == buoy.stationId,
                                trendUi = state.trendsByStationId[buoy.stationId],
                                onToggleFavorite = { onToggleFavorite(buoy.stationId) },
                                onToggleTrend = { onToggleTrend(buoy.stationId) },
                                onRetryTrend = { onRetryTrend(buoy.stationId) },
                                onClick = { onBuoySelected(buoy.stationId) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LakeSelector(
    selected: GreatLake,
    onSelected: (GreatLake) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(GreatLake.entries) { lake ->
            FilterChip(
                selected = lake == selected,
                onClick = { onSelected(lake) },
                label = { Text(lake.shortName) },
                colors = chipColors(selected = lake == selected),
                border = chipBorder(selected = lake == selected),
            )
        }
    }
}

@Composable
private fun SortSelector(
    selected: BuoySort,
    onSelected: (BuoySort) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(BuoySort.entries) { sort ->
            FilterChip(
                selected = sort == selected,
                onClick = { onSelected(sort) },
                label = { Text(sort.label) },
                colors = chipColors(selected = sort == selected),
                border = chipBorder(selected = sort == selected),
            )
        }
    }
}

@Composable
private fun chipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Color.White.copy(alpha = 0.95f),
    selectedLabelColor = SplashNavy,
    containerColor = Color.White.copy(alpha = 0.18f),
    labelColor = Color.White,
)

@Composable
private fun chipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = Color.White.copy(alpha = 0.35f),
    selectedBorderColor = Color.Transparent,
)

@Composable
private fun LakeSummaryCard(
    board: LakeBoard,
    isRefreshing: Boolean,
    errorMessage: String?,
    locationStatus: String?,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text(
            text = board.lake.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = buildString {
                if (board.sortedByDistance) {
                    append("${board.buoyCount} live buoys · nearest first")
                } else {
                    append("${board.buoyCount} live buoys")
                }
                RelativeTime.freshestAgeLabel(board.buoys.map { it.observation.observedAt })
                    ?.let { age -> append(" · updated $age") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        if (isRefreshing) {
            Text(
                text = "Refreshing…",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        locationStatus?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFCDD2),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = when {
                board.minTempF != null && board.maxTempF != null ->
                    "${board.minTempF.formatTemp()} – ${board.maxTempF.formatTemp()}"
                else -> "—"
            },
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "🌡️ Water temp range · avg ${board.avgTempF.formatTemp()}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryStat(
                modifier = Modifier.weight(1f),
                label = "🌬️ Windiest",
                value = board.windiest?.windSpeedMph.formatSpeed(),
                detail = board.windiest?.let {
                    "${BuoyNames.displayName(it.stationId)} · ${windDirectionLabel(it.windDirectionDeg)}"
                } ?: "—",
            )
            Spacer(modifier = Modifier.width(12.dp))
            SummaryStat(
                modifier = Modifier.weight(1f),
                label = "🌊 Biggest waves",
                value = board.biggestWaves?.waveHeightFt.formatFeet(),
                detail = board.biggestWaves?.let { BuoyNames.displayName(it.stationId) } ?: "—",
            )
        }
    }
}

@Composable
private fun SummaryStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    detail: String,
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(detail, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BuoyConditionCard(
    buoy: RankedBuoy,
    isFavorite: Boolean,
    trendExpanded: Boolean,
    trendUi: BuoyTrendUi?,
    onToggleFavorite: () -> Unit,
    onToggleTrend: () -> Unit,
    onRetryTrend: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val observation = buoy.observation
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BuoyNames.displayName(observation.stationId),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = buildString {
                        append("NDBC ${observation.stationId}")
                        buoy.distanceMiles?.let { miles ->
                            append(" · ${miles.formatDistance()}")
                        }
                    },
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) SplashGold else Color.White.copy(alpha = 0.35f),
                )
            }
            Text(
                text = observation.waterTempF.formatTemp(),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricColumn(
                modifier = Modifier.weight(1f),
                label = "🌬️ Wind",
                value = observation.windSpeedMph.formatSpeed(),
                detail = buildString {
                    append(windDirectionLabel(observation.windDirectionDeg))
                    observation.windGustMph?.let { append(" · gust ${it.roundToInt()} mph") }
                },
            )
            MetricColumn(
                modifier = Modifier.weight(1f),
                label = "🌊 Waves",
                value = observation.waveHeightFt.formatFeet(),
                detail = observation.observedAt.toObservationAgeLabel(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        BuoyTrendSection(
            expanded = trendExpanded,
            trendUi = trendUi,
            onToggle = onToggleTrend,
            onRetry = onRetryTrend,
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "🗺️ Tap card to view on buoy map",
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun BuoyTrendSection(
    expanded: Boolean,
    trendUi: BuoyTrendUi?,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
) {
    val trendInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SplashNavy.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = SplashCyan.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = trendInteraction,
                indication = null,
                onClick = onToggle,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = if (expanded) "📈 24h temperature · tap to hide" else "📈 24h temperature · tap to show",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        if (!expanded) return

        Spacer(modifier = Modifier.height(8.dp))
        when {
            trendUi?.isLoading == true || trendUi == null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SplashCyan,
                    )
                    Text(
                        text = "Loading NDBC history…",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            trendUi.errorMessage != null -> {
                Text(
                    text = trendUi.errorMessage,
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to retry",
                    color = SplashGold,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onRetry),
                )
            }

            trendUi.trend != null -> {
                val trend = trendUi.trend
                TempTrendSparkline(
                    trend = trend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF061F28))
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = trend.summaryLine(),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun TempTrendSparkline(
    trend: BuoyTempTrend,
    modifier: Modifier = Modifier,
) {
    val samples = trend.samples
    if (samples.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Not enough points for a chart",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }

    val lineColor = SplashCyan
    val fillColor = SplashGold.copy(alpha = 0.35f)
    val gridColor = Color.White.copy(alpha = 0.12f)
    val min = trend.minTempF ?: return
    val max = trend.maxTempF ?: return
    val span = (max - min).coerceAtLeast(0.4)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padY = 4f
        val usableH = (h - padY * 2).coerceAtLeast(1f)
        val stepX = if (samples.size == 1) 0f else w / (samples.size - 1)

        // Subtle mid guide so small temp swings are easier to read.
        drawLine(
            color = gridColor,
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.5f,
        )

        val path = Path()
        val fill = Path()
        samples.forEachIndexed { index, sample ->
            val x = index * stepX
            val y = padY + usableH * (1f - ((sample.waterTempF - min) / span).toFloat())
            if (index == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, h)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(w, h)
        fill.close()

        drawPath(path = fill, color = fillColor)
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )
        val last = samples.last()
        val lastX = (samples.size - 1) * stepX
        val lastY = padY + usableH * (1f - ((last.waterTempF - min) / span).toFloat())
        drawCircle(
            color = SplashGold,
            radius = 6.5f,
            center = Offset(lastX, lastY),
        )
        drawCircle(
            color = Color.White,
            radius = 2.5f,
            center = Offset(lastX, lastY),
        )
    }
}

private fun BuoyTempTrend.summaryLine(): String {
    val oldest = oldestTempF?.roundToInt()
    val latest = latestTempF?.roundToInt()
    val delta = deltaF
    val range = if (minTempF != null && maxTempF != null) {
        "${minTempF!!.roundToInt()}–${maxTempF!!.roundToInt()}°F"
    } else {
        null
    }
    val deltaLabel = when {
        delta == null -> null
        delta > 0.4 -> String.format(Locale.US, "+%.0f°F", delta)
        delta < -0.4 -> String.format(Locale.US, "%.0f°F", delta)
        else -> "steady"
    }
    return listOfNotNull(
        if (oldest != null && latest != null) "$oldest° → $latest°" else null,
        deltaLabel,
        range?.let { "range $it" },
        "${samples.size} readings",
    ).joinToString(" · ")
}

@Composable
private fun MetricColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    detail: String,
) {
    Column(modifier = modifier) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(detail, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
    }
}

private fun Double?.formatTemp(): String =
    this?.let { String.format(Locale.US, "%.0f°F", it) } ?: "—"

private fun Double?.formatSpeed(): String =
    this?.let { String.format(Locale.US, "%.0f mph", it) } ?: "—"

private fun Double?.formatFeet(): String =
    this?.let { String.format(Locale.US, "%.1f ft", it) } ?: "—"

private fun Double.formatDistance(): String =
    if (this < 10) {
        String.format(Locale.US, "%.1f mi away", this)
    } else {
        String.format(Locale.US, "%.0f mi away", this)
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val sample = listOf(
        RankedBuoy(
            observation = BuoyObservation(
                stationId = "45029",
                latitude = 42.9,
                longitude = -86.272,
                waterTempF = 75.0,
                windSpeedMph = 14.0,
                windGustMph = 19.0,
                windDirectionDeg = 200,
                waveHeightFt = 1.2,
                observedAt = "2026-07-14 22:50 UTC",
            ),
            distanceMiles = 8.4,
        ),
        RankedBuoy(
            observation = BuoyObservation(
                stationId = "45013",
                latitude = 43.1,
                longitude = -87.85,
                waterTempF = 72.0,
                windSpeedMph = 10.0,
                windGustMph = 14.0,
                windDirectionDeg = 190,
                waveHeightFt = 0.7,
                observedAt = "2026-07-14 21:00 UTC",
            ),
            distanceMiles = 42.0,
        ),
    )
    LakePulseTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(
                selectedLake = GreatLake.MICHIGAN,
                sort = BuoySort.NEAREST,
                board = LakeBoard(
                    lake = GreatLake.MICHIGAN,
                    buoys = sample,
                    minTempF = 72.0,
                    maxTempF = 75.0,
                    avgTempF = 73.5,
                    windiest = sample.first().observation,
                    biggestWaves = sample.first().observation,
                    sortedByDistance = true,
                ),
            ),
            onLakeSelected = {},
            onSortSelected = {},
            onRefresh = {},
            onBuoySelected = {},
        )
    }
}
