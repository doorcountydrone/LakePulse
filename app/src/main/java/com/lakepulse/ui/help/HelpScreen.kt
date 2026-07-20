package com.lakepulse.ui.help

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lakepulse.ui.theme.SplashCyan
import com.lakepulse.ui.theme.SplashGold
import com.lakepulse.ui.theme.SplashScreenGradientColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenAlerts: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & Disclaimer",
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
        ) {
            HelpSection(
                title = "How to use LakePulse",
                body = "LakePulse shows Great Lakes water conditions from public buoy, satellite, and state DNR sources. Use the bottom tabs to switch views.",
            )

            HelpSection(
                title = "🌡️ Buoys",
                body = "Live NDBC buoy water temperatures on a map. Marker color follows the cold→warm scale. Tap a buoy for wind, gusts, waves, and how old the observation is. Switch Map / Chart for a free NOAA ENC chart basemap with soundings and depth contours (not for navigation—zoom in to read depths). Your last Map/Chart choice is remembered per tab. Pull refresh to reload the latest observations.",
            )

            HelpSection(
                title = "🌊 Hybrid (Satellite)",
                body = "Lake-wide surface temperature from NOAA GLSEA satellite analysis, with live NDBC buoy pins on the same map. Tap water for a satellite reading; tap a colored buoy pin for live temp, wind, and waves. This is usually about a day behind live buoys—not minute-by-minute. Use Map / Chart for a free NOAA ENC chart under the thermal overlay (not for navigation). In Chart mode, drag the ⠿ handle to move the opacity card, then slide Depths ↔ Temp to balance the layers.",
            )

            HelpSection(
                title = "🌬️ Conditions",
                body = "A list of live buoy readings by Great Lake. Sort by nearest, temperature, wind, or waves. Each card shows how fresh the reading is (e.g. “40 min ago”). Turn on location to put closest buoys first. Tap 📈 24h temperature on a buoy for a sparkline of the last day’s water temp. Star favorites to keep them at the top. Tap the card to jump to that buoy on the map.",
            )

            HelpSection(
                title = "🎣 Fishing",
                body = "Michigan and Wisconsin DNR fishing reports, sorted closest-first when location is on. Each spot can show the nearest live buoy (temp, wind, waves). Open a spot and tap Show on chart to jump to Hybrid with the NOAA chart centered on that location. Star spots you care about—they stay at the top across weeks. Swipe down on Conditions or Fishing to refresh.",
            )

            HelpSection(
                title = "🔔 Alerts",
                body = "Optional local notifications for a temp band on a favorite buoy, calm wind, and new Michigan / Wisconsin DNR fishing weeks. Turn them on from the bell icon on Conditions or Fishing. Checks run about every 2 hours when online — Android may delay them to save battery.",
            )
            if (onOpenAlerts != null) {
                TextButton(onClick = onOpenAlerts) {
                    Text("Open Alerts →", color = SplashCyan)
                }
            }

            HelpSection(
                title = "Favorites & refresh",
                body = "Stars save on this device only. Use the refresh icon, or swipe down on Conditions and Fishing, to force a fresh download of buoy, satellite, or DNR data.",
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Disclaimer",
                color = SplashGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            HelpSection(
                title = "Not for navigation or safety decisions",
                body = "LakePulse is an informational tool for recreation and planning. Do not rely on it for navigation, search and rescue, marine forecasting, or any life-safety decision. Always check official weather, marine, and local advisories before going on the water.",
            )
            HelpSection(
                title = "Data may be delayed or incomplete",
                body = "Buoy readings come from NOAA NDBC and can lag, drop out, or miss sensors. Satellite GLSEA is a daily analysis, not a live thermometer. State DNR fishing reports are summaries that may omit spots or change after posting. LakePulse does not guarantee accuracy, completeness, or timeliness.",
            )
            HelpSection(
                title = "Third-party sources",
                body = "Content is provided by NOAA, NDBC, NOAA GLERL / CoastWatch (GLSEA), NOAA Office of Coast Survey (ENC chart display), and Michigan / Wisconsin DNR. Those agencies own their data. LakePulse is not affiliated with or endorsed by them. Chart layers are for situational awareness only and must not be used for navigation.",
            )
            HelpSection(
                title = "Limitation of liability",
                body = "Use at your own risk. To the fullest extent allowed by law, LakePulse and its creators are not liable for any loss, injury, or damage arising from use of the app or reliance on its information.",
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Sources: NOAA NDBC · NOAA GLSEA · Michigan DNR · Wisconsin DNR",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
