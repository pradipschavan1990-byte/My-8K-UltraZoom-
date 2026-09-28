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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraControl: CameraControl? = null
    private var zoomState by mutableFloatStateOf(1f)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.CAMERA] == true) startApp()
        else Toast.makeText(this, "Camera permission dya", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkPermission()
    }

    private fun checkPermission() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT <= 32) perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startApp()
        } else {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    private fun startApp() {
        setContent {
            var currentZoom by remember { mutableFloatStateOf(1f) }
            val context = LocalContext.current

            Scaffold { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Box(Modifier.weight(1f)) {
                        CameraPreview(
                            onUseCase = { control, capture ->
                                cameraControl = control
                                imageCapture = capture
                            }
                        )
                    }
                    Slider(
                        value = currentZoom,
                        onValueChange = {
                            currentZoom = it
                            cameraControl?.setZoomRatio(it)
                        },
                        valueRange = 1f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { takePhoto() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CAPTURE - ${currentZoom.toInt()}x Zoom")
                    }
                }
            }
        }
    }

    @Composable
    fun CameraPreview(onUseCase: (CameraControl, ImageCapture) -> Unit) {
        val context = LocalContext.current
        AndroidView(
            factory = { ctx ->
                val previewView = androidx.camera.view.PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(
                            this, selector, preview, capture
                        )
                        onUseCase(camera.cameraControl, capture)
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Camera Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "UltraZoom_${name}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/UltraZoom")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val output = uri?.let { ImageCapture.OutputFileOptions.Builder(contentResolver, it, values).build() } ?: return

        capture.takePicture(output, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Fail: ${exc.message}", Toast.LENGTH_SHORT).show() }
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (Build.VERSION.SDK_INT >= 29) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        uri?.let { contentResolver.update(it, values, null, null) }
                    }
                    runOnUiThread { Toast.makeText(this@MainActivity, "Photo Saved to Gallery!", Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
