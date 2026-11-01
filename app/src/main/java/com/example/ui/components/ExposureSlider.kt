package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextHigh
import com.example.ui.theme.TextMedium
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Camera Exposure Compensation Slider providing manual brightness adjustment (-3.0 EV to +3.0 EV).
 * Appears on long-press with dynamic sun glow, zero-EV snap milestone, and quick-step buttons.
 */
@Composable
fun ExposureSlider(
    isVisible: Boolean,
    exposureEv: Float,
    anchorPosition: Offset?,
    onExposureChanged: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onStepHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    // Clamp card position to ensure it remains nicely on screen
    val cardWidthDp = 76.dp
    val cardHeightDp = 270.dp
    val cardWidthPx = with(density) { cardWidthDp.toPx() }
    val cardHeightPx = with(density) { cardHeightDp.toPx() }

    val offsetX = anchorPosition?.let { pos ->
        // If long press is on right half, show to the left of the tap; else to the right
        val targetX = if (pos.x > screenWidthPx / 2f) {
            (pos.x - cardWidthPx - 30.dp.value).coerceAtLeast(16.dp.value)
        } else {
            (pos.x + 40.dp.value).coerceAtMost(screenWidthPx - cardWidthPx - 16.dp.value)
        }
        targetX.roundToInt()
    } ?: with(density) { (screenWidthPx - cardWidthPx - 20.dp.toPx()).roundToInt() }

    val offsetY = anchorPosition?.let { pos ->
        (pos.y - (cardHeightPx / 2f))
            .coerceIn(80.dp.value, screenHeightPx - cardHeightPx - 160.dp.value)
            .roundToInt()
    } ?: with(density) { 140.dp.toPx().roundToInt() }

    val sunAlpha by animateFloatAsState(
        targetValue = ((exposureEv + 3.0f) / 6.0f).coerceIn(0.4f, 1.0f),
        label = "sun_brightness_glow"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .width(cardWidthDp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkGlassCard.copy(alpha = 0.92f),
                            ObsidianBlack.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(1.5.dp, DarkGlassBorder.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .testTag("exposure_compensation_card")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header: Dismiss Button & EV value readout
                Row(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onDismiss() }
                            .testTag("dismiss_exposure_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Exposure Slider",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Sun Icon with animated ray glow
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            (if (exposureEv >= 0f) CyberGold else NeonCyan).copy(alpha = 0.15f * sunAlpha)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Sun Exposure Level",
                        tint = if (exposureEv >= 0f) CyberGold else NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // EV Readout Text (+1.3 EV / 0.0 EV / -1.2 EV)
                Text(
                    text = (if (exposureEv > 0f) "+" else "") +
                            String.format(Locale.US, "%.1f", exposureEv) + " EV",
                    color = when {
                        exposureEv > 0.1f -> CyberGold
                        exposureEv < -0.1f -> NeonCyan
                        else -> TextHigh
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("exposure_ev_value_text")
                )

                // Quick Increase (+) Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            val next = (exposureEv + 0.3f).coerceIn(-3.0f, 3.0f)
                            val rounded = (next * 10f).roundToInt() / 10f
                            onExposureChanged(rounded)
                            onStepHaptic()
                        }
                        .testTag("exposure_increase_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase Exposure",
                        tint = CyberGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Vertical Interactive Track
                val trackHeightDp = 110.dp
                val trackHeightPx = with(density) { trackHeightDp.toPx() }
                var dragAccumulator by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(trackHeightDp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(13.dp))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragAccumulator = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    // Dragging UP decreases Y, so it INCREASES exposure (+)
                                    // Dragging DOWN increases Y, so it DECREASES exposure (-)
                                    dragAccumulator -= dragAmount
                                    val deltaEv = (dragAccumulator / (trackHeightPx / 6.0f))
                                    if (kotlin.math.abs(deltaEv) >= 0.1f) {
                                        val newEv = (exposureEv + deltaEv).coerceIn(-3.0f, 3.0f)
                                        val rounded = (newEv * 10f).roundToInt() / 10f
                                        if (rounded != exposureEv) {
                                            val oldSign = exposureEv >= 0f
                                            val newSign = rounded >= 0f
                                            onExposureChanged(rounded)
                                            if (oldSign != newSign || rounded == 0.0f) {
                                                onStepHaptic()
                                            }
                                        }
                                        dragAccumulator = 0f
                                    }
                                }
                            )
                        }
                        .testTag("exposure_vertical_track"),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Zero Tick Mark
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.5.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    // Zero Mark horizontal notch
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(Color.White.copy(alpha = 0.5f))
                    )

                    // Draggable Indicator Pill (Thumb)
                    // EV range is -3.0f to +3.0f; normalized to -1f (bottom) to +1f (top)
                    val normalizedEv = (exposureEv / 3.0f).coerceIn(-1.0f, 1.0f)
                    val thumbOffsetYDp = with(density) {
                        // -normalizedEv because dragging up is negative Y
                        ((-normalizedEv * (trackHeightPx / 2f - 14.dp.toPx())).toDp())
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = thumbOffsetYDp)
                            .size(width = 22.dp, height = 14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (exposureEv >= 0f) CyberGold else NeonCyan)
                            .border(1.dp, Color.White, RoundedCornerShape(7.dp))
                    )
                }

                // Quick Decrease (-) Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            val next = (exposureEv - 0.3f).coerceIn(-3.0f, 3.0f)
                            val rounded = (next * 10f).roundToInt() / 10f
                            onExposureChanged(rounded)
                            onStepHaptic()
                        }
                        .testTag("exposure_decrease_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease Exposure",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Reset to 0.0 EV Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            onReset()
                            onStepHaptic()
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("exposure_reset_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Exposure to Zero",
                            tint = TextMedium,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RESET",
                            color = TextMedium,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
