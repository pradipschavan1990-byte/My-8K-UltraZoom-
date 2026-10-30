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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Filter
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import com.example.engine.CapCutFilter
import com.example.ui.components.CapCutFilterBar
import com.example.ui.components.DistanceMeterOverlay
import com.example.ui.components.DualZoomPipWindow
import com.example.ui.components.HdRealResultSheet
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
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    var boundCameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(
        modifier = Modifier
            .testTag("camera_screen_root")
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // 1. Live Camera Viewfinder or Simulation Engine
        if (hasCameraPermission) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        activePreviewView = previewView
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
                                boundCameraControl = camera.cameraControl
                                camera.cameraControl.setZoomRatio(uiState.currentZoom.coerceIn(1f, 8f))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    update = { pView ->
                        activePreviewView = pView
                        boundCameraControl?.setZoomRatio(uiState.currentZoom.coerceIn(1f, 8f))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("camera_preview_view")
                )

                // Live CapCut Filter tint overlay on real camera preview
                if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) {
                    val tintColor = when (uiState.selectedCapCutFilter) {
                        CapCutFilter.CINEMATIC_TEAL -> Color(0xFF00E5FF).copy(alpha = 0.09f)
                        CapCutFilter.GLOW_FACE -> Color(0xFFFFE0B2).copy(alpha = 0.12f)
                        CapCutFilter.CYBERPUNK -> Color(0xFFE040FB).copy(alpha = 0.11f)
                        CapCutFilter.GOLDEN_HOUR -> Color(0xFFFFB300).copy(alpha = 0.13f)
                        CapCutFilter.VINTAGE_35MM -> Color(0xFFFFD54F).copy(alpha = 0.10f)
                        CapCutFilter.ANIME_PASTEL -> Color(0xFF80D8FF).copy(alpha = 0.09f)
                        CapCutFilter.SUNSET_PEACH -> Color(0xFFFF80AB).copy(alpha = 0.11f)
                        CapCutFilter.MOODY_NOIR -> Color(0xFF000000).copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(tintColor)
                    )
                }
            }
        } else {
            // High-Res Telephoto Simulator Scene
            val sampleResId = when {
                uiState.activeMode == CameraMode.MACRO_2MP || uiState.simulationSubjectIndex == 2 -> R.drawable.sample_macro
                uiState.activeMode == CameraMode.ULTRA_WIDE_06X || uiState.simulationSubjectIndex == 3 || uiState.currentZoom <= 0.65f -> R.drawable.sample_ultrawide
                uiState.activeMode == CameraMode.MOON_MODE || uiState.simulationSubjectIndex == 1 -> R.drawable.sample_moon
                else -> R.drawable.sample_eagle
            }

            val zoomScale = (1.0f + (uiState.currentZoom - 1.0f) * 0.18f).coerceIn(1.0f, 6.0f)
            val driftX = if (uiState.isTripodLockEnabled) telemetry.driftOffsetX else 0f
            val driftY = if (uiState.isTripodLockEnabled) telemetry.driftOffsetY else 0f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(driftX.roundToInt(), driftY.roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                // Live CapCut Filter applied directly to simulation viewfinder!
                val colorFilter = if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) {
                    ColorFilter.colorMatrix(ColorMatrix(uiState.selectedCapCutFilter.colorMatrixValues))
                } else null

                Image(
                    painter = painterResource(id = sampleResId),
                    contentDescription = "Ultra Zoom Telephoto Subject",
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(zoomScale)
                        .testTag("telephoto_simulation_image")
                )
            }
        }

        // 2. Sniper Stabilizer Ring (Active when Zoom >= 5X)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.activeMode == CameraMode.MACRO_2MP) {
                // POCO X6 2MP Macro 4cm Focus Target Reticle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(2.dp, NeonGreen, CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .border(1.5.dp, NeonGreen, CircleShape)
                        )
                    }
                    Text(
                        text = "4cm MACRO FOCUS LOCKED",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                SniperStabilizerRing(
                    stabilityScore = telemetry.stabilityScore,
                    isSniperSteady = telemetry.isSniperSteady,
                    driftOffsetX = telemetry.driftOffsetX,
                    driftOffsetY = telemetry.driftOffsetY,
                    zoomLevel = uiState.currentZoom
                )
            }
        }

        // 3. Top HUD: Telemetry & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(ObsidianBlack.copy(alpha = 0.88f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // POCO X6 Hardware Indicator Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POCO X6 5G • TRIPLE CAM",
                    color = CyberGold,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = uiState.activeMode.lensDesc,
                    color = when (uiState.activeMode) {
                        CameraMode.MACRO_2MP -> NeonGreen
                        CameraMode.ULTRA_WIDE_06X -> NeonCyan
                        CameraMode.POCO_64MP -> CyberGold
                        else -> Color.White.copy(alpha = 0.8f)
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Mode Tabs (10X /HDREAL | 64MP | 8MP UW | 2MP MACRO | MOON | 4K VIDEO)
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

                // Active CapCut Filter indicator pill
                Box(
                    modifier = Modifier
                        .testTag("toggle_filter_pill")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) CyberGold.copy(alpha = 0.25f) else DarkGlassCard)
                        .border(
                            1.dp,
                            if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) CyberGold else DarkGlassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.toggleFilterSelector() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "CapCut Filter",
                            tint = if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) CyberGold else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.selectedCapCutFilter.displayName.uppercase(),
                            color = if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) CyberGold else Color.White.copy(alpha = 0.8f),
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
                        .background(if (uiState.isSaveRawEnabled) NeonGreen.copy(alpha = 0.2f) else DarkGlassCard)
                        .border(
                            1.dp,
                            if (uiState.isSaveRawEnabled) NeonGreen else DarkGlassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.toggleSaveRaw() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (uiState.isSaveRawEnabled) "RAW+AI ON" else "RAW OFF",
                        color = if (uiState.isSaveRawEnabled) NeonGreen else Color.White.copy(alpha = 0.5f),
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
            DistanceMeterOverlay(
                telemetry = telemetry,
                zoomLevel = uiState.currentZoom,
                isTripodLockEnabled = uiState.isTripodLockEnabled,
                activeMode = uiState.activeMode
            )

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
                val subjectLabel = when (uiState.simulationSubjectIndex) {
                    1 -> "TARGET: MOON (30X)"
                    2 -> "TARGET: 4cm MACRO"
                    3 -> "TARGET: 118° ULTRA-WIDE"
                    else -> "TARGET: 10X EAGLE"
                }
                Text(
                    text = subjectLabel,
                    color = CyberGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Video Saved Confirmation Banner
        if (uiState.lastSavedVideoMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 175.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeonGreen.copy(alpha = 0.95f))
                    .clickable {
                        viewModel.clearSavedVideoMessage()
                        onNavigateToGallery()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.lastSavedVideoMessage} • TAP TO VIEW",
                        color = ObsidianBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 5. Bottom Control Deck (CapCut Filter Carousel + Zoom Slider + Shutter + Vault)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(ObsidianBlack.copy(alpha = 0.94f))
                .padding(bottom = 16.dp, top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // CapCut Viral Filters Horizontal Carousel
            CapCutFilterBar(
                selectedFilter = uiState.selectedCapCutFilter,
                intensity = uiState.capCutFilterIntensity,
                onFilterSelected = { viewModel.setCapCutFilter(it) },
                onIntensityChanged = { viewModel.setCapCutFilterIntensity(it) }
            )

            // Zoom Dial Slider
            ZoomDialSlider(
                zoomLevel = uiState.currentZoom,
                isMacroActive = (uiState.activeMode == CameraMode.MACRO_2MP),
                onZoomChanged = { viewModel.setZoom(it) },
                onMacroClicked = { viewModel.selectMacroMode() },
                onQuickSwitch = { viewModel.quickSwitch1X10X() },
                onStepHaptic = { viewModel.hapticSoundManager.onZoomStepChanged() }
            )

            // Mode Switcher Bar: PHOTO vs VIDEO
            val isVideoMode = (uiState.activeMode == CameraMode.VIDEO_4K)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PHOTO Tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isVideoMode) CyberGold.copy(alpha = 0.28f) else DarkGlassSurface.copy(alpha = 0.6f))
                        .border(
                            1.5.dp,
                            if (!isVideoMode) CyberGold else DarkGlassBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            if (isVideoMode) {
                                viewModel.setMode(CameraMode.FUSION_100)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 7.dp)
                        .testTag("mode_switch_photo"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Photo Mode",
                            tint = if (!isVideoMode) CyberGold else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PHOTO",
                            color = if (!isVideoMode) CyberGold else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // VIDEO Tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isVideoMode) WarningRed.copy(alpha = 0.32f) else DarkGlassSurface.copy(alpha = 0.6f))
                        .border(
                            1.5.dp,
                            if (isVideoMode) WarningRed else DarkGlassBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            if (!isVideoMode) {
                                viewModel.setMode(CameraMode.VIDEO_4K)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 7.dp)
                        .testTag("mode_switch_video"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Mode",
                            tint = if (isVideoMode) WarningRed else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "VIDEO",
                            color = if (isVideoMode) WarningRed else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Video Recording Timer & HUD readout if recording is active
            if (uiState.isRecordingVideo) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(WarningRed.copy(alpha = 0.9f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "REC " + String.format(
                            "%02d:%02d",
                            uiState.videoRecordingSeconds / 60,
                            uiState.videoRecordingSeconds % 60
                        ) + " • 4K 10X (${uiState.selectedCapCutFilter.displayName})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

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
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkGlassCard)
                        .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable { onNavigateToGallery() }
                        .testTag("camera_gallery_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "8K Gallery Vault",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "GALLERY",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Center: Big Shutter Button
                if (isVideoMode) {
                    // 4K Video Shutter Button (Vibrant Red)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(
                                3.5.dp,
                                if (uiState.isRecordingVideo) WarningRed else Color.White,
                                CircleShape
                            )
                            .clickable {
                                val frameSupplier: (() -> Bitmap)? = activePreviewView?.let { pv ->
                                    {
                                        pv.bitmap ?: BitmapFactory.decodeResource(
                                            context.resources,
                                            when {
                                                uiState.simulationSubjectIndex == 2 -> R.drawable.sample_macro
                                                uiState.simulationSubjectIndex == 3 || uiState.currentZoom <= 0.65f -> R.drawable.sample_ultrawide
                                                uiState.simulationSubjectIndex == 1 -> R.drawable.sample_moon
                                                else -> R.drawable.sample_eagle
                                            }
                                        )
                                    }
                                }
                                viewModel.toggleVideoRecording(frameSupplier)
                            }
                            .testTag("video_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isRecordingVideo) {
                            // Stop Recording Square
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarningRed)
                            )
                        } else {
                            // Start Recording Red Circle with Video Icon
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(WarningRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Start Video Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                } else {
                    // 100-Frame Fusion Photo Shutter Button (CyberGold)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(
                                3.5.dp,
                                if (telemetry.isSniperSteady) NeonGreen else CyberGold,
                                CircleShape
                            )
                            .clickable {
                                // Capture real camera frame from active preview if available
                                val liveBmp = activePreviewView?.bitmap
                                viewModel.triggerFusionCapture(liveBmp)
                            }
                            .testTag("photo_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(CyberGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture 10X Photo",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Right: Dedicated Quick Video Record / PiP Toggle Button
                if (!isVideoMode) {
                    // Quick Video Record Shortcut Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkGlassCard)
                            .border(1.dp, WarningRed.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.setMode(CameraMode.VIDEO_4K)
                            }
                            .testTag("quick_video_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Switch to Video",
                                tint = WarningRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "REC 4K",
                                color = WarningRed,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    // Switch back to Photo Mode Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkGlassCard)
                            .border(1.dp, CyberGold.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.setMode(CameraMode.FUSION_100)
                            }
                            .testTag("switch_to_photo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Switch to Photo",
                                tint = CyberGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "PHOTO",
                                color = CyberGold,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 6. Computational 100-Frame Fusion Processing Dialog
        if (uiState.isProcessingFusion) {
            FusionProcessingDialog(progress = uiState.fusionProgress)
        }

        // 7. /HDREAL 3.0 Instant Before/After Remaster Result Sheet
        if (uiState.showHdRealPreviewModal && uiState.lastRawBitmap != null && uiState.lastEnhancedBitmap != null) {
            HdRealResultSheet(
                rawBitmap = uiState.lastRawBitmap!!,
                enhancedBitmap = uiState.lastEnhancedBitmap!!,
                zoomLevel = uiState.currentZoom,
                onDismiss = { viewModel.closeHdRealPreview() },
                onOpenGallery = {
                    viewModel.closeHdRealPreview()
                    onNavigateToGallery()
                }
            )
        }
    }
}
