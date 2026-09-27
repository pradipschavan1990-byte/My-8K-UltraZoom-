package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_photos")
data class CapturedPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val enhancedImagePath: String,
    val rawImagePath: String? = null,
    val zoomLevel: Float = 10f,
    val estimatedDistance: String = "45.2 m",
    val fusionFramesCount: Int = 100,
    val captureMode: String = "100-Frame Fusion",
    val stabilityScore: Int = 96,
    val hasDehaze: Boolean = false,
    val isMoonMode: Boolean = false,
    val title: String = "UltraZoom 10X Shot"
)
