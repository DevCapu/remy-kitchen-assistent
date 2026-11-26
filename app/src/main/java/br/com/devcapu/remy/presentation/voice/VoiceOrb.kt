package br.com.devcapu.remy.presentation.voice

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.devcapu.remy.R
import br.com.devcapu.remy.ui.theme.RemyTheme
import kotlin.math.PI
import kotlin.math.sin

const val VOICE_ORB_TEST_TAG = "voice_orb"
const val VOICE_ORB_STATUS_TEST_TAG = "voice_orb_status"

private val ORB_SIZE = 80.dp
private val ORB_ICON_SIZE = 28.dp
private const val MOTION_SMOOTHING_MS = 100
private const val LISTENING_CYCLE_MS = 1400
private const val SPEAKING_CYCLE_MS = 1100

@Composable
fun rememberPrefersReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun VoiceOrb(
    state: VoiceOrbUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = rememberPrefersReducedMotion(),
) {
    val colorScheme = MaterialTheme.colorScheme
    val motionTarget =
        resolveOrbMotionFactor(
            mode = state.mode,
            audioLevel = state.audioLevel,
            hasLiveAudioSignal = state.hasLiveAudioSignal,
            reduceMotion = reduceMotion,
        )

    val motionFactorState: State<Float> =
        if (reduceMotion) {
            remember { mutableFloatStateOf(0f) }
        } else {
            animateFloatAsState(
                targetValue = motionTarget,
                animationSpec = tween(durationMillis = MOTION_SMOOTHING_MS),
                label = "voiceOrbMotionFactor",
            )
        }

    val cyclePhaseState: State<Float> =
        if (!reduceMotion && state.mode != VoiceOrbMode.IDLE) {
            val duration =
                if (state.mode == VoiceOrbMode.LISTENING) {
                    LISTENING_CYCLE_MS
                } else {
                    SPEAKING_CYCLE_MS
                }
            val transition = rememberInfiniteTransition(label = "voiceOrbCycle")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = duration, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "voiceOrbPhase",
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }

    val orbContentDescription =
        when (state.mode) {
            VoiceOrbMode.IDLE -> stringResource(R.string.voice_orb_cd_idle)
            VoiceOrbMode.LISTENING -> stringResource(R.string.voice_orb_cd_listening)
            VoiceOrbMode.SPEAKING -> stringResource(R.string.voice_orb_cd_speaking)
        }
    val orbStateDescription =
        when (state.mode) {
            VoiceOrbMode.IDLE -> stringResource(R.string.voice_orb_state_idle)
            VoiceOrbMode.LISTENING -> stringResource(R.string.voice_orb_state_listening)
            VoiceOrbMode.SPEAKING -> stringResource(R.string.voice_orb_state_speaking)
        }
    val statusText =
        when (state.mode) {
            VoiceOrbMode.IDLE -> stringResource(R.string.voice_orb_status_idle)
            VoiceOrbMode.LISTENING -> stringResource(R.string.voice_orb_status_listening)
            VoiceOrbMode.SPEAKING -> stringResource(R.string.voice_orb_status_speaking)
        }

    val iconVector =
        when (state.mode) {
            VoiceOrbMode.IDLE -> Icons.Default.MicNone
            VoiceOrbMode.LISTENING -> Icons.Default.Mic
            VoiceOrbMode.SPEAKING -> Icons.Default.GraphicEq
        }
    val iconTint =
        when (state.mode) {
            VoiceOrbMode.IDLE -> colorScheme.onSurface
            VoiceOrbMode.LISTENING -> colorScheme.onPrimary
            VoiceOrbMode.SPEAKING -> colorScheme.onTertiary
        }
    val primaryColor = colorScheme.primary
    val secondaryColor = colorScheme.secondary
    val tertiaryColor = colorScheme.tertiary
    val surfaceVariantColor = colorScheme.surfaceVariant
    val statusTextColor =
        when (state.mode) {
            VoiceOrbMode.IDLE -> colorScheme.secondary
            VoiceOrbMode.LISTENING -> colorScheme.primary
            VoiceOrbMode.SPEAKING -> colorScheme.tertiary
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(ORB_SIZE)
                    .clip(CircleShape)
                    .testTag(VOICE_ORB_TEST_TAG)
                    .clickable(
                        role = Role.Button,
                        onClick = onClick,
                    ).semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = orbContentDescription
                        stateDescription = orbStateDescription
                        liveRegion = LiveRegionMode.Polite
                    },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(ORB_SIZE)) {
                val motion = motionFactorState.value
                val phase = cyclePhaseState.value
                when (state.mode) {
                    VoiceOrbMode.IDLE ->
                        drawIdleOrb(
                            primary = primaryColor,
                            secondary = secondaryColor,
                            surfaceVariant = surfaceVariantColor,
                        )
                    VoiceOrbMode.LISTENING ->
                        drawListeningOrb(
                            primary = primaryColor,
                            tertiary = tertiaryColor,
                            motion = motion,
                            phase = phase,
                            hasLiveAudioSignal = state.hasLiveAudioSignal,
                            reduceMotion = reduceMotion,
                        )
                    VoiceOrbMode.SPEAKING ->
                        drawSpeakingOrb(
                            primary = primaryColor,
                            tertiary = tertiaryColor,
                            motion = motion,
                            phase = phase,
                            reduceMotion = reduceMotion,
                        )
                }
            }

            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier =
                    Modifier
                        .size(ORB_ICON_SIZE)
                        .graphicsLayer {
                            val iconScale =
                                if (reduceMotion || state.mode == VoiceOrbMode.IDLE) {
                                    1f
                                } else {
                                    1f + (motionFactorState.value * 0.1f)
                                }
                            scaleX = iconScale
                            scaleY = iconScale
                        },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusTextColor,
            modifier = Modifier.testTag(VOICE_ORB_STATUS_TEST_TAG),
        )
    }
}

private fun DrawScope.drawIdleOrb(
    primary: Color,
    secondary: Color,
    surfaceVariant: Color,
) {
    val maxRadius = size.minDimension / 2f
    val coreRadius = maxRadius * 0.66f

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        primary.copy(alpha = 0.32f),
                        secondary.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                center = center,
                radius = maxRadius,
            ),
        radius = maxRadius,
        center = center,
    )

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        primary.copy(alpha = 0.45f),
                        surfaceVariant.copy(alpha = 0.85f),
                    ),
                center = center,
                radius = coreRadius,
            ),
        radius = coreRadius,
        center = center,
    )

    drawCircle(
        color = secondary.copy(alpha = 0.65f),
        radius = maxRadius * 0.84f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx()),
    )
}

private fun DrawScope.drawListeningOrb(
    primary: Color,
    tertiary: Color,
    motion: Float,
    phase: Float,
    hasLiveAudioSignal: Boolean,
    reduceMotion: Boolean,
) {
    val maxRadius = size.minDimension / 2f
    val breath =
        if (reduceMotion) {
            0f
        } else {
            ((sin(phase * 2.0 * PI) + 1.0) / 2.0).toFloat()
        }
    val effectiveAmplitude =
        when {
            reduceMotion -> 0f
            hasLiveAudioSignal -> (motion * 0.8f) + (breath * 0.2f)
            else -> 0.2f + (breath * 0.55f)
        }
    val haloRadius = (maxRadius * (0.76f + (effectiveAmplitude * 0.24f))).coerceAtMost(maxRadius)
    val coreRadius = maxRadius * (0.58f + (effectiveAmplitude * 0.08f))

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        primary.copy(alpha = 0.52f),
                        tertiary.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                center = center,
                radius = haloRadius,
            ),
        radius = haloRadius,
        center = center,
    )

    if (reduceMotion) {
        drawCircle(
            color = primary.copy(alpha = 0.85f),
            radius = maxRadius * 0.78f,
            center = center,
            style = Stroke(width = 2.5.dp.toPx()),
        )
        drawCircle(
            color = tertiary.copy(alpha = 0.55f),
            radius = maxRadius * 0.92f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    } else {
        val ring1Progress = phase
        val ring2Progress = (phase + 0.5f) % 1f
        val ringStrokeWidth = (2.dp + (1.5.dp * effectiveAmplitude)).toPx()

        drawRippleRing(
            color = primary,
            progress = ring1Progress,
            minRadius = coreRadius,
            maxRadius = maxRadius * 0.96f,
            strokeWidth = ringStrokeWidth,
        )
        drawRippleRing(
            color = tertiary,
            progress = ring2Progress,
            minRadius = coreRadius,
            maxRadius = maxRadius * 0.96f,
            strokeWidth = ringStrokeWidth * 0.85f,
        )
    }

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        primary,
                        tertiary.copy(alpha = 0.85f),
                    ),
                center = center,
                radius = coreRadius,
            ),
        radius = coreRadius,
        center = center,
    )
}

private fun DrawScope.drawSpeakingOrb(
    primary: Color,
    tertiary: Color,
    motion: Float,
    phase: Float,
    reduceMotion: Boolean,
) {
    val maxRadius = size.minDimension / 2f
    val waveA = if (reduceMotion) 0f else ((sin(phase * 2.0 * PI) + 1.0) / 2.0).toFloat()
    val waveB = if (reduceMotion) 0f else ((sin((phase + 0.5f) * 2.0 * PI) + 1.0) / 2.0).toFloat()
    val coreRadius = maxRadius * (0.58f + (waveA * motion * 0.09f))

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        tertiary.copy(alpha = 0.55f),
                        primary.copy(alpha = 0.25f),
                        Color.Transparent,
                    ),
                center = center,
                radius = maxRadius,
            ),
        radius = maxRadius,
        center = center,
    )

    val outerArcRadius = maxRadius * (0.86f + (waveA * 0.06f))
    val innerArcRadius = maxRadius * (0.74f + (waveB * 0.06f))
    val rotationOffset = if (reduceMotion) 0f else phase * 180f

    drawSpeakingArcs(
        color = tertiary.copy(alpha = 0.9f),
        radius = outerArcRadius,
        startOffset = rotationOffset,
        sweepAngle = 48f,
        strokeWidth = 2.5.dp.toPx(),
    )
    drawSpeakingArcs(
        color = primary.copy(alpha = 0.8f),
        radius = innerArcRadius,
        startOffset = -rotationOffset + 45f,
        sweepAngle = 40f,
        strokeWidth = 2.dp.toPx(),
    )

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        tertiary,
                        primary.copy(alpha = 0.9f),
                    ),
                center = center,
                radius = coreRadius,
            ),
        radius = coreRadius,
        center = center,
    )
}

private fun DrawScope.drawRippleRing(
    color: Color,
    progress: Float,
    minRadius: Float,
    maxRadius: Float,
    strokeWidth: Float,
) {
    val radius = minRadius + ((maxRadius - minRadius) * progress)
    val alpha = (1f - progress).coerceIn(0f, 1f) * 0.75f
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth),
    )
}

private fun DrawScope.drawSpeakingArcs(
    color: Color,
    radius: Float,
    startOffset: Float,
    sweepAngle: Float,
    strokeWidth: Float,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    for (segment in 0 until 4) {
        val startAngle = startOffset + (segment * 90f)
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Preview(name = "VoiceOrb - Idle", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VoiceOrbIdlePreview() {
    RemyTheme(darkTheme = true, dynamicColor = false) {
        Box(
            modifier =
                Modifier
                    .background(Color.Black)
                    .padding(16.dp),
        ) {
            VoiceOrb(
                state = VoiceOrbUiState(mode = VoiceOrbMode.IDLE),
                onClick = {},
                reduceMotion = false,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Preview(name = "VoiceOrb - Listening", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VoiceOrbListeningPreview() {
    RemyTheme(darkTheme = true, dynamicColor = false) {
        Box(
            modifier =
                Modifier
                    .background(Color.Black)
                    .padding(16.dp),
        ) {
            VoiceOrb(
                state =
                    VoiceOrbUiState(
                        mode = VoiceOrbMode.LISTENING,
                        audioLevel = 0.7f,
                        hasLiveAudioSignal = true,
                    ),
                onClick = {},
                reduceMotion = false,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Preview(name = "VoiceOrb - Speaking", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VoiceOrbSpeakingPreview() {
    RemyTheme(darkTheme = true, dynamicColor = false) {
        Box(
            modifier =
                Modifier
                    .background(Color.Black)
                    .padding(16.dp),
        ) {
            VoiceOrb(
                state = VoiceOrbUiState(mode = VoiceOrbMode.SPEAKING),
                onClick = {},
                reduceMotion = false,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Preview(name = "VoiceOrb - Reduce Motion", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VoiceOrbReduceMotionPreview() {
    RemyTheme(darkTheme = true, dynamicColor = false) {
        Row(
            modifier =
                Modifier
                    .background(Color.Black)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VoiceOrb(
                state = VoiceOrbUiState(mode = VoiceOrbMode.IDLE),
                onClick = {},
                reduceMotion = true,
            )
            VoiceOrb(
                state = VoiceOrbUiState(mode = VoiceOrbMode.LISTENING),
                onClick = {},
                reduceMotion = true,
            )
            VoiceOrb(
                state = VoiceOrbUiState(mode = VoiceOrbMode.SPEAKING),
                onClick = {},
                reduceMotion = true,
            )
        }
    }
}
