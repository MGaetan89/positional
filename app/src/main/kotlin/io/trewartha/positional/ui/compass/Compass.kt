package io.trewartha.positional.ui.compass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.North
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.google.android.material.color.MaterialColors.harmonize
import io.trewartha.positional.R
import io.trewartha.positional.ui.PositionalTheme
import io.trewartha.positional.ui.utils.placeholder
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Compass(
    azimuthDegrees: Float?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.placeholder(visible = azimuthDegrees == null),
        contentAlignment = Alignment.Center
    ) {
        CompassReading(azimuthDegrees ?: 0f)
        CompassRose(azimuthDegrees ?: 0f, Modifier.fillMaxSize())
    }
}

@Composable
private fun CompassReading(azimuthDegrees: Float, modifier: Modifier = Modifier) {
    ConstraintLayout(modifier) {
        val (arrowIcon, degreesText, symbolText, directionText) = createRefs()
        val innerContentChain = createVerticalChain(arrowIcon, degreesText, directionText)
        constrain(innerContentChain) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        }
        Icon(
            Icons.Rounded.North,
            contentDescription = null,
            modifier = Modifier
                .constrainAs(arrowIcon) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        DegreesText(
            azimuthDegrees = azimuthDegrees,
            modifier = Modifier.constrainAs(degreesText) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )
        DegreeSymbolText(
            modifier = Modifier.constrainAs(symbolText) {
                top.linkTo(degreesText.top)
                start.linkTo(degreesText.end)
            }
        )
        DirectionText(
            azimuthDegrees = azimuthDegrees,
            modifier = Modifier.constrainAs(directionText) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
        )
    }
}

@Composable
private fun CompassRose(
    azimuthDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val northTickColor =
        Color(harmonize(Color.Red.toArgb(), MaterialTheme.colorScheme.primary.toArgb()))
    val cardinalTickColor = MaterialTheme.colorScheme.onSurface
    val majorTickColor = MaterialTheme.colorScheme.onSurface
    val minorTickColor = majorTickColor.copy(alpha = 0.3f)

    val displayDensity = LocalDensity.current

    val northTickLengthPx = remember { with(displayDensity) { TICK_NORTH_LENGTH.toPx() } }
    val cardinalTickLengthPx = remember { with(displayDensity) { TICK_CARDINAL_LENGTH.toPx() } }
    val majorTickLengthPx = remember { with(displayDensity) { TICK_MAJOR_LENGTH.toPx() } }
    val minorTickLengthPx = remember { with(displayDensity) { TICK_MINOR_LENGTH.toPx() } }
    val northTickWidthPx = remember { with(displayDensity) { TICK_NORTH_WIDTH.toPx() } }
    val cardinalTickWidthPx = remember { with(displayDensity) { TICK_CARDINAL_WIDTH.toPx() } }
    val majorTickWidthPx = remember { with(displayDensity) { TICK_MAJOR_WIDTH.toPx() } }
    val minorTickWidthPx = remember { with(displayDensity) { TICK_MINOR_WIDTH.toPx() } }

    val northTickStyle = remember {
        TickStyle(
            color = northTickColor,
            lengthPx = northTickLengthPx,
            widthPx = northTickWidthPx
        )
    }
    val cardinalTickStyle = remember {
        TickStyle(
            color = cardinalTickColor,
            lengthPx = cardinalTickLengthPx,
            widthPx = cardinalTickWidthPx
        )
    }
    val majorTickStyle = remember {
        TickStyle(
            color = majorTickColor,
            lengthPx = majorTickLengthPx,
            widthPx = majorTickWidthPx
        )
    }
    val minorTickStyle = remember {
        TickStyle(
            color = minorTickColor,
            lengthPx = minorTickLengthPx,
            widthPx = minorTickWidthPx
        )
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        // We'll animate the compass rose to the negative of the azimuth so that the rose exactly
        // counteracts the rotation of the Android device, keeping the rose pointed north. We're
        // careful to transition naturally across the 0°/360° boundary because we don't want the
        // rose to completely spin around each time we cross the boundary. The approach has been
        // adapted from the following StackOverflow answer:
        // https://stackoverflow.com/a/68259116/1253644
        val (lastAnimatedAzimuth, setLastAnimatedAzimuth) = remember { mutableFloatStateOf(0f) }
        var newAnimatedAzimuth = lastAnimatedAzimuth // We'll update this if necessary
        val modLastAnimatedAzimuth = if (lastAnimatedAzimuth > 0) {
            lastAnimatedAzimuth % DEGREES_360
        } else {
            DEGREES_360 - (-lastAnimatedAzimuth % DEGREES_360)
        }
        if (modLastAnimatedAzimuth != azimuthDegrees) {
            val clockwiseDiff = if (azimuthDegrees > modLastAnimatedAzimuth) {
                modLastAnimatedAzimuth + DEGREES_360 - azimuthDegrees
            } else {
                modLastAnimatedAzimuth - azimuthDegrees
            }
            val counterClockwiseDiff = if (azimuthDegrees > modLastAnimatedAzimuth) {
                azimuthDegrees - modLastAnimatedAzimuth
            } else {
                DEGREES_360 - modLastAnimatedAzimuth + azimuthDegrees
            }
            val rotateClockwise = clockwiseDiff < counterClockwiseDiff
            newAnimatedAzimuth = if (rotateClockwise) {
                lastAnimatedAzimuth - clockwiseDiff
            } else {
                lastAnimatedAzimuth + counterClockwiseDiff
            }

            setLastAnimatedAzimuth(newAnimatedAzimuth)
        }
        val animatedRotation: Float by animateFloatAsState(
            targetValue = -newAnimatedAzimuth,
            label = "Rotation"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2
            for (tickDegrees in 0..<DEGREES_360) {
                when {
                    tickDegrees == DEGREES_0 -> northTickStyle
                    tickDegrees % TICK_PERIOD_DEGREES_CARDINAL == 0 -> cardinalTickStyle
                    tickDegrees % TICK_PERIOD_DEGREES_MAJOR == 0 -> majorTickStyle
                    tickDegrees % TICK_PERIOD_DEGREES_MINOR == 0 -> minorTickStyle
                    else -> null
                }?.let { tickStyle ->
                    val combinedAngle =
                        (animatedRotation + tickDegrees) + COMPASS_CANVAS_ROTATION_DIFF
                    val cos = cos(combinedAngle.toRadians())
                    val sin = sin(combinedAngle.toRadians())

                    // End is the outermost point of the tick mark, start is the innermost point.
                    // Note that these should account for the extra length the cap adds to each end
                    // of the line.
                    val capLength = tickStyle.widthPx / 2f
                    val outerRadiusForLine = outerRadius - capLength
                    val end = center + Offset(cos * outerRadiusForLine, sin * outerRadiusForLine)
                    val start = end - Offset(cos * tickStyle.lengthPx, sin * tickStyle.lengthPx)

                    drawLine(
                        color = tickStyle.color,
                        start = start,
                        end = end,
                        strokeWidth = tickStyle.widthPx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun DegreesText(azimuthDegrees: Float, modifier: Modifier = Modifier) {
    Text(
        text = "${azimuthDegrees.roundToInt() % 360}",
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge
    )
}

@Composable
private fun DegreeSymbolText(modifier: Modifier = Modifier) {
    Text(
        text = "°",
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge
    )
}

@Composable
private fun DirectionText(azimuthDegrees: Float, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            when {
                AZIMUTH_NW_MIN <= azimuthDegrees && azimuthDegrees < AZIMUTH_NW_MAX ->
                    R.string.compass_direction_northwest
                AZIMUTH_NE_MIN <= azimuthDegrees && azimuthDegrees < AZIMUTH_NE_MAX ->
                    R.string.compass_direction_northeast
                AZIMUTH_SW_MIN <= azimuthDegrees && azimuthDegrees < AZIMUTH_SW_MAX ->
                    R.string.compass_direction_southwest
                AZIMUTH_SE_MIN <= azimuthDegrees && azimuthDegrees < AZIMUTH_SE_MAX ->
                    R.string.compass_direction_southeast
                AZIMUTH_E_MIN <= azimuthDegrees && azimuthDegrees < AZIMUTH_E_MAX ->
                    R.string.compass_direction_east
                AZIMUTH_S_MIN <= azimuthDegrees && azimuthDegrees < AZIMUTH_S_MAX ->
                    R.string.compass_direction_south
                AZIMUTH_W_MIN <= azimuthDegrees && azimuthDegrees < AZIMUTH_W_MAX ->
                    R.string.compass_direction_west
                else ->
                    R.string.compass_direction_north
            }
        ),
        modifier = modifier,
        style = MaterialTheme.typography.displaySmall
    )
}

private const val AZIMUTH_N_MIN = 337.5f
private const val AZIMUTH_N_MAX = 22.5f
private const val AZIMUTH_E_MIN = 67.5f
private const val AZIMUTH_E_MAX = 112.5f
private const val AZIMUTH_S_MIN = 157.5f
private const val AZIMUTH_S_MAX = 202.5f
private const val AZIMUTH_W_MIN = 247.5f
private const val AZIMUTH_W_MAX = 292.5f
private const val AZIMUTH_NE_MIN = AZIMUTH_N_MAX
private const val AZIMUTH_NE_MAX = AZIMUTH_E_MIN
private const val AZIMUTH_SE_MIN = AZIMUTH_E_MAX
private const val AZIMUTH_SE_MAX = AZIMUTH_S_MIN
private const val AZIMUTH_SW_MIN = AZIMUTH_S_MAX
private const val AZIMUTH_SW_MAX = AZIMUTH_W_MIN
private const val AZIMUTH_NW_MIN = AZIMUTH_W_MAX
private const val AZIMUTH_NW_MAX = AZIMUTH_N_MIN
private const val COMPASS_CANVAS_ROTATION_DIFF = -90f
private const val DEGREES_0 = 0
private const val DEGREES_180 = 180f
private const val DEGREES_360 = 360
private const val TICK_PERIOD_DEGREES_CARDINAL = 90
private const val TICK_PERIOD_DEGREES_MAJOR = 45
private const val TICK_PERIOD_DEGREES_MINOR = 15

private val TICK_NORTH_WIDTH = 24.dp
private val TICK_NORTH_LENGTH = 32.dp
private val TICK_CARDINAL_WIDTH = 12.dp
private val TICK_CARDINAL_LENGTH = 24.dp
private val TICK_MAJOR_WIDTH = 12.dp
private val TICK_MAJOR_LENGTH = 16.dp
private val TICK_MINOR_WIDTH = 8.dp
private val TICK_MINOR_LENGTH = 8.dp

private data class TickStyle(
    val color: Color,
    val lengthPx: Float,
    val widthPx: Float
)

private fun Float.toRadians(): Float = (this / DEGREES_180 * Math.PI).toFloat()

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun CompassPreview() {
    PositionalTheme {
        Surface(Modifier.size(600.dp, 300.dp)) {
            Compass(azimuthDegrees = 25f)
        }
    }
}
