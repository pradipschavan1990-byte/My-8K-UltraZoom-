package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.data.CapturedPhotoEntity
import com.example.engine.FusionProcessingEngine
import com.example.engine.FusionProgress
import com.example.engine.HapticSoundManager
import com.example.engine.SensorTelemetryManager
import com.example.engine.TelemetryData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CameraMode(val title: String) {
    FUSION_100("100-Frame Fusion"),
    MOON_MODE("Moon & Sun Mode"),
    DE_HAZE("De-Haze Clear"),
    VIDEO_4K("4K 10X Video")
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
    val simulationSubjectIndex: Int = 0 // 0 = Distant Eagle, 1 = Moon Crater
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
        val clamped = zoom.coerceIn(0.5f, 30.0f)
        _uiState.value = _uiState.value.copy(currentZoom = clamped)
        telemetryManager.updateZoom(clamped)
    }

    fun quickSwitch1X10X() {
        val target = if (_uiState.value.currentZoom >= 8.0f) 1.0f else 10.0f
        setZoom(target)
        hapticSoundManager.onZoomKeyMilestoneReached()
    }

    fun setMode(mode: CameraMode) {
        _uiState.value = _uiState.value.copy(
            activeMode = mode,
            simulationSubjectIndex = if (mode == CameraMode.MOON_MODE) 1 else 0
        )
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
        val next = (_uiState.value.simulationSubjectIndex + 1) % 2
        _uiState.value = _uiState.value.copy(simulationSubjectIndex = next)
    }

    fun toggleVideoRecording() {
        val isRecording = _uiState.value.isRecordingVideo
        if (isRecording) {
            videoJob?.cancel()
            _uiState.value = _uiState.value.copy(isRecordingVideo = false)
            hapticSoundManager.playShutterEffect()
        } else {
            _uiState.value = _uiState.value.copy(isRecordingVideo = true, videoRecordingSeconds = 0)
            hapticSoundManager.playShutterEffect()
            videoJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _uiState.value = _uiState.value.copy(
                        videoRecordingSeconds = _uiState.value.videoRecordingSeconds + 1
                    )
                }
            }
        }
    }

    fun triggerFusionCapture(sourceBitmap: Bitmap? = null) {
        if (_uiState.value.isProcessingFusion) return

        hapticSoundManager.playShutterEffect()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingFusion = true)

            val context = getApplication<Application>()
            val baseBitmap: Bitmap = sourceBitmap ?: run {
                // If no real camera frame passed, use high-res telephoto sample
                val resId = if (_uiState.value.activeMode == CameraMode.MOON_MODE || _uiState.value.simulationSubjectIndex == 1) {
                    R.drawable.sample_moon
                } else {
                    R.drawable.sample_eagle
                }
                BitmapFactory.decodeResource(context.resources, resId)
            }

            val isMoon = _uiState.value.activeMode == CameraMode.MOON_MODE
            val isDehaze = _uiState.value.activeMode == CameraMode.DE_HAZE

            val (rawBmp, enhancedBmp) = FusionProcessingEngine.process100FrameFusion(
                context = context,
                baseBitmap = baseBitmap,
                zoomFactor = _uiState.value.currentZoom,
                isMoonMode = isMoon,
                isDehaze = isDehaze
            ) { progress ->
                _uiState.value = _uiState.value.copy(fusionProgress = progress)
            }

            // Save Enhanced Image
            val enhancedPath = FusionProcessingEngine.saveBitmapToFile(context, enhancedBmp, "enhanced_100x")

            // Save RAW if enabled
            val rawPath = if (_uiState.value.isSaveRawEnabled) {
                FusionProcessingEngine.saveBitmapToFile(context, rawBmp, "raw_sensor")
            } else null

            val currentTele = telemetry.value
            val entity = CapturedPhotoEntity(
                enhancedImagePath = enhancedPath,
                rawImagePath = rawPath,
                zoomLevel = _uiState.value.currentZoom,
                estimatedDistance = currentTele.distanceDisplay,
                fusionFramesCount = 100,
                captureMode = _uiState.value.activeMode.title,
                stabilityScore = currentTele.stabilityScore,
                hasDehaze = isDehaze,
                isMoonMode = isMoon,
                title = if (isMoon) "10X Moon Astro Shot" else "10X UltraZoom Fusion"
            )

            val insertedId = photoDao.insertPhoto(entity)
            val savedEntity = entity.copy(id = insertedId)

            _uiState.value = _uiState.value.copy(
                isProcessingFusion = false,
                lastCapturedPhoto = savedEntity
            )
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
        videoJob?.cancel()
    }
}
