package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.DistanceMeterOverlay
import com.example.ui.components.DualZoomPipWindow
import com.example.ui.components.SniperStabilizerRing
import com.example.ui.components.ZoomDialSlider
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.WarningRed
import com.example.viewmodel.CameraMode
import com.example.viewmodel.CameraViewModel
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToGallery: () -> Unit,
    onNavigateToRestore: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .testTag("camera_screen_root")
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // 1. Live Camera Viewfinder or Simulation Engine
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            // Set zoom ratio on camera control
                            camera.cameraControl.setZoomRatio(uiState.currentZoom.coerceIn(1f, 8f))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view")
            )
        } else {
            // High-Res Telephoto Simulator Scene
            val sampleResId = if (uiState.activeMode == CameraMode.MOON_MODE || uiState.simulationSubjectIndex == 1) {
                R.drawable.sample_moon
            } else {
                R.drawable.sample_eagle
            }

            // Real-time Zoom scaling with AI Tripod Gimbal Offset
            val zoomScale = (1.0f + (uiState.currentZoom - 1.0f) * 0.18f).coerceIn(1.0f, 6.0f)
            val driftX = if (uiState.isTripodLockEnabled) telemetry.driftOffsetX else 0f
            val driftY = if (uiState.isTripodLockEnabled) telemetry.driftOffsetY else 0f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(driftX.roundToInt(), driftY.roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = sampleResId),
                    contentDescription = "Ultra Zoom Telephoto Subject",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(zoomScale)
                        .testTag("telephoto_simulation_image")
                )
            }
        }

        // Viewfinder Grid Overlay lines
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 180.dp, top = 120.dp)
        )

        // 2. Sniper Stabilizer Ring (Active when Zoom >= 5X)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            SniperStabilizerRing(
                stabilityScore = telemetry.stabilityScore,
                isSniperSteady = telemetry.isSniperSteady,
                driftOffsetX = telemetry.driftOffsetX,
                driftOffsetY = telemetry.driftOffsetY,
                zoomLevel = uiState.currentZoom
            )
        }

        // 3. Top HUD: Telemetry & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(ObsidianBlack.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mode Tabs (100-FRAME FUSION | MOON MODE | DE-HAZE | 4K VIDEO)
            val modes = CameraMode.entries
            ScrollableTabRow(
                selectedTabIndex = modes.indexOf(uiState.activeMode),
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                edgePadding = 4.dp,
                indicator = { tabPositions ->
                    val idx = modes.indexOf(uiState.activeMode)
                    if (idx in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                            color = CyberGold,
                            height = 2.5.dp
                        )
                    }
                },
                divider = {}
            ) {
                modes.forEach { mode ->
                    val isSelected = uiState.activeMode == mode
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setMode(mode) },
                        modifier = Modifier.testTag("tab_mode_${mode.name.lowercase()}"),
                        text = {
                            Text(
                                text = mode.title.uppercase(),
                                color = if (isSelected) CyberGold else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            // Quick Toggle Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tripod Lock Toggle
                Box(
                    modifier = Modifier
                        .testTag("toggle_tripod_lock")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (uiState.isTripodLockEnabled) NeonCyan.copy(alpha = 0.2f) else DarkGlassCard)
                        .border(
                            1.dp,
                            if (uiState.isTripodLockEnabled) NeonCyan else DarkGlassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.toggleTripodLock() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Tripod Lock",
                            tint = if (uiState.isTripodLockEnabled) NeonCyan else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TRIPOD LOCK",
                            color = if (uiState.isTripodLockEnabled) NeonCyan else Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // RAW + AI Toggle
                Box(
                    modifier = Modifier
                        .testTag("toggle_save_raw")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (uiState.isSaveRawEnabled) CyberGold.copy(alpha = 0.2f) else DarkGlassCard)
                        .border(
                            1.dp,
                            if (uiState.isSaveRawEnabled) CyberGold else DarkGlassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.toggleSaveRaw() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (uiState.isSaveRawEnabled) "RAW + AI [ON]" else "RAW [OFF]",
                        color = if (uiState.isSaveRawEnabled) CyberGold else Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Auto-Fire Sniper Toggle
                Box(
                    modifier = Modifier
                        .testTag("toggle_auto_fire")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (uiState.isSniperAutoFireEnabled) NeonGreen.copy(alpha = 0.2f) else DarkGlassCard)
                        .border(
                            1.dp,
                            if (uiState.isSniperAutoFireEnabled) NeonGreen else DarkGlassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.toggleSniperAutoFire() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (uiState.isSniperAutoFireEnabled) "AUTO-FIRE ON" else "AUTO-FIRE",
                        color = if (uiState.isSniperAutoFireEnabled) NeonGreen else Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Silent Spy Mode Toggle
                IconButton(
                    onClick = { viewModel.toggleSilentSpyMode() },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("toggle_spy_mode")
                ) {
                    Icon(
                        imageVector = if (uiState.isSilentSpyMode) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Spy / Silent Mode",
                        tint = if (uiState.isSilentSpyMode) CyberGold else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Old Photo Restore Tool Button
                IconButton(
                    onClick = onNavigateToRestore,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("open_photo_restore")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Old Photo Restore Tool",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 4. Overlays: Distance Meter & Dual Zoom PiP Window
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 110.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Distance Depth Meter Overlay
            DistanceMeterOverlay(
                telemetry = telemetry,
                zoomLevel = uiState.currentZoom,
                isTripodLockEnabled = uiState.isTripodLockEnabled
            )

            // Dual Zoom PiP Window
            DualZoomPipWindow(
                isVisible = uiState.isPipOverviewVisible,
                currentZoom = uiState.currentZoom,
                isMoonMode = uiState.activeMode == CameraMode.MOON_MODE,
                onCloseClick = { viewModel.togglePipOverview() }
            )
        }

        // Switch simulator subject button when in emulator mode
        if (!hasCameraPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkGlassSurface.copy(alpha = 0.85f))
                    .border(1.dp, DarkGlassBorder, RoundedCornerShape(8.dp))
                    .clickable { viewModel.toggleSimulationSubject() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("switch_subject_button")
            ) {
                Text(
                    text = if (uiState.simulationSubjectIndex == 0) "TARGET: EAGLE (10X)" else "TARGET: MOON (30X)",
                    color = CyberGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 5. Bottom Control Deck (Zoom Slider + Shutter + Vault)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(ObsidianBlack.copy(alpha = 0.92f))
                .padding(bottom = 20.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom Slider with Vibration Haptic Feedbacks & Quick switch
            ZoomDialSlider(
                zoomLevel = uiState.currentZoom,
                onZoomChanged = { viewModel.setZoom(it) },
                onQuickSwitch = { viewModel.quickSwitch1X10X() },
                onStepHaptic = { viewModel.hapticSoundManager.onZoomStepChanged() }
            )

            // Shutter Deck Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 8K Vault / Gallery Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGlassCard)
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToGallery() }
                        .testTag("camera_gallery_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "8K Gallery Vault",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center: Big Shutter Button
                if (uiState.activeMode == CameraMode.VIDEO_4K) {
                    // 4K Video Shutter Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, if (uiState.isRecordingVideo) WarningRed else Color.White, CircleShape)
                            .clickable { viewModel.toggleVideoRecording() }
                            .testTag("video_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (uiState.isRecordingVideo) 28.dp else 56.dp)
                                .clip(if (uiState.isRecordingVideo) RoundedCornerShape(6.dp) else CircleShape)
                                .background(WarningRed)
                        )
                    }
                } else {
                    // 100-Frame Fusion Photo Shutter Button
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .border(
                                3.dp,
                                if (telemetry.isSniperSteady) NeonGreen else CyberGold,
                                CircleShape
                            )
                            .clickable { viewModel.triggerFusionCapture(null) }
                            .testTag("photo_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(CyberGold)
                        )
                    }
                }

                // Right: Subject / PiP Toggle Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGlassCard)
                        .border(1.dp, DarkGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.togglePipOverview() }
                        .testTag("camera_pip_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Toggle PiP Dual Zoom Window",
                        tint = CyberGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Video Recording Timer Readout if recording
            if (uiState.isRecordingVideo) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WarningRed)
                    )
                    Text(
                        text = "4K 10X REC  " + String.format(
                            "%02d:%02d",
                            uiState.videoRecordingSeconds / 60,
                            uiState.videoRecordingSeconds % 60
                        ) + " • AI GYRO STABILIZED",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 6. Computational 100-Frame Fusion Processing Dialog
        if (uiState.isProcessingFusion) {
            FusionProcessingDialog(progress = uiState.fusionProgress)
        }
    }
}
