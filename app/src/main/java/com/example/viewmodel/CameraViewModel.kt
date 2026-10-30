package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.data.CapturedPhotoEntity
import com.example.engine.CapCutFilter
import com.example.engine.CapCutFilterEngine
import com.example.engine.FusionProcessingEngine
import com.example.engine.FusionProgress
import com.example.engine.HapticSoundManager
import com.example.engine.MediaStorageHelper
import com.example.engine.SensorTelemetryManager
import com.example.engine.TelemetryData
import com.example.engine.VideoProcessingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class CameraMode(val title: String, val badge: String, val lensDesc: String) {
    FUSION_100("10X /HDREAL", "10X FUSION", "64MP + /HDREAL 3.0 Multi-Frame"),
    POCO_64MP("64MP Ultra HD", "64MP FULL SENSOR", "64MP OIS (9248x6936)"),
    ULTRA_WIDE_06X("8MP Ultra-Wide", "0.6X 118° FOV", "8MP Ultra-Wide Angle"),
    MACRO_2MP("2MP Macro", "4cm CLOSE-UP", "2MP Fixed 4cm Focus"),
    MOON_MODE("Moon Astro", "30X ASTRO", "AI Lunar Crater Super-Res"),
    VIDEO_4K("4K 10X Video", "4K 60FPS", "MediaCodec AVC + CapCut")
}

data class CameraUiState(
    val currentZoom: Float = 1.0f,
    val activeMode: CameraMode = CameraMode.FUSION_100,
    val isTripodLockEnabled: Boolean = true,
    val isSaveRawEnabled: Boolean = true,
    val isSilentSpyMode: Boolean = false,
    val isSniperAutoFireEnabled: Boolean = false,
    val isPipOverviewVisible: Boolean = true,
    val isProcessingFusion: Boolean = false,
    val fusionProgress: FusionProgress = FusionProgress(0, 100, "Initializing", 0f),
    val lastCapturedPhoto: CapturedPhotoEntity? = null,
    val isRecordingVideo: Boolean = false,
    val videoRecordingSeconds: Int = 0,
    val simulationSubjectIndex: Int = 0, // 0 = Distant Eagle (10X), 1 = Moon Crater, 2 = Macro Dewdrop, 3 = UltraWide Landscape
    val showHdRealPreviewModal: Boolean = false,
    val lastEnhancedBitmap: Bitmap? = null,
    val lastRawBitmap: Bitmap? = null,
    val hdRealIntensity: Float = 1.5f,
    val selectedCapCutFilter: CapCutFilter = CapCutFilter.CINEMATIC_TEAL,
    val capCutFilterIntensity: Float = 1.0f,
    val showFilterSelector: Boolean = false,
    val lastSavedVideoMessage: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val photoDao = db.photoDao()

    val telemetryManager = SensorTelemetryManager(application)
    val hapticSoundManager = HapticSoundManager(application)

    val telemetry: StateFlow<TelemetryData> = telemetryManager.telemetry

    val allPhotos = photoDao.getAllPhotos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var videoJob: Job? = null
    private var autoFireCooldown = false
    @Volatile
    private var isRecordingActive = false

    init {
        telemetryManager.startListening()
        telemetryManager.updateZoom(1.0f)

        // Monitor telemetry for Sniper Auto-Fire
        viewModelScope.launch {
            telemetry.collect { tele ->
                if (_uiState.value.isSniperAutoFireEnabled &&
                    _uiState.value.currentZoom >= 5.0f &&
                    tele.isSniperSteady &&
                    !_uiState.value.isProcessingFusion &&
                    !autoFireCooldown
                ) {
                    autoFireCooldown = true
                    hapticSoundManager.onSniperLocked()
                    triggerFusionCapture(null)
                    delay(3000)
                    autoFireCooldown = false
                }
            }
        }
    }

    fun setZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0.6f, 30.0f)
        val updatedSubject = when {
            _uiState.value.activeMode == CameraMode.MACRO_2MP -> 2
            _uiState.value.activeMode == CameraMode.MOON_MODE -> 1
            clamped <= 0.65f -> 3 // POCO X6 8MP Ultra-Wide Landscape
            _uiState.value.simulationSubjectIndex == 3 -> 0 // Switch back from ultra-wide
            else -> _uiState.value.simulationSubjectIndex
        }
        val updatedMode = if (clamped <= 0.65f && _uiState.value.activeMode != CameraMode.VIDEO_4K) {
            CameraMode.ULTRA_WIDE_06X
        } else if (_uiState.value.activeMode == CameraMode.ULTRA_WIDE_06X && clamped > 0.65f) {
            CameraMode.FUSION_100
        } else {
            _uiState.value.activeMode
        }

        _uiState.value = _uiState.value.copy(
            currentZoom = clamped,
            simulationSubjectIndex = updatedSubject,
            activeMode = updatedMode
        )
        telemetryManager.updateZoom(clamped)
    }

    fun quickSwitch1X10X() {
        val target = if (_uiState.value.currentZoom >= 8.0f) 1.0f else 10.0f
        setZoom(target)
        hapticSoundManager.onZoomKeyMilestoneReached()
    }

    fun setMode(mode: CameraMode) {
        val targetZoom = when (mode) {
            CameraMode.ULTRA_WIDE_06X -> 0.6f
            CameraMode.MACRO_2MP -> 1.0f
            CameraMode.POCO_64MP -> 1.0f
            CameraMode.FUSION_100 -> if (_uiState.value.currentZoom < 4.5f) 10.0f else _uiState.value.currentZoom
            CameraMode.MOON_MODE -> if (_uiState.value.currentZoom < 10.0f) 15.0f else _uiState.value.currentZoom
            CameraMode.VIDEO_4K -> _uiState.value.currentZoom
        }
        val targetSubject = when (mode) {
            CameraMode.MACRO_2MP -> 2 // POCO X6 2MP Macro (4cm)
            CameraMode.ULTRA_WIDE_06X -> 3 // POCO X6 8MP Ultra-Wide (118°)
            CameraMode.MOON_MODE -> 1 // 30X Moon Crater
            else -> 0 // 64MP Main (10X Telephoto / ISZ)
        }
        _uiState.value = _uiState.value.copy(
            activeMode = mode,
            currentZoom = targetZoom,
            simulationSubjectIndex = targetSubject
        )
        telemetryManager.updateZoom(targetZoom)
        hapticSoundManager.onZoomStepChanged()
    }

    fun selectMacroMode() {
        setMode(CameraMode.MACRO_2MP)
    }

    fun selectUltraWideMode() {
        setMode(CameraMode.ULTRA_WIDE_06X)
    }

    fun select64MpMode() {
        setMode(CameraMode.POCO_64MP)
    }

    fun setCapCutFilter(filter: CapCutFilter) {
        _uiState.value = _uiState.value.copy(selectedCapCutFilter = filter)
        hapticSoundManager.onZoomStepChanged()
    }

    fun setCapCutFilterIntensity(intensity: Float) {
        _uiState.value = _uiState.value.copy(capCutFilterIntensity = intensity.coerceIn(0.1f, 1.0f))
    }

    fun toggleFilterSelector() {
        _uiState.value = _uiState.value.copy(showFilterSelector = !_uiState.value.showFilterSelector)
        hapticSoundManager.onZoomStepChanged()
    }

    fun toggleTripodLock() {
        val newStatus = !_uiState.value.isTripodLockEnabled
        _uiState.value = _uiState.value.copy(isTripodLockEnabled = newStatus)
        hapticSoundManager.onZoomStepChanged()
    }

    fun toggleSaveRaw() {
        _uiState.value = _uiState.value.copy(isSaveRawEnabled = !_uiState.value.isSaveRawEnabled)
        hapticSoundManager.onZoomStepChanged()
    }

    fun toggleSilentSpyMode() {
        val newStatus = !_uiState.value.isSilentSpyMode
        _uiState.value = _uiState.value.copy(isSilentSpyMode = newStatus)
        hapticSoundManager.isSilentSpyMode = newStatus
        hapticSoundManager.onZoomStepChanged()
    }

    fun toggleSniperAutoFire() {
        _uiState.value = _uiState.value.copy(isSniperAutoFireEnabled = !_uiState.value.isSniperAutoFireEnabled)
        hapticSoundManager.onZoomStepChanged()
    }

    fun togglePipOverview() {
        _uiState.value = _uiState.value.copy(isPipOverviewVisible = !_uiState.value.isPipOverviewVisible)
    }

    fun toggleSimulationSubject() {
        // Cycle: 0 (10X Eagle) -> 1 (30X Moon) -> 2 (2MP Macro 4cm) -> 3 (8MP Ultra-Wide 118°)
        val next = (_uiState.value.simulationSubjectIndex + 1) % 4
        val associatedMode = when (next) {
            1 -> CameraMode.MOON_MODE
            2 -> CameraMode.MACRO_2MP
            3 -> CameraMode.ULTRA_WIDE_06X
            else -> CameraMode.FUSION_100
        }
        val targetZoom = when (next) {
            1 -> 15.0f
            2 -> 1.0f
            3 -> 0.6f
            else -> 10.0f
        }
        _uiState.value = _uiState.value.copy(
            simulationSubjectIndex = next,
            activeMode = associatedMode,
            currentZoom = targetZoom
        )
        telemetryManager.updateZoom(targetZoom)
        hapticSoundManager.onZoomStepChanged()
    }

    fun closeHdRealPreview() {
        _uiState.value = _uiState.value.copy(showHdRealPreviewModal = false)
    }

    fun clearSavedVideoMessage() {
        _uiState.value = _uiState.value.copy(lastSavedVideoMessage = null)
    }

    /**
     * Toggles 4K 10X Video Recording with the active CapCut Filter.
     * Records real MP4 video with frame-by-frame filter and saves to vault.
     */
    fun toggleVideoRecording(sourceFrameSupplier: (() -> Bitmap)? = null) {
        val isRecording = _uiState.value.isRecordingVideo
        if (isRecording) {
            // Stop recording
            isRecordingActive = false
            hapticSoundManager.playShutterEffect()
        } else {
            // Start recording
            isRecordingActive = true
            hapticSoundManager.playShutterEffect()
            _uiState.value = _uiState.value.copy(
                isRecordingVideo = true,
                videoRecordingSeconds = 0,
                lastSavedVideoMessage = null
            )

            val context = getApplication<Application>()
            val currentFilter = _uiState.value.selectedCapCutFilter
            val filterIntensity = _uiState.value.capCutFilterIntensity
            val zoom = _uiState.value.currentZoom

            val frameSupplier = sourceFrameSupplier ?: {
                val resId = when {
                    _uiState.value.activeMode == CameraMode.MACRO_2MP || _uiState.value.simulationSubjectIndex == 2 -> R.drawable.sample_macro
                    _uiState.value.activeMode == CameraMode.ULTRA_WIDE_06X || _uiState.value.simulationSubjectIndex == 3 || _uiState.value.currentZoom <= 0.65f -> R.drawable.sample_ultrawide
                    _uiState.value.activeMode == CameraMode.MOON_MODE || _uiState.value.simulationSubjectIndex == 1 -> R.drawable.sample_moon
                    else -> R.drawable.sample_eagle
                }
                BitmapFactory.decodeResource(context.resources, resId)
            }

            videoJob = viewModelScope.launch(Dispatchers.IO) {
                // Record video with VideoProcessingEngine
                val videoPath = VideoProcessingEngine.recordFilteredVideoClip(
                    context = context,
                    baseFrameSupplier = frameSupplier,
                    filter = currentFilter,
                    filterIntensity = filterIntensity,
                    zoomLevel = zoom,
                    isRecordingPredicate = { isRecordingActive },
                    onProgressUpdate = { sec ->
                        _uiState.value = _uiState.value.copy(videoRecordingSeconds = sec)
                    }
                )

                // Export video directly to device public MediaStore gallery (DCIM/UltraZoom8K)
                val videoFile = File(videoPath)
                MediaStorageHelper.exportVideoToGallery(
                    context = context,
                    sourceVideoFile = videoFile,
                    filenamePrefix = "video_${currentFilter.id}",
                    showToast = true
                )

                // Capture thumbnail
                val base = frameSupplier()
                val filteredThumb = CapCutFilterEngine.applyFilter(base, currentFilter, filterIntensity)
                val thumbPath = FusionProcessingEngine.saveBitmapToFile(
                    context = context,
                    bitmap = filteredThumb,
                    prefix = "video_thumb",
                    exportToGallery = false
                )

                val duration = _uiState.value.videoRecordingSeconds.coerceAtLeast(1)
                val tele = telemetry.value
                val modeDesc = when (_uiState.value.activeMode) {
                    CameraMode.MACRO_2MP -> "POCO X6 2MP Macro Video"
                    CameraMode.ULTRA_WIDE_06X -> "POCO X6 8MP Ultra-Wide Video"
                    CameraMode.POCO_64MP -> "POCO X6 64MP 4K Video"
                    else -> "4K 10X Video"
                }

                val videoEntity = CapturedPhotoEntity(
                    enhancedImagePath = thumbPath,
                    rawImagePath = thumbPath,
                    zoomLevel = zoom,
                    estimatedDistance = tele.distanceDisplay,
                    fusionFramesCount = duration * 24,
                    captureMode = "$modeDesc (${currentFilter.displayName})",
                    stabilityScore = tele.stabilityScore,
                    hasDehaze = false,
                    isMoonMode = _uiState.value.activeMode == CameraMode.MOON_MODE,
                    title = "$modeDesc • ${currentFilter.displayName}",
                    filterName = currentFilter.displayName,
                    mediaType = "VIDEO",
                    videoPath = videoPath,
                    videoDurationSeconds = duration
                )

                val id = photoDao.insertPhoto(videoEntity)
                val savedEntity = videoEntity.copy(id = id)

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isRecordingVideo = false,
                        lastCapturedPhoto = savedEntity,
                        lastSavedVideoMessage = "व्हिडिओ गॅलरीमध्ये सेव्ह झाला! (DCIM/UltraZoom8K)"
                    )
                }
            }
        }
    }

    /**
     * Triggers 100-Frame Fusion Photo Capture with CapCut Filter + /HDREAL Remaster.
     */
    fun triggerFusionCapture(sourceBitmap: Bitmap? = null) {
        if (_uiState.value.isProcessingFusion) return

        hapticSoundManager.playShutterEffect()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingFusion = true)

            val context = getApplication<Application>()
            val baseBitmap: Bitmap = sourceBitmap ?: run {
                val resId = when {
                    _uiState.value.activeMode == CameraMode.MACRO_2MP || _uiState.value.simulationSubjectIndex == 2 -> R.drawable.sample_macro
                    _uiState.value.activeMode == CameraMode.ULTRA_WIDE_06X || _uiState.value.simulationSubjectIndex == 3 || _uiState.value.currentZoom <= 0.65f -> R.drawable.sample_ultrawide
                    _uiState.value.activeMode == CameraMode.MOON_MODE || _uiState.value.simulationSubjectIndex == 1 -> R.drawable.sample_moon
                    else -> R.drawable.sample_eagle
                }
                BitmapFactory.decodeResource(context.resources, resId)
            }

            val zoom = _uiState.value.currentZoom
            val isMoon = _uiState.value.activeMode == CameraMode.MOON_MODE
            val activeFilter = _uiState.value.selectedCapCutFilter
            val filterIntensity = _uiState.value.capCutFilterIntensity

            val (rawBmp, enhancedBmp) = FusionProcessingEngine.process100FrameFusion(
                context = context,
                baseBitmap = baseBitmap,
                zoomFactor = zoom,
                isMoonMode = isMoon,
                isDehaze = false,
                capCutFilter = activeFilter,
                filterIntensity = filterIntensity
            ) { progress ->
                _uiState.value = _uiState.value.copy(fusionProgress = progress)
            }

            // Save Enhanced Image with filter info
            val enhancedPath = FusionProcessingEngine.saveBitmapToFile(
                context,
                enhancedBmp,
                "hdreal_${activeFilter.id}_${zoom.toInt()}x"
            )

            // Save RAW telephoto crop
            val rawPath = if (_uiState.value.isSaveRawEnabled) {
                FusionProcessingEngine.saveBitmapToFile(context, rawBmp, "raw_telephoto_${zoom.toInt()}x")
            } else null

            val currentTele = telemetry.value
            val filterSuffix = if (activeFilter != CapCutFilter.ORIGINAL) " • ${activeFilter.displayName}" else ""
            val currentMode = _uiState.value.activeMode
            val titleName = when {
                currentMode == CameraMode.MACRO_2MP -> "POCO X6 2MP Macro (4cm)$filterSuffix"
                currentMode == CameraMode.ULTRA_WIDE_06X -> "POCO X6 8MP Ultra-Wide 118°$filterSuffix"
                currentMode == CameraMode.POCO_64MP -> "POCO X6 64MP Ultra HD (9248x6936)$filterSuffix"
                zoom >= 10f -> "/HDREAL 10X Ultra Clear$filterSuffix"
                zoom >= 5f -> "/HDREAL 5X Clear$filterSuffix"
                currentMode == CameraMode.MOON_MODE -> "10X Moon Astro$filterSuffix"
                else -> "/HDREAL Shot$filterSuffix"
            }

            val entity = CapturedPhotoEntity(
                enhancedImagePath = enhancedPath,
                rawImagePath = rawPath,
                zoomLevel = zoom,
                estimatedDistance = currentTele.distanceDisplay,
                fusionFramesCount = 100,
                captureMode = if (zoom >= 4.5f) "/HDREAL + ${activeFilter.displayName}" else _uiState.value.activeMode.title,
                stabilityScore = currentTele.stabilityScore,
                hasDehaze = false,
                isMoonMode = isMoon,
                title = titleName,
                filterName = activeFilter.displayName,
                mediaType = "IMAGE"
            )

            val insertedId = photoDao.insertPhoto(entity)
            val savedEntity = entity.copy(id = insertedId)

            _uiState.value = _uiState.value.copy(
                isProcessingFusion = false,
                lastCapturedPhoto = savedEntity,
                lastEnhancedBitmap = enhancedBmp,
                lastRawBitmap = rawBmp,
                showHdRealPreviewModal = (zoom >= 4.5f)
            )
        }
    }

    /**
     * Applies a CapCut Filter to an existing photo in the gallery and saves it as a new version.
     */
    fun applyCapCutFilterToExistingPhoto(
        photo: CapturedPhotoEntity,
        filter: CapCutFilter,
        intensity: Float = 1.0f
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val sourceFile = File(photo.enhancedImagePath)
            if (!sourceFile.exists()) return@launch

            val srcBmp = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return@launch
            val filteredBmp = CapCutFilterEngine.applyFilter(srcBmp, filter, intensity)

            val newPath = FusionProcessingEngine.saveBitmapToFile(context, filteredBmp, "capcut_${filter.id}")
            val updated = photo.copy(
                id = 0, // Auto-generate new primary key
                timestamp = System.currentTimeMillis(),
                enhancedImagePath = newPath,
                filterName = filter.displayName,
                title = "${photo.title} (${filter.displayName})"
            )
            photoDao.insertPhoto(updated)
        }
    }

    fun applyUltraMaxRemaster(photo: CapturedPhotoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val sourceFile = photo.rawImagePath?.let { File(it) } ?: File(photo.enhancedImagePath)
            if (!sourceFile.exists()) return@launch

            val srcBmp = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return@launch
            val ultraRemastered = FusionProcessingEngine.applyHDRealClarity(
                source = srcBmp,
                zoomFactor = photo.zoomLevel,
                isMoonMode = photo.isMoonMode,
                isDehaze = photo.hasDehaze,
                intensity = 2.2f
            )

            val newPath = FusionProcessingEngine.saveBitmapToFile(context, ultraRemastered, "hdreal_max_clear")
            val updated = photo.copy(
                enhancedImagePath = newPath,
                title = "/HDREAL 8K Max Clear (${photo.zoomLevel.toInt()}X)"
            )
            photoDao.insertPhoto(updated)
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            photoDao.deleteById(photoId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryManager.stopListening()
        hapticSoundManager.release()
        isRecordingActive = false
        videoJob?.cancel()
    }
}
