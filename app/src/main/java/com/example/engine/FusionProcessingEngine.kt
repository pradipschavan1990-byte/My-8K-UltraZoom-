package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class FusionProgress(
    val frameNumber: Int,
    val totalFrames: Int = 100,
    val stage: String,
    val progressPercent: Float
)

object FusionProcessingEngine {

    suspend fun process100FrameFusion(
        context: Context,
        baseBitmap: Bitmap,
        zoomFactor: Float,
        isMoonMode: Boolean = false,
        isDehaze: Boolean = false,
        onProgress: (FusionProgress) -> Unit = {}
    ): Pair<Bitmap, Bitmap> = withContext(Dispatchers.Default) {
        // Step 1: Burst frame simulation & Sub-pixel accumulation
        val totalBurst = 100
        val burstInterval = 10
        for (i in 1..totalBurst step burstInterval) {
            delay(15)
            val currentFrame = min(i + burstInterval - 1, totalBurst)
            val stageName = when {
                currentFrame <= 30 -> "Capturing 100-Frame Raw Burst..."
                currentFrame <= 65 -> "AI Sub-Pixel Optical Alignment..."
                currentFrame <= 85 -> if (isMoonMode) "Lunar Crater Synthesis & Tone Lock..." else "Multi-Frame Noise Cancellation..."
                else -> if (isDehaze) "Atmospheric De-Haze Restoration..." else "HDREAL 2.0 Super-Resolution Matrix..."
            }
            onProgress(FusionProgress(currentFrame, totalBurst, stageName, currentFrame / 100f))
        }

        // Keep raw copy
        val rawCopy = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Process Enhanced Bitmap
        val width = baseBitmap.width
        val height = baseBitmap.height
        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Color matrix adjustments
        val cm = ColorMatrix()
        if (isMoonMode) {
            // High contrast for craters, deep blacks for space
            val contrast = 1.6f
            val brightness = -15f
            cm.set(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        } else if (isDehaze) {
            // Cut through haze: increase contrast and saturation
            cm.setSaturation(1.35f)
            val contrastCm = ColorMatrix(floatArrayOf(
                1.3f, 0f, 0f, 0f, -18f,
                0f, 1.3f, 0f, 0f, -18f,
                0f, 0f, 1.3f, 0f, -18f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastCm)
        } else {
            // HDREAL 2.0 standard: vibrant clarity and crisp micro-contrast
            cm.setSaturation(1.15f)
            val clarityCm = ColorMatrix(floatArrayOf(
                1.18f, 0f, 0f, 0f, -6f,
                0f, 1.18f, 0f, 0f, -6f,
                0f, 0f, 1.18f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(clarityCm)
        }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)

        // Apply fast 3x3 unsharp mask / edge sharpening convolution
        val sharpened = applySharpenFilter(enhanced, sharpnessStrength = if (isMoonMode) 1.5f else 1.25f)

        onProgress(FusionProgress(100, 100, "100-Frame AI Fusion Complete!", 1.0f))
        Pair(rawCopy, sharpened)
    }

    private fun applySharpenFilter(src: Bitmap, sharpnessStrength: Float): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Read pixel data
        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val centerWeight = 1f + 4f * sharpnessStrength
        val edgeWeight = -sharpnessStrength

        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val idx = yOffset + x

                val cCenter = pixels[idx]
                val cTop = pixels[idx - width]
                val cBottom = pixels[idx + width]
                val cLeft = pixels[idx - 1]
                val cRight = pixels[idx + 1]

                val a = Color.alpha(cCenter)

                val r = (centerWeight * Color.red(cCenter) +
                        edgeWeight * (Color.red(cTop) + Color.red(cBottom) + Color.red(cLeft) + Color.red(cRight))).roundToInt().coerceIn(0, 255)

                val g = (centerWeight * Color.green(cCenter) +
                        edgeWeight * (Color.green(cTop) + Color.green(cBottom) + Color.green(cLeft) + Color.green(cRight))).roundToInt().coerceIn(0, 255)

                val b = (centerWeight * Color.blue(cCenter) +
                        edgeWeight * (Color.blue(cTop) + Color.blue(cBottom) + Color.blue(cLeft) + Color.blue(cRight))).roundToInt().coerceIn(0, 255)

                outPixels[idx] = Color.argb(a, r, g, b)
            }
        }

        // Copy borders
        for (x in 0 until width) {
            outPixels[x] = pixels[x]
            outPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            outPixels[y * width] = pixels[y * width]
            outPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "ultra_zoom_captures")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            file.absolutePath
        }

    fun loadBitmapFromResourceOrFile(context: Context, pathOrRes: String): Bitmap? {
        return try {
            if (pathOrRes.startsWith("/")) {
                BitmapFactory.decodeFile(pathOrRes)
            } else {
                val resId = context.resources.getIdentifier(pathOrRes, "drawable", context.packageName)
                if (resId != 0) {
                    BitmapFactory.decodeResource(context.resources, resId)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
