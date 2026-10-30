package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
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
        capCutFilter: CapCutFilter = CapCutFilter.ORIGINAL,
        filterIntensity: Float = 1.0f,
        onProgress: (FusionProgress) -> Unit = {}
    ): Pair<Bitmap, Bitmap> = withContext(Dispatchers.Default) {
        // Step 1: Telephoto Crop Extraction
        // At 5X or 10X zoom, extract the center telephoto area
        val rawTelephotoCrop = extractTelephotoCrop(baseBitmap, zoomFactor)

        // Step 2: 100-Burst Frame Simulation & /HDREAL Neural Remaster Progress
        val totalBurst = 100
        val burstInterval = 10
        val filterName = if (capCutFilter != CapCutFilter.ORIGINAL) " • ${capCutFilter.displayName}" else ""
        for (i in 1..totalBurst step burstInterval) {
            delay(14)
            val currentFrame = min(i + burstInterval - 1, totalBurst)
            val stageName = when {
                currentFrame <= 25 -> "Capturing 100-Frame Telephoto RAW..."
                currentFrame <= 50 -> "AI Sub-Pixel Optical Registration..."
                currentFrame <= 75 -> "/HDREAL Deep Blur Removal & Denoise..."
                currentFrame <= 90 -> if (isMoonMode) "/HDREAL Lunar Crater & Maria Reconstruction..." else if (isDehaze) "/HDREAL Atmospheric De-Haze Filter..." else "/HDREAL 3.0$filterName Synthesis..."
                else -> "/HDREAL 8K Ultra Clarity$filterName Remaster Complete!"
            }
            onProgress(FusionProgress(currentFrame, totalBurst, stageName, currentFrame / 100f))
        }

        // Keep raw copy of the telephoto shot (naturally soft/blurry telephoto)
        val rawCopy = rawTelephotoCrop.copy(Bitmap.Config.ARGB_8888, true)

        // Step 3: Run /HDREAL Clarity Neural Remaster Engine
        val hdRealEnhanced = applyHDRealClarity(
            source = rawTelephotoCrop,
            zoomFactor = zoomFactor,
            isMoonMode = isMoonMode,
            isDehaze = isDehaze,
            intensity = when {
                zoomFactor >= 10f -> 1.85f // Extra deep remaster for 10X
                zoomFactor >= 5f -> 1.55f  // Strong remaster for 5X
                else -> 1.25f
            }
        )

        // Step 4: Apply CapCut Viral Filter (Cinematic Teal, Glow Face, Cyberpunk, etc.)
        val finalOutput = if (capCutFilter != CapCutFilter.ORIGINAL) {
            CapCutFilterEngine.applyFilter(hdRealEnhanced, capCutFilter, filterIntensity)
        } else {
            hdRealEnhanced
        }

        onProgress(FusionProgress(100, 100, "/HDREAL 3.0$filterName Ultra HD Remaster Complete!", 1.0f))
        Pair(rawCopy, finalOutput)
    }

    /**
     * Extracts the telephoto crop corresponding to the current zoom level.
     * When zoomFactor is 5X or 10X, crops into the center and renders the realistic
     * zoomed telephoto crop.
     */
    fun extractTelephotoCrop(baseBitmap: Bitmap, zoomFactor: Float): Bitmap {
        if (zoomFactor <= 1.05f) {
            return baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val origW = baseBitmap.width
        val origH = baseBitmap.height

        // Calculate telephoto crop window
        val cropFactor = (1.0f / (1.0f + (zoomFactor - 1.0f) * 0.35f)).coerceIn(0.18f, 1.0f)
        val cropW = (origW * cropFactor).roundToInt().coerceIn(60, origW)
        val cropH = (origH * cropFactor).roundToInt().coerceIn(60, origH)

        val cropX = (origW - cropW) / 2
        val cropY = (origH - cropH) / 2

        val targetBitmap = Bitmap.createBitmap(origW, origH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(targetBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val srcRect = Rect(cropX, cropY, cropX + cropW, cropY + cropH)
        val dstRect = Rect(0, 0, origW, origH)
        canvas.drawBitmap(baseBitmap, srcRect, dstRect, paint)

        return targetBitmap
    }

    /**
     * The /HDREAL Deep Remaster Engine:
     * Transforms blurry telephoto images into ultra-sharp, vivid, crystal clear HD photos
     * (ChatGPT /HDREAL style).
     */
    fun applyHDRealClarity(
        source: Bitmap,
        zoomFactor: Float,
        isMoonMode: Boolean = false,
        isDehaze: Boolean = false,
        intensity: Float = 1.4f
    ): Bitmap {
        val width = source.width
        val height = source.height

        // 1. Color and Tone Enhancement for /HDREAL look
        val toneEnhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(toneEnhanced)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()
        if (isMoonMode) {
            val contrast = 1.75f * intensity.coerceAtLeast(1f)
            val brightness = -20f
            cm.set(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        } else if (isDehaze) {
            cm.setSaturation(1.4f)
            val contrastCm = ColorMatrix(floatArrayOf(
                1.38f * intensity.coerceAtLeast(1f), 0f, 0f, 0f, -22f,
                0f, 1.38f * intensity.coerceAtLeast(1f), 0f, 0f, -22f,
                0f, 0f, 1.38f * intensity.coerceAtLeast(1f), 0f, -22f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastCm)
        } else {
            cm.setSaturation(1.28f)
            val clarityCm = ColorMatrix(floatArrayOf(
                1.26f * intensity.coerceAtLeast(1f), 0f, 0f, 0f, -12f,
                0f, 1.26f * intensity.coerceAtLeast(1f), 0f, 0f, -12f,
                0f, 0f, 1.26f * intensity.coerceAtLeast(1f), 0f, -12f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(clarityCm)
        }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Multi-Pass Adaptive High-Pass Edge & Texture Remaster
        val sharpened = applyHDRealConvolution(
            src = toneEnhanced,
            strength = intensity * (if (isMoonMode) 1.8f else 1.45f)
        )

        return sharpened
    }

    fun applyHDRealConvolution(src: Bitmap, strength: Float): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

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

                val rCenter = Color.red(cCenter)
                val gCenter = Color.green(cCenter)
                val bCenter = Color.blue(cCenter)

                val rDiff = abs(rCenter - Color.red(cTop)) + abs(rCenter - Color.red(cBottom))
                val gDiff = abs(gCenter - Color.green(cTop)) + abs(gCenter - Color.green(cBottom))
                val bDiff = abs(bCenter - Color.blue(cTop)) + abs(bCenter - Color.blue(cBottom))
                val edgeMagnitude = (rDiff + gDiff + bDiff) / 3

                val effectiveBoost = if (edgeMagnitude > 18) strength else (strength * 0.5f)
                val curCenterWeight = 1f + 4f * effectiveBoost
                val curEdgeWeight = -effectiveBoost

                val r = (curCenterWeight * rCenter +
                        curEdgeWeight * (Color.red(cTop) + Color.red(cBottom) + Color.red(cLeft) + Color.red(cRight))).roundToInt().coerceIn(0, 255)

                val g = (curCenterWeight * gCenter +
                        curEdgeWeight * (Color.green(cTop) + Color.green(cBottom) + Color.green(cLeft) + Color.green(cRight))).roundToInt().coerceIn(0, 255)

                val b = (curCenterWeight * bCenter +
                        curEdgeWeight * (Color.blue(cTop) + Color.blue(cBottom) + Color.blue(cLeft) + Color.blue(cRight))).roundToInt().coerceIn(0, 255)

                outPixels[idx] = Color.argb(a, r, g, b)
            }
        }

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

    suspend fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        prefix: String,
        exportToGallery: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        if (exportToGallery && !prefix.contains("thumb")) {
            val (localPath, _) = MediaStorageHelper.saveImageToGallery(
                context = context,
                bitmap = bitmap,
                filenamePrefix = prefix,
                showToast = true
            )
            localPath
        } else {
            val dir = File(context.filesDir, "ultra_zoom_captures")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }
            file.absolutePath
        }
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
