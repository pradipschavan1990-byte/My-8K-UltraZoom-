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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.filled.Tune
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
                modifier = Modifier.fillMaxWidth(),
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
                    // 1. Tripod Gimbal Lock
                    SettingsToggleItem(
                        icon = Icons.Default.Lock,
                        title = "Tripod Gimbal Lock",
                        subtitle = "Gyro stabilization anti-shake engine",
                        isChecked = isTripodLockEnabled,
                        onCheckedChange = { onToggleTripodLock() },
                        activeColor = NeonCyan,
                        testTag = "setting_tripod_lock"
                    )

                    // 2. RAW + AI Mode
                    SettingsToggleItem(
                        icon = Icons.Default.RawOn,
                        title = "Save RAW + AI Image",
                        subtitle = "Saves uncompressed telephoto alongside /HDREAL",
                        isChecked = isSaveRawEnabled,
                        onCheckedChange = { onToggleSaveRaw() },
                        activeColor = NeonGreen,
                        testTag = "setting_save_raw"
                    )

                    // 3. Silent Spy Mode
                    SettingsToggleItem(
                        icon = if (isSilentSpyMode) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        title = "Silent Spy Shutter",
                        subtitle = "Mute capture & zoom haptic clicks",
                        isChecked = isSilentSpyMode,
                        onCheckedChange = { onToggleSilentSpyMode() },
                        activeColor = CyberGold,
                        testTag = "setting_silent_mode"
                    )

                    // 4. Picture-in-Picture Overview
                    SettingsToggleItem(
                        icon = Icons.Default.PictureInPicture,
                        title = "Dual-Zoom PiP Window",
                        subtitle = "Mini 1X radar window on screen",
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
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGlassCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .testTag("settings_open_restore_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "AI Photo Restore",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "AI Photo Restore Tool",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "ChatGPT /HDREAL Old Photo Remaster",
                                    color = NeonCyan.copy(alpha = 0.85f),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "OPEN →",
                            color = CyberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("settings_done_button")
                ) {
                    Text(
                        text = "DONE",
                        color = ObsidianBlack,
                        fontSize = 12.sp,
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
            .background(DarkGlassSurface)
            .border(1.dp, if (isChecked) activeColor.copy(alpha = 0.6f) else DarkGlassBorder, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isChecked) activeColor else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = DarkGlassCard
            )
        )
    }
}
