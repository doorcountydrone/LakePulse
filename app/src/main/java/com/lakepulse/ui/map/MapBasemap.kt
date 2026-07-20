package com.lakepulse.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class MapBasemap(val jsMode: String, val label: String) {
    Street("map", "Map"),
    Chart("chart", "Chart"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapBasemapToggle(
    selected: MapBasemap,
    onSelected: (MapBasemap) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MapBasemap.entries.forEach { mode ->
            val selectedChip = mode == selected
            FilterChip(
                selected = selectedChip,
                onClick = { onSelected(mode) },
                label = {
                    Text(
                        text = mode.label,
                        color = Color.Black,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.88f),
                    labelColor = Color.Black,
                    selectedContainerColor = Color.White,
                    selectedLabelColor = Color.Black,
                ),
            )
        }
    }
}
