package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
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
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import kotlin.math.roundToInt

@Composable
fun ZoomDialSlider(
    zoomLevel: Float,
    onZoomChanged: (Float) -> Unit,
    onQuickSwitch: () -> Unit,
    onStepHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f, 30.0f)

    Column(
        modifier = modifier
            .testTag("zoom_dial_slider_container")
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Zoom Preset Buttons & 1X <-> 10X Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Presets row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { preset ->
                    val isSelected = (zoomLevel - preset).let { it >= -0.3f && it <= 0.3f }
                    val isSuperZoom = preset >= 10.0f

                    Box(
                        modifier = Modifier
                            .testTag("zoom_preset_${preset.toInt()}x")
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> if (isSuperZoom) CyberGold else NeonCyan
                                    else -> DarkGlassSurface.copy(alpha = 0.85f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else DarkGlassBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                onZoomChanged(preset)
                                onStepHaptic()
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (preset == 0.5f) "0.5X" else "${preset.toInt()}X",
                            color = when {
                                isSelected -> ObsidianBlack
                                isSuperZoom -> CyberGold
                                else -> Color.White
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick 1X <-> 10X Instant Switch Button
            Box(
                modifier = Modifier
                    .testTag("quick_switch_1x_10x")
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkGlassCard)
                    .border(1.dp, CyberGold.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .clickable {
                        onQuickSwitch()
                        onStepHaptic()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Quick Switch 1X to 10X",
                        tint = CyberGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (zoomLevel >= 8f) "JUMP 1X" else "10X ZOOM",
                        color = CyberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Continuous Slider with Live Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "0.5X",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Slider(
                value = zoomLevel,
                onValueChange = { newVal ->
                    val rounded = (newVal * 10f).roundToInt() / 10f
                    if ((rounded * 10).toInt() % 10 == 0) {
                        onStepHaptic()
                    }
                    onZoomChanged(rounded)
                },
                valueRange = 0.5f..30.0f,
                colors = SliderDefaults.colors(
                    thumbColor = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                    activeTrackColor = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                    inactiveTrackColor = DarkGlassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("continuous_zoom_slider")
            )

            // Current magnification badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkGlassSurface)
                    .border(1.dp, if (zoomLevel >= 10f) CyberGold else NeonCyan, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = String.format("%.1fX", zoomLevel),
                    color = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
