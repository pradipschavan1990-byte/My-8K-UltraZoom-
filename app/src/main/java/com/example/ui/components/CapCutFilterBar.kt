package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CapCutFilter
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton

@Composable
fun CapCutFilterBar(
    selectedFilter: CapCutFilter,
    intensity: Float,
    onFilterSelected: (CapCutFilter) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val filters = CapCutFilter.entries

    Column(
        modifier = modifier
            .testTag("capcut_filter_bar")
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(ObsidianBlack.copy(alpha = 0.95f))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Filter Carousel Title & Intensity Slider Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "CapCut Filters",
                    tint = CyberGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "CAPCUT VIRAL FILTERS",
                    color = CyberGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedFilter != CapCutFilter.ORIGINAL) {
                    Text(
                        text = "INTENSITY ${(intensity * 100).roundToInt()}%",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("close_capcut_filter_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Filters",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Intensity Slider when non-original filter selected
        if (selectedFilter != CapCutFilter.ORIGINAL) {
            Slider(
                value = intensity,
                onValueChange = onIntensityChanged,
                valueRange = 0.2f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberGold,
                    activeTrackColor = CyberGold,
                    inactiveTrackColor = DarkGlassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 14.dp)
                    .testTag("filter_intensity_slider")
            )
        }

        // Horizontal Carousel of Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters, key = { it.id }) { filter ->
                val isSelected = filter == selectedFilter

                Box(
                    modifier = Modifier
                        .testTag("filter_chip_${filter.id}")
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> DarkGlassCard
                                else -> DarkGlassSurface.copy(alpha = 0.8f)
                            }
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> CyberGold
                                else -> DarkGlassBorder
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Badge pill (e.g. VIRAL, TRENDING)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (filter.badge) {
                                            "VIRAL" -> Color(0xFFFF3D00).copy(alpha = 0.85f)
                                            "TRENDING" -> CyberGold.copy(alpha = 0.85f)
                                            "BEAUTY" -> Color(0xFFFF4081).copy(alpha = 0.85f)
                                            "RETRO" -> Color(0xFF7C4DFF).copy(alpha = 0.85f)
                                            else -> DarkGlassBorder
                                        }
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = filter.badge,
                                    color = Color.White,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = filter.displayName,
                                color = if (isSelected) CyberGold else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Marathi title
                        Text(
                            text = filter.marathiName,
                            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
