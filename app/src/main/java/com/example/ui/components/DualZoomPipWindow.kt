package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack

@Composable
fun DualZoomPipWindow(
    isVisible: Boolean,
    currentZoom: Float,
    isMoonMode: Boolean,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && currentZoom >= 2.0f,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .testTag("dual_zoom_pip_window")
                .size(width = 110.dp, height = 135.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkGlassSurface.copy(alpha = 0.88f))
                .border(BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.7f)), RoundedCornerShape(10.dp))
        ) {
            // Background 1X Preview Image
            val sampleRes = if (isMoonMode) R.drawable.sample_moon else R.drawable.sample_eagle
            Image(
                painter = painterResource(id = sampleRes),
                contentDescription = "Dual Zoom 1X Wide Field View",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack.copy(alpha = 0.35f))
            )

            // Dynamic Target Frame Box: shrinks as zoom increases!
            // At 2X: 50% box, at 10X: 18% box, at 30X: 8% box
            val boxRatio = (1.0f / currentZoom.coerceIn(1f, 30f)).coerceIn(0.1f, 0.65f)
            val boxWidthDp = (100 * boxRatio).dp
            val boxHeightDp = (120 * boxRatio).dp

            Box(
                modifier = Modifier
                    .size(width = boxWidthDp, height = boxHeightDp)
                    .align(Alignment.Center)
                    .border(BorderStroke(1.5.dp, CyberGold), RoundedCornerShape(2.dp))
                    .background(CyberGold.copy(alpha = 0.15f))
            )

            // Header Tag: 1X OVERVIEW
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(ObsidianBlack.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "1X DUAL VIEW",
                    color = NeonCyan,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Target Zoom indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .background(ObsidianBlack.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = String.format("%.1fX TARGET", currentZoom),
                    color = CyberGold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Close PiP button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .testTag("close_pip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Dual Zoom View",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
