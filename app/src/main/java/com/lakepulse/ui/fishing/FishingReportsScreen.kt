package com.lakepulse.ui.fishing

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lakepulse.data.favorites.FavoritesStore
import com.lakepulse.data.model.FishingLocationReport
import com.lakepulse.data.model.FishingReportWeekSummary
import com.lakepulse.data.model.FishingSource
import com.lakepulse.data.model.NearestBuoyConditions
import com.lakepulse.data.model.biteSummary
import com.lakepulse.data.model.toObservationAgeLabel
import com.lakepulse.data.model.windDirectionLabel
import com.lakepulse.ui.components.CalmEmptyState
import com.lakepulse.ui.components.SkeletonCardList
import com.lakepulse.ui.theme.SplashCyan
import com.lakepulse.ui.theme.SplashGold
import com.lakepulse.ui.theme.SplashScreenGradientColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FishingReportsRoute(
    viewModel: FishingReportsViewModel,
    onOpenHelp: () -> Unit = {},
    onOpenAlerts: () -> Unit = {},
    onShowOnChart: (FishingLocationReport) -> Unit = {},
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

    FishingReportsScreen(
        state = state,
        onSourceSelected = viewModel::selectSource,
        onWeekSelected = viewModel::selectWeek,
        onLocationSelected = viewModel::selectLocation,
        onClearLocation = viewModel::clearSelectedLocation,
        onToggleFavorite = viewModel::toggleFavoriteFishing,
        onOpenHelp = onOpenHelp,
        onOpenAlerts = onOpenAlerts,
        onShowOnChart = onShowOnChart,
        onRefresh = { viewModel.refresh(force = true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingReportsScreen(
    state: FishingReportsUiState,
    onSourceSelected: (FishingSource) -> Unit,
    onWeekSelected: (String) -> Unit,
    onLocationSelected: (FishingLocationReport) -> Unit,
    onClearLocation: () -> Unit,
    onToggleFavorite: (FishingLocationReport) -> Unit,
    onOpenHelp: () -> Unit = {},
    onOpenAlerts: () -> Unit = {},
    onShowOnChart: (FishingLocationReport) -> Unit = {},
    onRefresh: () -> Unit,
) {
    val selectedLocation = state.selectedLocation
    if (selectedLocation != null) {
        BackHandler(onBack = onClearLocation)
        FishingReportDetailScreen(
            location = selectedLocation,
            weekTitle = state.week?.summary?.title.orEmpty(),
            nearestBuoy = state.nearestBuoyByLocationId[selectedLocation.id],
            isFavorite = FavoritesStore.fishingKey(selectedLocation) in state.favoriteFishingKeys,
            onToggleFavorite = { onToggleFavorite(selectedLocation) },
            onShowOnChart = { onShowOnChart(selectedLocation) },
            onBack = onClearLocation,
        )
        return
    }

    val sourceLabel = when (state.source) {
        FishingSource.MICHIGAN -> "Michigan DNR"
        FishingSource.WISCONSIN -> "Wisconsin DNR"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🎣 Fishing Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = when {
                                state.userLocation != null -> "$sourceLabel · closest first"
                                state.locationStatus != null -> state.locationStatus
                                else -> "$sourceLabel outdoor reports"
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
        val isRefreshing = state.isLoading && state.week != null
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
                    Text(
                        text = "State",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                item {
                    SourceSelector(
                        selected = state.source,
                        onSelected = onSourceSelected,
                    )
                }

                item {
                    Text(
                        text = when (state.source) {
                            FishingSource.MICHIGAN -> "🎣 Latest weekly reports"
                            FishingSource.WISCONSIN -> "🎣 Latest state reports"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                item {
                    WeekSelector(
                        weeks = state.weeks,
                        selectedWeekId = state.selectedWeekId,
                        onSelected = onWeekSelected,
                    )
                }

                when {
                    state.isLoading && state.week == null -> {
                        item {
                            SkeletonCardList(count = 5)
                        }
                    }

                    state.errorMessage != null && state.week == null -> {
                        item {
                            CalmEmptyState(
                                title = "Couldn't load reports",
                                message = state.errorMessage,
                                actionLabel = "Try again",
                                onAction = onRefresh,
                            )
                        }
                    }

                    state.week != null && state.week.locations.isEmpty() -> {
                        item {
                            CalmEmptyState(
                                title = "No locations this week",
                                message = when (state.source) {
                                    FishingSource.MICHIGAN ->
                                        "This Michigan DNR report didn’t list any spots. Try another week."
                                    FishingSource.WISCONSIN ->
                                        "This Wisconsin DNR report didn’t list any spots. Try Lake Superior or refresh."
                                },
                                actionLabel = "Refresh",
                                onAction = onRefresh,
                            )
                        }
                    }

                    state.week != null -> {
                        item {
                            Text(
                                text = "🗺️ ${state.week.locations.size} locations · tap for full report",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        itemsIndexed(
                            state.week.locations,
                            key = { index, location -> "${location.id}#$index" },
                        ) { _, location ->
                            FishingLocationCard(
                                location = location,
                                showDistance = state.userLocation != null,
                                nearestBuoy = state.nearestBuoyByLocationId[location.id],
                                isFavorite = FavoritesStore.fishingKey(location) in state.favoriteFishingKeys,
                                onToggleFavorite = { onToggleFavorite(location) },
                                onClick = { onLocationSelected(location) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        item {
                            Text(
                                text = when (state.source) {
                                    FishingSource.MICHIGAN ->
                                        "Source: Michigan DNR weekly fishing report"
                                    FishingSource.WISCONSIN ->
                                        "Source: Wisconsin DNR outdoor fishing report"
                                },
                                color = Color.White.copy(alpha = 0.55f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FishingReportDetailScreen(
    location: FishingLocationReport,
    weekTitle: String,
    nearestBuoy: NearestBuoyConditions?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShowOnChart: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(
                                weekTitle.takeIf { it.isNotBlank() },
                                location.region.displayName,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) SplashGold else Color.White.copy(alpha = 0.45f),
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
        ) {
            location.distanceMiles?.let { miles ->
                Text(
                    text = "${miles.formatDistance()} away",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            nearestBuoy?.let { buoy ->
                NearestBuoyCard(buoy = buoy, detailed = true)
                Spacer(modifier = Modifier.height(16.dp))
            }
            TextButton(
                onClick = onShowOnChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SplashCyan.copy(alpha = 0.22f)),
            ) {
                Text(
                    text = "🗺️ Show on chart",
                    color = SplashCyan,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = location.body,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = when (location.source) {
                    FishingSource.MICHIGAN -> "Michigan DNR weekly fishing report"
                    FishingSource.WISCONSIN -> "Wisconsin DNR outdoor fishing report"
                },
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SourceSelector(
    selected: FishingSource,
    onSelected: (FishingSource) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FishingSource.entries, key = { it.name }) { source ->
            FilterChip(
                selected = source == selected,
                onClick = { onSelected(source) },
                label = { Text(text = source.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    labelColor = Color.White,
                    selectedContainerColor = SplashCyan.copy(alpha = 0.28f),
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun WeekSelector(
    weeks: List<FishingReportWeekSummary>,
    selectedWeekId: String?,
    onSelected: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(weeks.take(12), key = { it.id }) { week ->
            FilterChip(
                selected = week.id == selectedWeekId,
                onClick = { onSelected(week.id) },
                label = {
                    Text(
                        text = week.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    labelColor = Color.White,
                    selectedContainerColor = SplashCyan.copy(alpha = 0.28f),
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun FishingLocationCard(
    location: FishingLocationReport,
    showDistance: Boolean,
    nearestBuoy: NearestBuoyConditions?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    text = location.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = location.biteSummary(),
                    color = SplashGold,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(location.region.displayName)
                        if (showDistance) {
                            location.distanceMiles?.let { miles ->
                                append(" · ${miles.formatDistance()}")
                            }
                        }
                    },
                    color = Color.White.copy(alpha = 0.7f),
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
        }
        nearestBuoy?.let { buoy ->
            Spacer(modifier = Modifier.height(10.dp))
            NearestBuoyCard(buoy = buoy, detailed = false)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = location.body,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap to read full report",
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun NearestBuoyCard(
    buoy: NearestBuoyConditions,
    detailed: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "🌡️ Nearest buoy · ${buoy.displayName}",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildString {
                append(buoy.waterTempF.roundToInt())
                append("°F")
                buoy.windSpeedMph?.let { wind ->
                    append(" · 🌬️ ")
                    append(wind.roundToInt())
                    append(" mph")
                    append(" ")
                    append(windDirectionLabel(buoy.windDirectionDeg))
                }
                buoy.waveHeightFt?.let { waves ->
                    append(" · 🌊 ")
                    append(String.format(Locale.US, "%.1f ft", waves))
                }
                append(" · ")
                append(buoy.distanceMiles.formatDistance())
                append(" away")
                buoy.observedAt.toObservationAgeLabel(unknown = "").takeIf { it.isNotBlank() }
                    ?.let { age ->
                        append(" · ")
                        append(age)
                    }
            },
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (detailed) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Live NDBC ${buoy.stationId}",
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun Double.formatDistance(): String =
    if (this < 10) {
        String.format(Locale.US, "%.1f mi", this)
    } else {
        "${roundToInt()} mi"
    }
