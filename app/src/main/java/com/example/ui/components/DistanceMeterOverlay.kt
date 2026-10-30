package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
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
import com.example.engine.TelemetryData
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack

import com.example.viewmodel.CameraMode

@Composable
fun DistanceMeterOverlay(
    telemetry: TelemetryData,
    zoomLevel: Float,
    isTripodLockEnabled: Boolean,
    activeMode: CameraMode = CameraMode.FUSION_100,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("distance_meter_overlay")
            .clip(RoundedCornerShape(8.dp))
            .background(DarkGlassSurface.copy(alpha = 0.82f))
            .border(1.dp, DarkGlassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (activeMode) {
            CameraMode.MACRO_2MP -> {
                // POCO X6 2MP Macro telemetry
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FOCUS: ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "4.0 cm",
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(POCO X6 2MP MACRO)",
                        color = NeonGreen.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Macro Focus Lock",
                        tint = NeonGreen,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "OPTIMAL FOCUS DISTANCE 4cm LOCKED",
                        color = NeonGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            CameraMode.ULTRA_WIDE_06X -> {
                // POCO X6 8MP Ultra-Wide telemetry
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FOV: ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "118° WIDE",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(POCO X6 8MP UW)",
                        color = NeonCyan.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "EXPANSIVE HORIZON ARCHITECTURE MODE",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            CameraMode.POCO_64MP -> {
                // POCO X6 64MP Full Sensor telemetry
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberGold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SENSOR: ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "64MP OIS",
                        color = CyberGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(9248 x 6936 RAW)",
                        color = CyberGold.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "FULL PIXEL ARRAY DIRECT READOUT",
                    color = CyberGold.copy(alpha = 0.9f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            else -> {
                // Distance Readout
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEPTH: ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = telemetry.distanceDisplay,
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${telemetry.laserConfidence}% AI CONF)",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Tripod Lock Status
                if (isTripodLockEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "AI Tripod Lock",
                            tint = if (telemetry.isTripodLocked) NeonGreen else CyberGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (telemetry.isTripodLocked) "AI TRIPOD LOCKED" else "GIMBAL STABILIZING...",
                            color = if (telemetry.isTripodLocked) NeonGreen else CyberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // /HDREAL 3.0 Telephoto Clarity Status
                if (zoomLevel >= 4.5f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberGold)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/HDREAL 3.0: 5X/10X CLEAR HD ACTIVE",
                            color = CyberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
