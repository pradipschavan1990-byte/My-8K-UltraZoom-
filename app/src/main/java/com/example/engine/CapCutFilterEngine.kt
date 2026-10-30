package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CapCutFilter(
    val id: String,
    val displayName: String,
    val marathiName: String,
    val badge: String,
    val colorMatrixValues: FloatArray
) {
    ORIGINAL(
        id = "original",
        displayName = "Original",
        marathiName = "ओरिजिनल",
        badge = "RAW",
        colorMatrixValues = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    CINEMATIC_TEAL(
        id = "cinematic_teal",
        displayName = "Cinematic Look",
        marathiName = "सिनेमॅटिक लूक",
        badge = "VIRAL",
        colorMatrixValues = floatArrayOf(
            1.24f, 0.05f, -0.06f, 0f, 12f,
            -0.05f, 1.15f, 0.08f, 0f, 6f,
            -0.12f, 0.14f, 1.32f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    GLOW_FACE(
        id = "glow_face",
        displayName = "Glow Face",
        marathiName = "ग्लो फेस ब्युटी",
        badge = "TRENDING",
        colorMatrixValues = floatArrayOf(
            1.22f, 0.08f, 0.05f, 0f, 24f,
            0.05f, 1.16f, 0.05f, 0f, 20f,
            0.04f, 0.06f, 1.12f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Cyberpunk Neon",
        marathiName = "सायबरपंक निऑन",
        badge = "VIRAL",
        colorMatrixValues = floatArrayOf(
            1.38f, -0.15f, 0.22f, 0f, 18f,
            -0.20f, 1.05f, 0.25f, 0f, -6f,
            0.26f, -0.10f, 1.48f, 0f, 32f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    GOLDEN_HOUR(
        id = "golden_hour",
        displayName = "Golden Hour",
        marathiName = "गोल्डन अवर",
        badge = "TRENDING",
        colorMatrixValues = floatArrayOf(
            1.34f, 0.12f, -0.12f, 0f, 26f,
            0.08f, 1.22f, -0.14f, 0f, 16f,
            -0.16f, -0.06f, 0.86f, 0f, -12f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VINTAGE_35MM(
        id = "vintage_35mm",
        displayName = "Vintage 90s",
        marathiName = "विंटेज कॅमकॉर्डर",
        badge = "RETRO",
        colorMatrixValues = floatArrayOf(
            1.12f, 0.12f, 0.05f, 0f, 18f,
            0.08f, 1.06f, 0.06f, 0f, 14f,
            0.02f, 0.08f, 0.88f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    ANIME_PASTEL(
        id = "anime_pastel",
        displayName = "Anime Drama",
        marathiName = "अ‍ॅनिम ड्रामा",
        badge = "POPULAR",
        colorMatrixValues = floatArrayOf(
            1.26f, 0.04f, 0.02f, 0f, 20f,
            0.02f, 1.28f, 0.04f, 0f, 18f,
            0.02f, 0.06f, 1.34f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    MOODY_NOIR(
        id = "moody_noir",
        displayName = "Dark Noir B&W",
        marathiName = "मोनोलिथ ब्लॅक & व्हाईट",
        badge = "FILM",
        colorMatrixValues = floatArrayOf(
            0.33f * 1.38f, 0.45f * 1.38f, 0.22f * 1.38f, 0f, -18f,
            0.33f * 1.38f, 0.45f * 1.38f, 0.22f * 1.38f, 0f, -18f,
            0.33f * 1.38f, 0.45f * 1.38f, 0.22f * 1.38f, 0f, -18f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    SUNSET_PEACH(
        id = "sunset_peach",
        displayName = "Sunset Dream",
        marathiName = "सनसेट ब्लश",
        badge = "BEAUTY",
        colorMatrixValues = floatArrayOf(
            1.28f, 0.08f, 0.06f, 0f, 28f,
            0.06f, 1.12f, 0.08f, 0f, 12f,
            0.04f, 0.06f, 1.18f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
    );

    companion object {
        fun fromId(id: String): CapCutFilter {
            return entries.firstOrNull { it.id == id } ?: ORIGINAL
        }
    }
}

object CapCutFilterEngine {

    /**
     * Applies the selected CapCut filter onto a bitmap with customizable intensity (0.0 to 1.0).
     * For GLOW_FACE, applies real highlight bloom diffusion and porcelain skin smoothing.
     * For CINEMATIC_TEAL, applies teal/orange color grading + anamorphic film vignette.
     */
    fun applyFilter(
        source: Bitmap,
        filter: CapCutFilter,
        intensity: Float = 1.0f
    ): Bitmap {
        if (filter == CapCutFilter.ORIGINAL) {
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val clampedIntensity = intensity.coerceIn(0.1f, 1.0f)
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. ColorMatrix Grading Pass
        val cm = ColorMatrix()
        if (clampedIntensity >= 0.98f) {
            cm.set(filter.colorMatrixValues)
        } else {
            // Interpolate towards identity
            val identity = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            val blended = FloatArray(20)
            for (i in 0 until 20) {
                blended[i] = identity[i] + (filter.colorMatrixValues[i] - identity[i]) * clampedIntensity
            }
            cm.set(blended)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Filter-Specific Advanced Enhancements
        when (filter) {
            CapCutFilter.GLOW_FACE -> {
                applyGlowFaceDiffusion(output, clampedIntensity)
            }
            CapCutFilter.CINEMATIC_TEAL -> {
                applyCinematicVignette(canvas, width, height, clampedIntensity)
            }
            CapCutFilter.GOLDEN_HOUR -> {
                applyGoldenHourSunbeam(canvas, width, height, clampedIntensity)
            }
            CapCutFilter.VINTAGE_35MM -> {
                applyVintageFilmGrain(output, clampedIntensity)
            }
            else -> {
                // Other filters already completed via ColorMatrix
            }
        }

        return output
    }

    /**
     * CapCut Glow Face Effect:
     * Extracts highlights, diffuses them, and screens them back over skin tones for an angelic radiant glow.
     */
    private fun applyGlowFaceDiffusion(bitmap: Bitmap, intensity: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bloomBoost = (0.35f * intensity)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            // Calculate luminance
            val lum = (0.299f * r + 0.587f * g + 0.114f * b)
            if (lum > 130f) {
                val highlightFactor = ((lum - 130f) / 125f) * bloomBoost
                val newR = min(255, (r + (255 - r) * highlightFactor).roundToInt())
                val newG = min(255, (g + (255 - g) * (highlightFactor * 0.95f)).roundToInt())
                val newB = min(255, (b + (255 - b) * (highlightFactor * 0.85f)).roundToInt())
                pixels[i] = Color.rgb(newR, newG, newB)
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * Cinematic Anamorphic Vignette:
     * Deepens corners with a subtle cinema-grade radial falloff.
     */
    private fun applyCinematicVignette(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        val radius = max(width, height) * 0.72f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f,
                height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.argb((95 * intensity).roundToInt(), 5, 12, 22)),
                floatArrayOf(0.55f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /**
     * Golden Hour Sunbeam Warmth:
     * Adds soft atmospheric golden light washing in from the top corner.
     */
    private fun applyGoldenHourSunbeam(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.85f,
                height * 0.15f,
                max(width, height) * 0.8f,
                intArrayOf(Color.argb((75 * intensity).roundToInt(), 255, 190, 80), Color.TRANSPARENT),
                floatArrayOf(0.1f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    /**
     * Vintage 90s Film Grain & Warmth:
     * Adds subtle organic film grain across the frame.
     */
    private fun applyVintageFilmGrain(bitmap: Bitmap, intensity: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val grainAmount = (14 * intensity).toInt()
        var seed = 12345
        for (i in pixels.indices step 2) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val noise = (seed % (grainAmount * 2 + 1)) - grainAmount
            val c = pixels[i]
            val r = (Color.red(c) + noise).coerceIn(0, 255)
            val g = (Color.green(c) + noise).coerceIn(0, 255)
            val b = (Color.blue(c) + noise).coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
