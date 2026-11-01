package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonCyan
import kotlin.math.roundToInt

@Composable
fun ZoomDialSlider(
    zoomLevel: Float,
    isMacroActive: Boolean = false,
    onZoomChanged: (Float) -> Unit,
    onMacroClicked: () -> Unit = {},
    onQuickSwitch: () -> Unit = {},
    onStepHaptic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Ultra-Compact Single Row Zoom Bar (Removed large 1x/2x/10x preset boxes per user request)
    Row(
        modifier = modifier
            .testTag("zoom_dial_slider_container")
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Min zoom label
        Text(
            text = "0.6X",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // Slim Zoom Slider
        Slider(
            value = zoomLevel,
            onValueChange = { newVal ->
                val rounded = (newVal * 10f).roundToInt() / 10f
                if ((rounded * 10).toInt() % 10 == 0) {
                    onStepHaptic()
                }
                onZoomChanged(rounded)
            },
            valueRange = 0.6f..30.0f,
            colors = SliderDefaults.colors(
                thumbColor = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                activeTrackColor = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .testTag("continuous_zoom_slider")
        )

        // Compact Zoom Badge (Clickable to quickly toggle 1X <-> 10X)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(
                    width = 1.dp,
                    color = (if (zoomLevel >= 10f) CyberGold else NeonCyan).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable {
                    onQuickSwitch()
                    onStepHaptic()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("quick_switch_1x_10x"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%.1fX", zoomLevel),
                color = if (zoomLevel >= 10f) CyberGold else NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
