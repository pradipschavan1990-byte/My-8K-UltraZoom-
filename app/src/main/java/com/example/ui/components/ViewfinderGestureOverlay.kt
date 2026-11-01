package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextHigh
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Viewfinder gesture detector providing intuitive multi-touch pinch-to-zoom
 * and tap-to-focus on the camera preview area.
 * Controls the zoom bar synchronously and renders a sleek floating zoom badge.
 */
@Composable
fun ViewfinderGestureOverlay(
    currentZoom: Float,
    onZoomChanged: (Float) -> Unit,
    onStepHaptic: () -> Unit,
    onTapToFocus: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentZoomState by rememberUpdatedState(currentZoom)
    var isPinching by remember { mutableStateOf(false) }
    var showPinchBadge by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var hideBadgeJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var accumulatedZoom = currentZoomState
                var lastCommittedZoom = currentZoomState

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var zoomStarted = false
                    var pastSlop = false
                    var isLongPressed = false
                    val touchSlop = viewConfiguration.touchSlop
                    val initialPos = down.position

                    // Launch long-press timer
                    val longPressJob = coroutineScope.launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        if (!pastSlop && !zoomStarted) {
                            isLongPressed = true
                            onLongPress(initialPos)
                            onStepHaptic()
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) {
                            longPressJob.cancel()
                            break
                        }

                        val pressedPointers = event.changes.filter { it.pressed }
                        if (pressedPointers.size >= 2) {
                            longPressJob.cancel()
                            if (!zoomStarted) {
                                zoomStarted = true
                                pastSlop = true
                                accumulatedZoom = currentZoomState
                                lastCommittedZoom = currentZoomState
                                isPinching = true
                                showPinchBadge = true
                                hideBadgeJob?.cancel()
                            }

                            val zoomDelta = event.calculateZoom()
                            if (zoomDelta != 1.0f && zoomDelta.isFinite()) {
                                accumulatedZoom = (accumulatedZoom * zoomDelta).coerceIn(0.6f, 30.0f)
                                val rounded = (accumulatedZoom * 10f).roundToInt() / 10f
                                if (rounded != lastCommittedZoom) {
                                    val oldIntStep = (lastCommittedZoom * 10).toInt() / 10
                                    val newIntStep = (rounded * 10).toInt() / 10
                                    lastCommittedZoom = rounded
                                    onZoomChanged(rounded)
                                    if (oldIntStep != newIntStep) {
                                        onStepHaptic()
                                    }
                                }
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pressedPointers.size == 1 && !zoomStarted) {
                            val moveDist = (pressedPointers[0].position - initialPos).getDistance()
                            if (moveDist > touchSlop) {
                                pastSlop = true
                                longPressJob.cancel()
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    longPressJob.cancel()

                    if (zoomStarted) {
                        isPinching = false
                        hideBadgeJob?.cancel()
                        hideBadgeJob = coroutineScope.launch {
                            delay(900)
                            showPinchBadge = false
                        }
                    } else if (!pastSlop && !isLongPressed) {
                        onTapToFocus(initialPos)
                    }
                }
            }
            .testTag("viewfinder_gesture_overlay")
    ) {
        // Floating live zoom pill indicator when pinching
        AnimatedVisibility(
            visible = isPinching || showPinchBadge,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 120.dp)
        ) {
            val isTelephoto = currentZoom >= 10f
            val accentColor = if (isTelephoto) CyberGold else NeonCyan
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(1.5.dp, accentColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("floating_pinch_zoom_badge"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Pinch Zoom Active",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", currentZoom)}X",
                        color = TextHigh,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when {
                            currentZoom >= 10f -> "10X TELEPHOTO"
                            currentZoom <= 0.65f -> "0.6X ULTRA-WIDE"
                            else -> "WIDE SENSOR"
                        },
                        color = accentColor.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
