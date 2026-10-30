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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
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

data class PocoLensPreset(
    val zoom: Float,
    val label: String,
    val subLabel: String,
    val isMacro: Boolean = false
)

@Composable
fun ZoomDialSlider(
    zoomLevel: Float,
    isMacroActive: Boolean = false,
    onZoomChanged: (Float) -> Unit,
    onMacroClicked: () -> Unit = {},
    onQuickSwitch: () -> Unit,
    onStepHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tailored for POCO X6 Camera Hardware Setup (64MP Main + 8MP UW + 2MP Macro)
    val presets = listOf(
        PocoLensPreset(0.6f, "0.6X", "8MP UW"),
        PocoLensPreset(1.0f, "1X", "64MP"),
        PocoLensPreset(2.0f, "2X", "ISZ"),
        PocoLensPreset(5.0f, "5X", "AI"),
        PocoLensPreset(10.0f, "10X", "/HDREAL"),
        PocoLensPreset(1.0f, "4cm", "MACRO", isMacro = true)
    )

    Column(
        modifier = modifier
            .testTag("zoom_dial_slider_container")
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // POCO X6 Lens Presets & 1X <-> 10X Quick Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Presets row
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { preset ->
                    val isSelected = if (preset.isMacro) {
                        isMacroActive
                    } else {
                        !isMacroActive && (zoomLevel - preset.zoom).let { it >= -0.25f && it <= 0.25f }
                    }

                    val isSuperZoom = preset.zoom >= 10.0f
                    val isMacro = preset.isMacro

                    Box(
                        modifier = Modifier
                            .testTag(if (preset.isMacro) "lens_preset_macro" else "zoom_preset_${preset.zoom}x")
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> if (isMacro) NeonGreen else if (isSuperZoom) CyberGold else NeonCyan
                                    else -> DarkGlassSurface.copy(alpha = 0.85f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else DarkGlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (preset.isMacro) {
                                    onMacroClicked()
                                } else {
                                    onZoomChanged(preset.zoom)
                                }
                                onStepHaptic()
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = preset.label,
                                color = when {
                                    isSelected -> ObsidianBlack
                                    isMacro -> NeonGreen
                                    isSuperZoom -> CyberGold
                                    else -> Color.White
                                },
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = preset.subLabel,
                                color = when {
                                    isSelected -> ObsidianBlack.copy(alpha = 0.8f)
                                    else -> Color.White.copy(alpha = 0.55f)
                                },
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Quick 1X <-> 10X Instant Switch Button
            Box(
                modifier = Modifier
                    .testTag("quick_switch_1x_10x")
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkGlassCard)
                    .border(1.dp, CyberGold.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .clickable {
                        onQuickSwitch()
                        onStepHaptic()
                    }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
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
                        text = if (zoomLevel >= 8f) "1X 64M" else "10X HD",
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "0.6X",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.5.sp,
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
                valueRange = 0.6f..30.0f,
                colors = SliderDefaults.colors(
                    thumbColor = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                    activeTrackColor = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                    inactiveTrackColor = DarkGlassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("continuous_zoom_slider")
            )

            // Current magnification & Sensor description badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkGlassSurface)
                    .border(1.dp, if (zoomLevel >= 10f) CyberGold else NeonCyan, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = String.format("%.1fX", zoomLevel),
                    color = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
