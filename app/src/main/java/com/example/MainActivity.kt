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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var cameraControl: CameraControl? = null

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) startApp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startApp()
        else permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun startApp() {
        setContent {
            var zoom by remember { mutableFloatStateOf(1f) }
            var isVideo by remember { mutableStateOf(false) }
            var isRec by remember { mutableStateOf(false) }
            MaterialTheme {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    AndroidView(factory = { ctx ->
                        val pv = androidx.camera.view.PreviewView(ctx)
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                            val imgCap = ImageCapture.Builder().build()
                            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
                            val vidCap = VideoCapture.withOutput(recorder)
                            provider.unbindAll()
                            val cam = provider.bindToLifecycle(this@MainActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imgCap, vidCap)
                            cameraControl = cam.cameraControl
                            imageCapture = imgCap
                            videoCapture = vidCap
                        }, ContextCompat.getMainExecutor(ctx))
                        pv
                    }, modifier = Modifier.fillMaxSize())

                    Text("8K UltraZoom ${zoom.toInt()}x", color = Color.White, modifier = Modifier.padding(16.dp).background(Color.Black.copy(0.6f)).padding(6.dp))

                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(0.8f)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1x", color = Color.White)
                            Slider(value = zoom, onValueChange = { zoom = it; cameraControl?.setZoomRatio(it) }, valueRange = 1f..100f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(activeTrackColor = Color.Yellow, thumbColor = Color.White))
                            Text("100x", color = Color.White)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { isVideo = !isVideo }) { Text(if(isVideo) "PHOTO" else "VIDEO") }
                            Button(onClick = {
                                if (isVideo) {
                                    if (isRec) { recording?.stop(); isRec = false } else { startVideo(); isRec = true }
                                } else takePhoto()
                            }, shape = CircleShape, modifier = Modifier.size(80.dp), colors = ButtonDefaults.buttonColors(containerColor = if(isRec) Color.Red else Color.White)) {
                                Text(if(isVideo) "REC" else "CAPTURE", color = Color.Black)
                            }
                            Button(onClick = { Toast.makeText(this@MainActivity, "Photos in DCIM/UltraZoom", Toast.LENGTH_LONG).show() }) { Text("Gallery") }
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto() {
        val cap = imageCapture ?: return
        val file = File(getExternalFilesDir(null), "8K_${System.currentTimeMillis()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        cap.takePicture(output, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(e: ImageCaptureException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
            override fun onImageSaved(o: ImageCapture.OutputFileResults) {
                // Gallery madhe add kara
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/UltraZoom")
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                }
                try { contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) } catch (e: Exception) {}
                runOnUiThread { Toast.makeText(this@MainActivity, "✅ Photo Saved! ${file.name}", Toast.LENGTH_LONG).show() }
            }
        })
    }

    private fun startVideo() {
        val cap = videoCapture ?: return
        val file = File(getExternalFilesDir(null), "8K_${System.currentTimeMillis()}.mp4")
        val output = FileOutputOptions.Builder(file).build()
        recording = cap.output.prepareRecording(this, output).apply {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) withAudioEnabled()
        }.start(executor) { event ->
            if (event is VideoRecordEvent.Finalize) {
                runOnUiThread {
                    if (event.hasError()) Toast.makeText(this, "Video Fail", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "✅ Video Saved!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); executor.shutdown() }
}
