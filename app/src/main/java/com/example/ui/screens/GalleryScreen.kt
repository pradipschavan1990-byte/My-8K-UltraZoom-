package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.engine.MediaStorageHelper
import kotlinx.coroutines.launch
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CapturedPhotoEntity
import com.example.engine.CapCutFilter
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.WarningRed
import com.example.viewmodel.CameraViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val photos by viewModel.allPhotos.collectAsState()
    var selectedPhoto by remember { mutableStateOf<CapturedPhotoEntity?>(null) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    val context = LocalContext.current

    // Detailed Inspect View (Photo Compare or Video Player with CapCut filter tools)
    if (selectedPhoto != null) {
        val photo = selectedPhoto!!
        val isVideo = photo.mediaType == "VIDEO"
        val videoFile = photo.videoPath?.let { File(it) }
        val enhancedFile = File(photo.enhancedImagePath)
        val rawFile = photo.rawImagePath?.let { File(it) }

        val enhancedBitmap = remember(photo.enhancedImagePath) {
            if (enhancedFile.exists()) BitmapFactory.decodeFile(enhancedFile.absolutePath) else null
        }
        val rawBitmap = remember(photo.rawImagePath) {
            if (rawFile != null && rawFile.exists()) BitmapFactory.decodeFile(rawFile.absolutePath) else null
        }

        var previewCapCutFilter by remember { mutableStateOf<CapCutFilter?>(null) }
        var filterSuccessMessage by remember { mutableStateOf<String?>(null) }
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .testTag("gallery_detail_view")
                .fillMaxSize()
                .background(ObsidianBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar with Back & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedPhoto = null },
                        modifier = Modifier.testTag("close_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close View",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = photo.title,
                            color = CyberGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = if (isVideo) "4K VIDEO • CAPCUT FILTERED" else "10X PHOTO • /HDREAL REMASTERED",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.deletePhoto(photo.id)
                            selectedPhoto = null
                        },
                        modifier = Modifier.testTag("delete_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = WarningRed
                        )
                    }
                }

                // Media Viewer (Video Player OR Photo Compare Slider)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkGlassSurface)
                        .border(1.5.dp, if (isVideo) WarningRed else NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo && videoFile != null && videoFile.exists()) {
                        // Native Android VideoView Player
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.fromFile(videoFile))
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("gallery_video_player")
                        )

                        // Video Tag Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarningRed.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "4K VIDEO • ${photo.videoDurationSeconds}s",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (rawBitmap != null && enhancedBitmap != null) {
                        BeforeAfterSlider(
                            beforeBitmap = rawBitmap,
                            afterBitmap = enhancedBitmap,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (enhancedBitmap != null) {
                        Image(
                            bitmap = enhancedBitmap.asImageBitmap(),
                            contentDescription = "Enhanced Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(
                                id = if (photo.isMoonMode) R.drawable.sample_moon else R.drawable.sample_eagle
                            ),
                            contentDescription = "Sample Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Telemetry & Filter Metadata Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkGlassCard)
                        .border(1.dp, DarkGlassBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MAGNIFICATION: ${String.format("%.1fX", photo.zoomLevel)}",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DISTANCE: ${photo.estimatedDistance}",
                            color = CyberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FILTER: ${photo.filterName.uppercase()}",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "STABILITY: ${photo.stabilityScore}%",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "SAVED: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(photo.timestamp))}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Explicit Save/Export to Device Gallery button (DCIM/UltraZoom8K)
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch {
                            if (isVideo && videoFile != null && videoFile.exists()) {
                                MediaStorageHelper.exportVideoToGallery(
                                    context = context,
                                    sourceVideoFile = videoFile,
                                    filenamePrefix = "video_${photo.filterName.lowercase()}",
                                    showToast = true
                                )
                            } else if (enhancedBitmap != null) {
                                MediaStorageHelper.saveImageToGallery(
                                    context = context,
                                    bitmap = enhancedBitmap,
                                    filenamePrefix = "photo_${photo.filterName.lowercase()}",
                                    showToast = true
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_to_device_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Save to Phone Gallery",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "फोन गॅलरीमध्ये सेव्ह करा (DCIM/UltraZoom8K)",
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                // If Photo: Provide CapCut Viral Filter Switcher
                if (!isVideo) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkGlassCard)
                            .border(1.dp, DarkGlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "CapCut Filters",
                                tint = CyberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "APPLY CAPCUT VIRAL FILTERS",
                                color = CyberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Horizontal Filter selection
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CapCutFilter.entries.filter { it != CapCutFilter.ORIGINAL }) { filter ->
                                val isSelected = previewCapCutFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberGold.copy(alpha = 0.25f) else DarkGlassSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberGold else DarkGlassBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            previewCapCutFilter = filter
                                            viewModel.applyCapCutFilterToExistingPhoto(photo, filter, 1.0f)
                                            filterSuccessMessage = "Saved with ${filter.displayName}!"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = filter.displayName,
                                            color = if (isSelected) CyberGold else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = filter.marathiName,
                                            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f),
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        if (filterSuccessMessage != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = filterSuccessMessage!!,
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Boost with /HDREAL Ultra Max Button
                    Button(
                        onClick = {
                            viewModel.applyUltraMaxRemaster(photo)
                            selectedPhoto = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("apply_hdreal_max_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Apply /HDREAL",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ /HDREAL 8K MAX CLARITY (BOOST 220%)",
                            color = ObsidianBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        return
    }

    // Main Gallery Grid View
    Column(
        modifier = Modifier
            .testTag("gallery_screen")
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("gallery_back_button")
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
                    text = "8K TELEPHOTO VAULT",
                    color = CyberGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${photos.size} Photos & Videos with CapCut Filters",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Filter Categories Tabs (ALL, PHOTOS, VIDEOS, CAPCUT)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("ALL", "PHOTOS", "VIDEOS", "CAPCUT").forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberGold,
                        selectedLabelColor = ObsidianBlack,
                        containerColor = DarkGlassCard,
                        labelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) CyberGold else DarkGlassBorder
                    ),
                    modifier = Modifier.testTag("gallery_tab_$cat")
                )
            }
        }

        val filteredPhotos = photos.filter {
            when (selectedCategory) {
                "PHOTOS" -> it.mediaType == "IMAGE"
                "VIDEOS" -> it.mediaType == "VIDEO"
                "CAPCUT" -> it.filterName != "Original"
                else -> true
            }
        }

        if (filteredPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "No items",
                        tint = NeonCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No Items in $selectedCategory",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Capture a photo or record 4K 10X video with CapCut filters!",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredPhotos, key = { it.id }) { photo ->
                    val isVideo = photo.mediaType == "VIDEO"

                    Card(
                        modifier = Modifier
                            .testTag("photo_card_${photo.id}")
                            .fillMaxWidth()
                            .clickable { selectedPhoto = photo },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isVideo) WarningRed.copy(alpha = 0.6f) else DarkGlassBorder
                        )
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.2f)
                                    .background(DarkGlassSurface)
                            ) {
                                val file = File(photo.enhancedImagePath)
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = photo.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(
                                            id = if (photo.isMoonMode) R.drawable.sample_moon else R.drawable.sample_eagle
                                        ),
                                        contentDescription = photo.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Video badge or Zoom Badge
                                if (isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(WarningRed.copy(alpha = 0.9f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${photo.videoDurationSeconds}s",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Center Play icon
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ObsidianBlack.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = CyberGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                } else {
                                    // Zoom badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .background(ObsidianBlack.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${photo.zoomLevel.toInt()}X",
                                            color = CyberGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Filter Tag at bottom right
                                if (photo.filterName != "Original") {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .background(DarkGlassCard.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                            .border(0.8.dp, CyberGold, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = photo.filterName.uppercase(),
                                            color = CyberGold,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Meta info
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = photo.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isVideo) "4K VIDEO • ${photo.filterName}" else photo.captureMode,
                                    color = if (isVideo) WarningRed else NeonGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
