package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import kotlin.math.abs

@Composable
fun CompositionGridOverlay(
    isGridEnabled: Boolean,
    isLevelerEnabled: Boolean,
    rollAngleDegrees: Float,
    isLevelStraight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("composition_grid_overlay")
            .fillMaxSize()
    ) {
        // 1. Rule of Thirds 3x3 Grid
        if (isGridEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val lineColor = Color.White.copy(alpha = 0.22f)
                val strokeW = 1.dp.toPx()

                // Vertical lines at 33.3% and 66.6%
                drawLine(
                    color = lineColor,
                    start = Offset(w * 0.3333f, 0f),
                    end = Offset(w * 0.3333f, h),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = lineColor,
                    start = Offset(w * 0.6666f, 0f),
                    end = Offset(w * 0.6666f, h),
                    strokeWidth = strokeW
                )

                // Horizontal lines at 33.3% and 66.6%
                drawLine(
                    color = lineColor,
                    start = Offset(0f, h * 0.3333f),
                    end = Offset(w, h * 0.3333f),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = lineColor,
                    start = Offset(0f, h * 0.6666f),
                    end = Offset(w, h * 0.6666f),
                    strokeWidth = strokeW
                )

                // Golden intersection cross marks at the 4 Rule-of-Thirds sweet spots
                val points = listOf(
                    Offset(w * 0.3333f, h * 0.3333f),
                    Offset(w * 0.6666f, h * 0.3333f),
                    Offset(w * 0.3333f, h * 0.6666f),
                    Offset(w * 0.6666f, h * 0.6666f)
                )

                val crossLen = 6.dp.toPx()
                points.forEach { pt ->
                    drawCircle(
                        color = CyberGold.copy(alpha = 0.65f),
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )
                    drawLine(
                        color = CyberGold.copy(alpha = 0.45f),
                        start = Offset(pt.x - crossLen, pt.y),
                        end = Offset(pt.x + crossLen, pt.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = CyberGold.copy(alpha = 0.45f),
                        start = Offset(pt.x, pt.y - crossLen),
                        end = Offset(pt.x, pt.y + crossLen),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // 2. Virtual Horizon Spirit Level (Center Tilt Gauge)
        if (isLevelerEnabled) {
            val levelColor by animateColorAsState(
                targetValue = if (isLevelStraight) NeonGreen else CyberGold.copy(alpha = 0.75f),
                label = "levelColor"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp),
                contentAlignment = Alignment.Center
            ) {
                // Horizon Level Bar rotating with phone angle
                Canvas(
                    modifier = Modifier
                        .fillMaxSize(0.45f)
                        .rotate(-rollAngleDegrees)
                ) {
                    val cw = size.width
                    val ch = size.height
                    val cy = ch / 2f

                    // Left wing
                    drawLine(
                        color = levelColor,
                        start = Offset(cw * 0.15f, cy),
                        end = Offset(cw * 0.40f, cy),
                        strokeWidth = if (isLevelStraight) 2.5.dp.toPx() else 1.5.dp.toPx()
                    )

                    // Right wing
                    drawLine(
                        color = levelColor,
                        start = Offset(cw * 0.60f, cy),
                        end = Offset(cw * 0.85f, cy),
                        strokeWidth = if (isLevelStraight) 2.5.dp.toPx() else 1.5.dp.toPx()
                    )

                    // Center level indicator circle
                    drawCircle(
                        color = levelColor,
                        radius = if (isLevelStraight) 4.dp.toPx() else 3.dp.toPx(),
                        center = Offset(cw / 2f, cy)
                    )
                }

                // Level Degree Badge
                Box(
                    modifier = Modifier
                        .offset(y = 26.dp)
                        .background(ObsidianBlack.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .border(
                            0.5.dp,
                            if (isLevelStraight) NeonGreen.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isLevelStraight) "0° LEVEL" else "${if (rollAngleDegrees > 0) "+" else ""}${rollAngleDegrees}°",
                        color = if (isLevelStraight) NeonGreen else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
