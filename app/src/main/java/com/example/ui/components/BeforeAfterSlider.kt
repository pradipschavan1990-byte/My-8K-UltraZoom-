package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = modifier
            .testTag("before_after_slider")
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBlack)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val newFraction = (splitFraction + dragAmount / size.width).coerceIn(0.05f, 0.95f)
                        splitFraction = newFraction
                    }
                }
        ) {
            val splitX = size.width * splitFraction
            val beforeImageBitmap = beforeBitmap.asImageBitmap()
            val afterImageBitmap = afterBitmap.asImageBitmap()

            val canvasIntSize = IntSize(size.width.roundToInt(), size.height.roundToInt())

            // Draw After (Enhanced) on the right side
            clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
                drawImage(
                    image = afterImageBitmap,
                    dstSize = canvasIntSize
                )
            }

            // Draw Before (RAW) on the left side
            clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                drawImage(
                    image = beforeImageBitmap,
                    dstSize = canvasIntSize
                )
            }

            // Draw Divider Line
            drawLine(
                color = Color.White,
                start = Offset(splitX, 0f),
                end = Offset(splitX, size.height),
                strokeWidth = 3f
            )

            // Draw Split Handle Dot
            drawCircle(
                color = NeonCyan,
                radius = 16f,
                center = Offset(splitX, size.height / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = Offset(splitX, size.height / 2f)
            )
        }

        // Before Badge (Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(ObsidianBlack.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "BEFORE (RAW)",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // After Badge (Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(ObsidianBlack.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "AFTER (HDREAL 2.0)",
                color = CyberGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Drag Hint at Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .background(DarkGlassSurface.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "DRAG TO COMPARE",
                color = NeonCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
