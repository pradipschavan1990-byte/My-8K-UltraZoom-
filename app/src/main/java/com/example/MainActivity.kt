package com.example

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraControl: CameraControl? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { if (it[Manifest.permission.CAMERA] == true) startApp() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startApp()
        else permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun startApp() {
        setContent {
            var zoom by remember { mutableFloatStateOf(1f) }
            var isVideo by remember { mutableStateOf(false) }
            var isRecording by remember { mutableStateOf(false) }

            MaterialTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    CameraPreview(
                        isVideoMode = isVideo,
                        onUseCase = { control, imgCap, vidCap ->
                            cameraControl = control
                            imageCapture = imgCap
                            videoCapture = vidCap
                        }
                    )
                    
                    // Top Bar
                    Text("8K UltraZoom - ${zoom.toInt()}x", color = Color.White, modifier = Modifier.padding(16.dp).background(Color.Black.copy(0.5f)).padding(8.dp))

                    // Controls
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(0.7f)).padding(12.dp)) {
                        // Zoom Slider - JAD disnar
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1x", color = Color.White)
                            Slider(value = zoom, onValueChange = { zoom = it; cameraControl?.setZoomRatio(it) }, valueRange = 1f..100f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Yellow))
                            Text("100x", color = Color.White)
                        }
                        
                        Spacer(Modifier.height(10.dp))
                        
                        // Buttons Row
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { isVideo = !isVideo }) { Text(if(isVideo) "PHOTO" else "VIDEO") }
                            
                            Button(
                                onClick = { if(isVideo) { if(isRecording) stopVideo() else startVideo { isRecording = it }; isRecording = !isRecording } else takePhoto() },
                                shape = CircleShape,
                                modifier = Modifier.size(80.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if(isRecording) Color.Red else Color.White)
                            ) { Text(if(isVideo) "REC" else "CAPTURE", color = Color.Black) }
                            
                            Button(onClick = { Toast.makeText(this@MainActivity, "Gallery: DCIM/UltraZoom", Toast.LENGTH_SHORT).show() }) { Text("Gallery") }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CameraPreview(isVideoMode: Boolean, onUseCase: (CameraControl, ImageCapture, VideoCapture<Recorder>) -> Unit) {
        val context = LocalContext.current
        AndroidView(factory = { ctx ->
            val previewView = androidx.camera.view.PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val imgCap = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                    val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
                    val vidCap = VideoCapture.withOutput(recorder)
                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imgCap, vidCap)
                    onUseCase(camera.cameraControl, imgCap, vidCap)
                } catch (e: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }, modifier = Modifier.fillMaxSize())
    }

    private fun takePhoto() {
        val cap = imageCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "8K_${name}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/UltraZoom")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val out = uri?.let { ImageCapture.OutputFileOptions.Builder(contentResolver, it, values).build() } ?: return
        cap.takePicture(out, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(e: ImageCaptureException) { runOnUiThread { Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show() } }
            override fun onImageSaved(r: ImageCapture.OutputFileResults) { runOnUiThread { Toast.makeText(this@MainActivity, "Photo Saved! Gallery madhe bagh", Toast.LENGTH_LONG).show() } }
        })
    }

    private fun startVideo(onStart: (Boolean) -> Unit) {
        val cap = videoCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "8K_${name}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/UltraZoom")
        }
        val output = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()
        recording = cap.output.prepareRecording(this, output).apply { if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) withAudioEnabled() }.start(cameraExecutor) {
            if (it is VideoRecordEvent.Finalize) runOnUiThread { Toast.makeText(this, if(it.hasError()) "Video Fail" else "Video Saved!", Toast.LENGTH_SHORT).show() }
        }
    }
    private fun stopVideo() { recording?.stop(); recording = null }
    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }
}
