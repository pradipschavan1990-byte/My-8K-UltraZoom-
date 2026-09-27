package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.PhotoRestoreScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CameraViewModel

enum class AppScreen {
    CAMERA,
    GALLERY,
    RESTORE
}

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppNavHost(viewModel = cameraViewModel)
            }
        }
    }
}

@Composable
fun MainAppNavHost(viewModel: CameraViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.CAMERA) }

    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
        when (currentScreen) {
            AppScreen.CAMERA -> {
                CameraScreen(
                    viewModel = viewModel,
                    onNavigateToGallery = { currentScreen = AppScreen.GALLERY },
                    onNavigateToRestore = { currentScreen = AppScreen.RESTORE }
                )
            }
            AppScreen.GALLERY -> {
                GalleryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AppScreen.CAMERA }
                )
            }
            AppScreen.RESTORE -> {
                PhotoRestoreScreen(
                    onNavigateBack = { currentScreen = AppScreen.CAMERA }
                )
            }
        }
    }
}
