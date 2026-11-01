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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.camera.core.FocusMeteringAction
import kotlinx.coroutines.delay
import com.example.ui.components.CameraSettingsDialog
import com.example.ui.components.TelemetryModalDialog
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
    var maxZoomRatio by remember { mutableStateOf(10f) }
    var minZoomRatio by remember { mutableStateOf(0.6f) }

    // Minimalist Pop-up Window States
    var showFilterDrawer by remember { mutableStateOf(false) }
    var showTelemetryModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }

    // Visual Tap-to-Focus Reticle & AI Tripod Lock Target
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            isFocusing = true
            delay(2400)
            isFocusing = false
        }
    }

    DisposableEffect(activePreviewView) {
        viewModel.currentFrameProvider = { activePreviewView?.bitmap }
        onDispose {
            viewModel.currentFrameProvider = null
        }
    }

    LaunchedEffect(uiState.currentZoom, boundCameraControl, maxZoomRatio, minZoomRatio) {
        boundCameraControl?.let { control ->
            try {
                control.setZoomRatio(uiState.currentZoom.coerceIn(minZoomRatio, maxZoomRatio))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Pro Settings Pop-up Modal
    if (showSettingsModal) {
        CameraSettingsDialog(
            isTripodLockEnabled = uiState.isTripodLockEnabled,
            onToggleTripodLock = { viewModel.toggleTripodLock() },
            isSaveRawEnabled = uiState.isSaveRawEnabled,
            onToggleSaveRaw = { viewModel.toggleSaveRaw() },
            isSilentSpyMode = uiState.isSilentSpyMode,
            onToggleSilentSpyMode = { viewModel.toggleSilentSpyMode() },
            isPipVisible = uiState.isPipOverviewVisible,
            onTogglePip = { viewModel.togglePipOverview() },
            onOpenPhotoRestore = onNavigateToRestore,
            onDismiss = { showSettingsModal = false }
        )
    }

    // Pro Telemetry & LiDAR Radar Pop-up Modal
    if (showTelemetryModal) {
        TelemetryModalDialog(
            telemetry = telemetry,
            zoomLevel = uiState.currentZoom,
            isTripodLockEnabled = uiState.isTripodLockEnabled,
            activeMode = uiState.activeMode,
            onDismiss = { showTelemetryModal = false }
        )
    }

    Box(
        modifier = Modifier
            .testTag("camera_screen_root")
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // 1. Live Camera Viewfinder or Simulation Engine
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            focusPoint = tapOffset
                            try {
                                val factory = activePreviewView?.meteringPointFactory
                                val point = factory?.createPoint(tapOffset.x, tapOffset.y)
                                if (point != null) {
                                    val action = FocusMeteringAction.Builder(point).build()
                                    boundCameraControl?.startFocusAndMetering(action)
                                }
                                viewModel.hapticSoundManager.onZoomStepChanged()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
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

                                val zState = camera.cameraInfo.zoomState.value
                                val maxZ = zState?.maxZoomRatio ?: 10f
                                val minZ = zState?.minZoomRatio ?: 0.6f
                                maxZoomRatio = maxZ
                                minZoomRatio = minZ
                                camera.cameraControl.setZoomRatio(uiState.currentZoom.coerceIn(minZ, maxZ))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    update = { pView ->
                        activePreviewView = pView
                        try {
                            boundCameraControl?.setZoomRatio(uiState.currentZoom.coerceIn(minZoomRatio, maxZoomRatio))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
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
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            focusPoint = tapOffset
                            viewModel.hapticSoundManager.onZoomStepChanged()
                        }
                    }
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

        // Visual Tap-to-Focus Reticle & AI Tripod Lock Instant Feedback
        if (isFocusing && focusPoint != null) {
            val point = focusPoint!!
            Box(
                modifier = Modifier
                    .offset { IntOffset(point.x.roundToInt() - 40.dp.roundToPx(), point.y.roundToInt() - 40.dp.roundToPx()) }
                    .size(80.dp)
                    .border(1.5.dp, CyberGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(CyberGold, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ObsidianBlack.copy(alpha = 0.85f))
                        .border(0.5.dp, CyberGold.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.isTripodLockEnabled) "AI TRIPOD LOCKED" else "AF / AE LOCK",
                        color = CyberGold,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
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

        // 3. Ultra-Clean Top HUD (Minimalist, Transparent Gradient with statusBarsPadding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBlack.copy(alpha = 0.90f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Device & Active Lens Mode (Clickable for Pro Settings)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSettingsModal = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "POCO X6 5G",
                    color = CyberGold,
                    fontSize = 11.sp,
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

            // Center: Minimalist Clickable Telemetry Pop-up Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkGlassSurface.copy(alpha = 0.92f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .clickable { showTelemetryModal = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("open_telemetry_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isSniperSteady) NeonGreen else CyberGold)
                    )
                    Text(
                        text = "${telemetry.distanceDisplay} • ${telemetry.stabilityScore}% STABLE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Quick Pop-up Launchers (Filters ✨ & Settings ⚙️)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter Wand Pop-up Trigger
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL || showFilterDrawer)
                                CyberGold.copy(alpha = 0.35f)
                            else DarkGlassCard
                        )
                        .border(
                            1.dp,
                            if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) CyberGold else DarkGlassBorder,
                            CircleShape
                        )
                        .clickable { showFilterDrawer = !showFilterDrawer }
                        .testTag("top_filter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "CapCut Filters",
                        tint = if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL) CyberGold else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Pro Settings Dialog Trigger
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkGlassCard)
                        .border(1.dp, DarkGlassBorder, CircleShape)
                        .clickable { showSettingsModal = true }
                        .testTag("top_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Pro Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. Picture-in-Picture Mini Radar Window (only when active)
        if (uiState.isPipOverviewVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 62.dp, end = 12.dp)
            ) {
                DualZoomPipWindow(
                    isVisible = true,
                    currentZoom = uiState.currentZoom,
                    isMoonMode = uiState.activeMode == CameraMode.MOON_MODE,
                    onCloseClick = { viewModel.togglePipOverview() }
                )
            }
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

        // 6. Clean Bottom Control Deck
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(ObsidianBlack.copy(alpha = 0.94f))
                .padding(bottom = 16.dp, top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Zoom Dial Slider
            ZoomDialSlider(
                zoomLevel = uiState.currentZoom,
                isMacroActive = (uiState.activeMode == CameraMode.MACRO_2MP),
                onZoomChanged = { viewModel.setZoom(it) },
                onMacroClicked = { viewModel.selectMacroMode() },
                onQuickSwitch = { viewModel.quickSwitch1X10X() },
                onStepHaptic = { viewModel.hapticSoundManager.onZoomStepChanged() }
            )

            // Camera Modes Tab Carousel
            val modes = CameraMode.entries
            val isVideoMode = (uiState.activeMode == CameraMode.VIDEO_4K)
            ScrollableTabRow(
                selectedTabIndex = modes.indexOf(uiState.activeMode),
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    val idx = modes.indexOf(uiState.activeMode)
                    if (idx in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                            color = if (isVideoMode) WarningRed else CyberGold,
                            height = 2.dp
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
                                color = if (isSelected) {
                                    if (mode == CameraMode.VIDEO_4K) WarningRed else CyberGold
                                } else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
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

        // 7. CapCut Filter Pop-up Drawer (Translucent glass, floats on top above the slim zoom slider)
        AnimatedVisibility(
            visible = showFilterDrawer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 168.dp)
                .fillMaxWidth()
        ) {
            CapCutFilterBar(
                selectedFilter = uiState.selectedCapCutFilter,
                intensity = uiState.capCutFilterIntensity,
                onFilterSelected = { viewModel.setCapCutFilter(it) },
                onIntensityChanged = { viewModel.setCapCutFilterIntensity(it) },
                onClose = { showFilterDrawer = false }
            )
        }

        // Active Filter indicator chip when filter drawer is closed (Clickable to re-open drawer)
        if (uiState.selectedCapCutFilter != CapCutFilter.ORIGINAL && !showFilterDrawer) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 172.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .border(1.dp, CyberGold.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .clickable { showFilterDrawer = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("active_filter_indicator_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Active Filter",
                        tint = CyberGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${uiState.selectedCapCutFilter.displayName} (${(uiState.capCutFilterIntensity * 100).roundToInt()}%)",
                        color = CyberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• TAP TO CHANGE",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 8. Computational 100-Frame Fusion Processing Dialog
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
