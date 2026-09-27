package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.WarningRed
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SniperStabilizerRing(
    stabilityScore: Int, // 0..100
    isSniperSteady: Boolean,
    driftOffsetX: Float,
    driftOffsetY: Float,
    zoomLevel: Float,
    modifier: Modifier = Modifier
) {
    // Only display when zoom >= 5X
    if (zoomLevel < 4.5f) return

    val statusColor by animateColorAsState(
        targetValue = when {
            stabilityScore >= 82 -> NeonGreen
            stabilityScore >= 50 -> CyberGold
            else -> WarningRed
        },
        animationSpec = tween(250),
        label = "reticle_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sniper_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sniper_rotation"
    )

    Box(
        modifier = modifier
            .testTag("sniper_stabilizer_ring")
            .size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Reticle Canvas
        Canvas(modifier = Modifier.size(220.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.78f * if (isSniperSteady) pulseScale else 1f

            // Outer Dashed Range Ring
            drawCircle(
                color = statusColor.copy(alpha = 0.45f),
                radius = radius,
                center = center,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                )
            )

            // Inner Fine Ring
            drawCircle(
                color = statusColor.copy(alpha = 0.85f),
                radius = radius * 0.45f,
                center = center,
                style = Stroke(width = 2f)
            )

            // 4 Sniper Crosshair Corner Brackets
            val bracketLength = 24f
            val angles = listOf(45.0, 135.0, 225.0, 315.0)
            for (deg in angles) {
                val rad = Math.toRadians(deg + (if (isSniperSteady) 0.0 else rotationAngle.toDouble() * 0.1))
                val bx = center.x + (radius * cos(rad)).toFloat()
                val by = center.y + (radius * sin(rad)).toFloat()
                drawCircle(
                    color = statusColor,
                    radius = 3.5f,
                    center = Offset(bx, by)
                )
            }

            // Crosshair Hash Ticks
            val crosshairSize = 18f
            // Top
            drawLine(
                color = statusColor,
                start = Offset(center.x, center.y - radius * 0.45f),
                end = Offset(center.x, center.y - radius * 0.45f - crosshairSize),
                strokeWidth = 2.5f
            )
            // Bottom
            drawLine(
                color = statusColor,
                start = Offset(center.x, center.y + radius * 0.45f),
                end = Offset(center.x, center.y + radius * 0.45f + crosshairSize),
                strokeWidth = 2.5f
            )
            // Left
            drawLine(
                color = statusColor,
                start = Offset(center.x - radius * 0.45f, center.y),
                end = Offset(center.x - radius * 0.45f - crosshairSize, center.y),
                strokeWidth = 2.5f
            )
            // Right
            drawLine(
                color = statusColor,
                start = Offset(center.x + radius * 0.45f, center.y),
                end = Offset(center.x + radius * 0.45f + crosshairSize, center.y),
                strokeWidth = 2.5f
            )

            // Center Dynamic Gyro Target Dot (Compensated)
            val gyroCenter = Offset(
                center.x + (driftOffsetX * 2.2f).coerceIn(-radius * 0.35f, radius * 0.35f),
                center.y + (driftOffsetY * 2.2f).coerceIn(-radius * 0.35f, radius * 0.35f)
            )
            drawCircle(
                color = statusColor,
                radius = if (isSniperSteady) 6f else 4f,
                center = gyroCenter
            )
        }

        // Stability Status Pill below reticle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset { IntOffset(0, 140) }
                .background(ObsidianBlack.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            val statusText = when {
                stabilityScore >= 82 -> "SNIPER LOCKED $stabilityScore%"
                stabilityScore >= 50 -> "STABILIZING $stabilityScore%"
                else -> "HOLD STEADY $stabilityScore%"
            }
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}
