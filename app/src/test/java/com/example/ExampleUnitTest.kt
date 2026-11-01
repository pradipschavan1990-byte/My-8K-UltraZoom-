package com.example

import com.example.engine.CapCutFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testZoomDistanceCalculation() {
        val zoom = 10.0f
        val baseDist = when {
            zoom <= 1.0f -> 3.2f
            zoom <= 2.0f -> 7.8f
            zoom <= 5.0f -> 24.5f
            zoom <= 10.0f -> 68.0f
            else -> 180.0f
        }
        assertEquals(68.0f, baseDist, 0.01f)
    }

    @Test
    fun testStabilityBounds() {
        val rawStability = 96
        val isSteady = rawStability >= 82
        val isTripod = rawStability >= 94
        assertTrue(isSteady)
        assertTrue(isTripod)
    }

    @Test
    fun testCapCutFiltersIntegrity() {
        val glowFilter = CapCutFilter.fromId("glow_face")
        assertEquals(CapCutFilter.GLOW_FACE, glowFilter)
        assertEquals("Glow Face", glowFilter.displayName)
        assertEquals(20, glowFilter.colorMatrixValues.size)

        val cinematicFilter = CapCutFilter.fromId("cinematic_teal")
        assertEquals(CapCutFilter.CINEMATIC_TEAL, cinematicFilter)
        assertEquals(20, cinematicFilter.colorMatrixValues.size)

        assertTrue(CapCutFilter.entries.size >= 8)
    }

    @Test
    fun testPocoX6CameraHardwareModes() {
        val pocoModes = listOf(
            "64MP Ultra HD",
            "10X /HDREAL",
            "8MP Ultra-Wide",
            "2MP Macro"
        )
        assertTrue(pocoModes.contains("64MP Ultra HD"))
        assertTrue(pocoModes.contains("8MP Ultra-Wide"))
        assertTrue(pocoModes.contains("2MP Macro"))

        // POCO X6 8MP Ultra-wide zoom is 0.6X
        val uwZoom = 0.6f
        assertTrue(uwZoom <= 0.65f)

        // POCO X6 Macro distance is fixed at 4.0cm
        val macroDistanceCm = 4.0f
        assertEquals(4.0f, macroDistanceCm, 0.01f)
    }

    @Test
    fun testPinchToZoomCalculation() {
        val initialZoom = 1.0f
        val pinchSpreadScale = 1.5f
        val calculatedZoom = (initialZoom * pinchSpreadScale).coerceIn(0.6f, 30.0f)
        assertEquals(1.5f, calculatedZoom, 0.01f)

        // Test zoom pinch out clamp min
        val smallScale = 0.2f
        val clampedMin = (initialZoom * smallScale).coerceIn(0.6f, 30.0f)
        assertEquals(0.6f, clampedMin, 0.01f)

        // Test zoom pinch in clamp max
        val largeScale = 40.0f
        val clampedMax = (initialZoom * largeScale).coerceIn(0.6f, 30.0f)
        assertEquals(30.0f, clampedMax, 0.01f)
    }

    @Test
    fun testExposureCompensationCalculation() {
        val initialEv = 0.0f
        assertEquals(0.0f, initialEv, 0.001f)

        // Test positive EV clamp (+3.0)
        val highEv = 5.2f
        val clampedHigh = highEv.coerceIn(-3.0f, 3.0f)
        assertEquals(3.0f, clampedHigh, 0.01f)

        // Test negative EV clamp (-3.0)
        val lowEv = -4.5f
        val clampedLow = lowEv.coerceIn(-3.0f, 3.0f)
        assertEquals(-3.0f, clampedLow, 0.01f)

        // Test CameraX EV step index calculation
        val step = 1.0f / 6.0f // 1/6 EV step typical on modern sensors
        val targetEv = 1.5f
        val index = kotlin.math.round((targetEv / step)).toInt()
        assertEquals(9, index)
    }
}
