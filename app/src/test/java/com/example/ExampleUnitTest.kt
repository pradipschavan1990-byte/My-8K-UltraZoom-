package com.example

import org.junit.Assert.assertEquals
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
}
