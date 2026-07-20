package com.lakepulse.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lakepulse.ui.theme.DeepWater
import com.lakepulse.ui.theme.Shore

@Composable
fun MapTopChrome(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean = true,
    onOpenHelp: (() -> Unit)? = null,
    topTrailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepWater.copy(alpha = 0.98f),
                        DeepWater.copy(alpha = 0.88f),
                        Shore.copy(alpha = 0.55f),
                        Color.Transparent,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.94f),
                    fontWeight = FontWeight.Medium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (topTrailing != null) {
                    topTrailing()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenHelp != null) {
                        IconButton(onClick = onOpenHelp) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Help",
                                tint = Color.White,
                            )
                        }
                    }
                    IconButton(onClick = onRefresh, enabled = refreshEnabled) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapLegendCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepWater.copy(alpha = 0.96f),
                        DeepWater.copy(alpha = 0.94f),
                        Shore.copy(alpha = 0.92f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun MapBottomScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        DeepWater.copy(alpha = 0.35f),
                        DeepWater.copy(alpha = 0.72f),
                    ),
                ),
            ),
    )
}
