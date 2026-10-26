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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
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
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
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
    val span = (max - min).coerceAtLeast(1)
    val fraction = ((value - min).toFloat() / span).coerceIn(0f, 1f)
    val shown = if (rtl) 1f - fraction else fraction

    val thumbWidthPx = with(density) { thumbWidth.toPx() }
    // Read inside the gesture: the thumb grows while it is held, and the drag must follow
    // the finger with the live width instead of restarting the gesture when it changes.
    val liveThumbWidthPx = rememberUpdatedState(thumbWidthPx)

    val travel = (widthPx - thumbWidthPx).coerceAtLeast(0f)
    val thumbCentreX = thumbWidthPx / 2f + shown * travel

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ControlHeight)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(widthPx, rtl, min, max) {
                if (widthPx <= 0) return@pointerInput
                awaitEachGesture {
                    // Minimum on the leading side, maximum on the trailing one — the same
                    // direction people already expect from every slider they know.
                    fun valueAt(x: Float): Int {
                        val usable = (widthPx - liveThumbWidthPx.value).coerceAtLeast(1f)
                        val raw = ((x - liveThumbWidthPx.value / 2f) / usable).coerceIn(0f, 1f)
                        val directed = if (rtl) 1f - raw else raw
                        return (min + (directed * span).roundToInt()).coerceIn(min, max)
                    }
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    onValueChange(valueAt(down.position.x))
                    var tracking = true
                    while (tracking) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        when {
                            change == null -> tracking = false
                            !change.pressed -> {
                                change.consume()
                                pressed = false
                                tracking = false
                            }

                            else -> {
                                onValueChange(valueAt(change.position.x))
                                change.consume()
                            }
                        }
                    }
                    pressed = false
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
                // Filled part: always grows away from the leading edge, so it reads the
                // same in Arabic (right to left) as it does in English.
                val filled = (shown * trackWidth).coerceIn(0f, trackWidth)
                if (filled > 0f) {
                    drawRoundRect(
                        color = LiveTypeTokens.AccentPeriwinkleDeep,
                        topLeft = if (rtl) {
                            Offset(inset + trackWidth - filled, 0f)
                        } else {
                            Offset(inset, 0f)
                        },
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
