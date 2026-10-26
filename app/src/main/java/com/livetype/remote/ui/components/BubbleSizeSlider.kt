package com.livetype.remote.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.livetype.remote.ui.theme.LiveTypeTokens
import kotlin.math.roundToInt

/**
 * The bubble-size control.
 *
 * Reworked from the stock slider into the same visual system Apple uses for the
 * Control Center brightness and volume sliders (see `docs/` note in the report): a thin,
 * fully rounded track whose leading part is filled with the accent colour, a taller white
 * pill thumb that floats above it on a soft shadow, and a thumb that grows a little while
 * it is held. The motion of the growth is a critically damped spring — it settles, never
 * bounces — and it is skipped when the system animation scale is zero.
 *
 * Functionality is unchanged: same [valueRange], same integer dp values, same callback.
 */
@Composable
fun BubbleSizeSlider(
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val animationsOn = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }

    var widthPx by remember { mutableIntStateOf(0) }
    var pressed by remember { mutableStateOf(false) }

    val thumbWidth by animateDpAsState(
        targetValue = if (pressed) ThumbWidthPressed else ThumbWidth,
        animationSpec = if (animationsOn) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        },
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (pressed) ThumbHeightPressed else ThumbHeight,
        animationSpec = if (animationsOn) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        },
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) ThumbElevationPressed else ThumbElevation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
    )

    val min = valueRange.first
    val max = valueRange.last
    val fraction = SliderGeometry.fractionOf(value, min, max)

    // While a finger is down the thumb follows the finger through this local float, so the
    // position on screen never waits for a state round trip through the view model.
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(fraction) }
    val shown = if (dragging) dragFraction else fraction

    val thumbWidthPx = with(density) { thumbWidth.toPx() }
    // Read inside the gesture: the thumb grows while it is held, and the drag must follow
    // the finger with the live width instead of restarting the gesture when it changes.
    val liveThumbWidthPx = rememberUpdatedState(thumbWidthPx)

    val thumbCentreX = if (widthPx > 0) {
        SliderGeometry.thumbCentrePx(widthPx.toFloat(), thumbWidthPx, shown)
    } else {
        thumbWidthPx / 2f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ControlHeight)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(widthPx, min, max) {
                if (widthPx <= 0) return@pointerInput
                awaitEachGesture {
                    // One axis, no mirroring: right is always more, left is always less.
                    fun track(x: Float) {
                        val thumb = liveThumbWidthPx.value
                        dragFraction = SliderGeometry.fractionAt(x, widthPx.toFloat(), thumb)
                        onValueChange(
                            SliderGeometry.valueAt(x, widthPx.toFloat(), thumb, min, max)
                        )
                    }
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    dragging = true
                    dragFraction = fraction
                    track(down.position.x)
                    var tracking = true
                    while (tracking) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        when {
                            change == null -> tracking = false
                            !change.pressed -> {
                                change.consume()
                                pressed = false
                                dragging = false
                                tracking = false
                            }

                            else -> {
                                track(change.position.x)
                                change.consume()
                            }
                        }
                    }
                    pressed = false
                    dragging = false
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, min.toFloat()..max.toFloat(), 0)
                setProgress { target ->
                    onValueChange(target.roundToInt().coerceIn(min, max))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(TrackHeight)) {
            // The track spans exactly the distance the thumb can travel, so no grey stub
            // is ever left stranded under the ends of the pill.
            val inset = thumbWidthPx / 2f
            val trackWidth = (size.width - thumbWidthPx).coerceAtLeast(0f)
            if (trackWidth > 0f) {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(
                    color = LiveTypeTokens.BorderSubtle,
                    topLeft = Offset(inset, 0f),
                    size = Size(trackWidth, size.height),
                    cornerRadius = radius,
                )
                // The filled part grows from the left edge to the thumb, like the thumb
                // itself: one direction, no mirroring.
                val filled = (shown * trackWidth).coerceIn(0f, trackWidth)
                if (filled > 0f) {
                    drawRoundRect(
                        color = LiveTypeTokens.AccentPeriwinkleDeep,
                        topLeft = Offset(inset, 0f),
                        size = Size(filled, size.height),
                        cornerRadius = radius,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset((thumbCentreX - thumbWidthPx / 2f).roundToInt(), 0) }
                .size(thumbWidth, thumbHeight)
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(percent = 50),
                    clip = false,
                )
                .background(Color.White, RoundedCornerShape(percent = 50)),
        )
    }
}

private val ControlHeight: Dp = 44.dp
private val TrackHeight: Dp = 8.dp
private val ThumbWidth: Dp = 17.dp
private val ThumbWidthPressed: Dp = 21.dp
private val ThumbHeight: Dp = 27.dp
private val ThumbHeightPressed: Dp = 31.dp
private val ThumbElevation: Dp = 3.dp
private val ThumbElevationPressed: Dp = 8.dp
