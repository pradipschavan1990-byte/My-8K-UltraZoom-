package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppDatabase
import com.example.data.CapturedPhotoEntity
import com.example.engine.FusionProcessingEngine
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoRestoreScreen(
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedBitmap by remember {
        mutableStateOf<Bitmap?>(BitmapFactory.decodeResource(context.resources, R.drawable.sample_eagle))
    }
    var rawOriginalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedResultBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var targetZoom by remember { mutableFloatStateOf(10.0f) }
    var isDehazeEnabled by remember { mutableStateOf(true) }
    var isMoonModeEnabled by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        withContext(Dispatchers.Main) {
                            selectedBitmap = bmp
                            enhancedResultBitmap = null
                            saveSuccessMessage = false
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .testTag("photo_restore_screen")
            .fillMaxSize()
            .background(ObsidianBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("restore_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Camera",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "OLD PHOTO ZOOM RESTORE",
                    color = CyberGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "10X Super-Resolution & De-Haze Recovery",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Image Preview or Before/After Interactive Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkGlassSurface)
                .border(1.5.dp, if (enhancedResultBitmap != null) NeonCyan else DarkGlassBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (enhancedResultBitmap != null && rawOriginalBitmap != null) {
                BeforeAfterSlider(
                    beforeBitmap = rawOriginalBitmap!!,
                    afterBitmap = enhancedResultBitmap!!,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedBitmap != null) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Select a distant or blurry photo to restore",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBlack.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = CyberGold)
                        Text(
                            text = "Synthesizing 100-Frame HDREAL 2.0...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Photo Source Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PHOTO SOURCE",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGlassCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("upload_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Upload Photo",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Preset Eagle Button
                Button(
                    onClick = {
                        selectedBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sample_eagle)
                        enhancedResultBitmap = null
                        isMoonModeEnabled = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGlassCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkGlassBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("preset_eagle_button")
                ) {
                    Text(
                        text = "Eagle Sample",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Preset Moon Button
                Button(
                    onClick = {
                        selectedBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sample_moon)
                        enhancedResultBitmap = null
                        isMoonModeEnabled = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGlassCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkGlassBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("preset_moon_button")
                ) {
                    Text(
                        text = "Moon Sample",
                        color = CyberGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // AI Tuning Options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkGlassCard)
                .border(1.dp, DarkGlassBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "RESTORATION ENHANCEMENTS",
                color = CyberGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Zoom Factor Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reconstruction Zoom Magnification:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format("%.0fX", targetZoom),
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = targetZoom,
                onValueChange = { targetZoom = it },
                valueRange = 2f..20f,
                steps = 17,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = DarkGlassBorder
                ),
                modifier = Modifier.testTag("restore_zoom_slider")
            )

            // Dehaze switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterDrama,
                        contentDescription = "De-Haze",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Atmospheric De-Haze Filter",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Switch(
                    checked = isDehazeEnabled,
                    onCheckedChange = { isDehazeEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = DarkGlassSurface
                    )
                )
            }

            // Moon Mode switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Moon Mode",
                        tint = CyberGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Astro & Crater Texture Recovery",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Switch(
                    checked = isMoonModeEnabled,
                    onCheckedChange = { isMoonModeEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberGold,
                        checkedTrackColor = DarkGlassSurface
                    )
                )
            }
        }

        // Action Trigger Button: AI RESTORE 10X
        Button(
            onClick = {
                val bmp = selectedBitmap ?: return@Button
                isProcessing = true
                saveSuccessMessage = false
                coroutineScope.launch {
                    val (raw, enhanced) = FusionProcessingEngine.process100FrameFusion(
                        context = context,
                        baseBitmap = bmp,
                        zoomFactor = targetZoom,
                        isMoonMode = isMoonModeEnabled,
                        isDehaze = isDehazeEnabled
                    )
                    rawOriginalBitmap = raw
                    enhancedResultBitmap = enhanced
                    isProcessing = false
                }
            },
            enabled = selectedBitmap != null && !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("run_ai_restore_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = "Run AI Restore",
                tint = ObsidianBlack
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RUN 10X AI RESTORATION",
                color = ObsidianBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Save restored image to Vault
        if (enhancedResultBitmap != null) {
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val enhanced = enhancedResultBitmap ?: return@launch
                        val raw = rawOriginalBitmap
                        val enhancedPath = FusionProcessingEngine.saveBitmapToFile(context, enhanced, "restored_100x")
                        val rawPath = raw?.let { FusionProcessingEngine.saveBitmapToFile(context, it, "restored_raw") }

                        val entity = CapturedPhotoEntity(
                            enhancedImagePath = enhancedPath,
                            rawImagePath = rawPath,
                            zoomLevel = targetZoom,
                            estimatedDistance = "Restored Photo",
                            fusionFramesCount = 100,
                            captureMode = "10X Old Photo Restore",
                            stabilityScore = 99,
                            hasDehaze = isDehazeEnabled,
                            isMoonMode = isMoonModeEnabled,
                            title = "AI Restored Photo"
                        )
                        val db = AppDatabase.getDatabase(context)
                        db.photoDao().insertPhoto(entity)

                        withContext(Dispatchers.Main) {
                            saveSuccessMessage = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_restored_to_vault")
            ) {
                Icon(
                    imageVector = if (saveSuccessMessage) Icons.Default.CheckCircle else Icons.Default.Save,
                    contentDescription = "Save",
                    tint = ObsidianBlack
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (saveSuccessMessage) "SAVED TO 8K VAULT!" else "SAVE RESTORED TO GALLERY",
                    color = ObsidianBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
