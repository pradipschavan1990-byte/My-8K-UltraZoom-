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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack

@Composable
fun CameraSettingsDialog(
    isTripodLockEnabled: Boolean,
    onToggleTripodLock: () -> Unit,
    isSaveRawEnabled: Boolean,
    onToggleSaveRaw: () -> Unit,
    isSilentSpyMode: Boolean,
    onToggleSilentSpyMode: () -> Unit,
    isPipVisible: Boolean,
    onTogglePip: () -> Unit,
    isGridEnabled: Boolean = true,
    onToggleGrid: () -> Unit = {},
    isHorizonLevelerEnabled: Boolean = true,
    onToggleHorizonLeveler: () -> Unit = {},
    isHapticZoomEnabled: Boolean = true,
    onToggleHapticZoom: () -> Unit = {},
    onOpenPhotoRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .testTag("camera_settings_dialog")
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ObsidianBlack.copy(alpha = 0.96f))
                .border(1.5.dp, CyberGold.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CyberGold)
                        )
                        Column {
                            Text(
                                text = "PRO CAMERA SETTINGS",
                                color = CyberGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "POCO X6 5G Hardware & Optics",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_settings_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Settings List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. AI Gyro Auto-Stabilizer (Tripod Gimbal Lock)
                    SettingsToggleItem(
                        icon = Icons.Default.Lock,
                        title = "AI Auto-Stabilization (Gimbal Lock)",
                        subtitle = "Gyro anti-shake stabilization at 5X-30X zoom",
                        isChecked = isTripodLockEnabled,
                        onCheckedChange = { onToggleTripodLock() },
                        activeColor = NeonGreen,
                        testTag = "setting_tripod_lock"
                    )

                    // 2. 3x3 Composition Grid (Rule of Thirds)
                    SettingsToggleItem(
                        icon = Icons.Default.GridOn,
                        title = "3×3 Composition Grid Overlay",
                        subtitle = "Professional Rule-of-Thirds framing guides",
                        isChecked = isGridEnabled,
                        onCheckedChange = { onToggleGrid() },
                        activeColor = CyberGold,
                        testTag = "setting_grid_overlay"
                    )

                    // 3. Virtual Horizon Spirit Level
                    SettingsToggleItem(
                        icon = Icons.Default.Explore,
                        title = "Horizon Spirit Level (0° Leveler)",
                        subtitle = "Real-time tilt gauge for perfectly straight photos",
                        isChecked = isHorizonLevelerEnabled,
                        onCheckedChange = { onToggleHorizonLeveler() },
                        activeColor = NeonCyan,
                        testTag = "setting_horizon_leveler"
                    )

                    // 4. Haptic Zoom Feedback
                    SettingsToggleItem(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Zoom Ticks",
                        subtitle = "Tactile vibration ticks at 1X, 2X, 5X, 10X milestones",
                        isChecked = isHapticZoomEnabled,
                        onCheckedChange = { onToggleHapticZoom() },
                        activeColor = CyberGold,
                        testTag = "setting_haptic_zoom"
                    )

                    // 5. RAW + AI Mode
                    SettingsToggleItem(
                        icon = Icons.Default.RawOn,
                        title = "Save RAW + AI Image",
                        subtitle = "Saves uncompressed telephoto alongside /HDREAL",
                        isChecked = isSaveRawEnabled,
                        onCheckedChange = { onToggleSaveRaw() },
                        activeColor = NeonCyan,
                        testTag = "setting_save_raw"
                    )

                    // 6. Silent Spy Mode
                    SettingsToggleItem(
                        icon = if (isSilentSpyMode) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        title = "Silent Spy Shutter",
                        subtitle = "Mute capture & zoom audio clicks",
                        isChecked = isSilentSpyMode,
                        onCheckedChange = { onToggleSilentSpyMode() },
                        activeColor = CyberGold,
                        testTag = "setting_silent_mode"
                    )

                    // 7. Picture-in-Picture Overview
                    SettingsToggleItem(
                        icon = Icons.Default.PictureInPicture,
                        title = "Dual-Zoom PiP Window",
                        subtitle = "Mini 1X radar overview window on screen",
                        isChecked = isPipVisible,
                        onCheckedChange = { onTogglePip() },
                        activeColor = NeonCyan,
                        testTag = "setting_pip_window"
                    )
                }

                // AI Old Photo Restore Tool Shortcut Button
                Button(
                    onClick = {
                        onDismiss()
                        onOpenPhotoRestore()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("open_photo_restore_from_settings"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGold,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "AI Restore",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI OLD PHOTO RESTORE TOOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGlassCard.copy(alpha = 0.5f))
            .border(1.dp, if (isChecked) activeColor.copy(alpha = 0.4f) else DarkGlassBorder, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isChecked) activeColor.copy(alpha = 0.2f) else DarkGlassSurface)
                    .border(1.dp, if (isChecked) activeColor.copy(alpha = 0.6f) else DarkGlassBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isChecked) activeColor else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeColor,
                checkedTrackColor = activeColor.copy(alpha = 0.35f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = DarkGlassBorder
            ),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
