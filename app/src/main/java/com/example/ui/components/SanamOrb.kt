package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class SanamOrbMode {
    IDLE,
    LISTENING,
    SPEAKING,
    THINKING,
    MUTED
}

val SanamPurpleNeon = Color(0xFFA020F0)
val SanamRedGradientStart = Color(0xFFFF0A4A)
val SanamRedGradientEnd = Color(0xFFD50000)

@Composable
fun SanamStarryBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        List(40) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 2.5f + 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "star_twinkle")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        stars.forEachIndexed { idx, (rx, ry, r) ->
            val opacity = if (idx % 2 == 0) alphaAnim else (1.1f - alphaAnim).coerceIn(0.1f, 1.0f)
            drawCircle(
                color = Color(0xFFA020F0).copy(alpha = opacity * 0.5f),
                radius = r,
                center = Offset(rx * width, ry * height)
            )
        }
    }
}

@Composable
fun SanamRedSLogo(modifier: Modifier = Modifier, sizeDp: Dp = 50.dp) {
    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.22f

        val redGradient = Brush.verticalGradient(
            colors = listOf(SanamRedGradientStart, SanamRedGradientEnd)
        )

        val path = Path().apply {
            moveTo(w * 0.82f, h * 0.22f)
            cubicTo(w * 0.82f, h * 0.08f, w * 0.18f, h * 0.08f, w * 0.18f, h * 0.26f)
            cubicTo(w * 0.18f, h * 0.46f, w * 0.82f, h * 0.48f, w * 0.82f, h * 0.70f)
            cubicTo(w * 0.82f, h * 0.90f, w * 0.18f, h * 0.90f, w * 0.18f, h * 0.78f)
        }

        drawPath(
            path = path,
            brush = redGradient,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SanamHolographicOrb(
    mode: SanamOrbMode,
    statusMessage: String,
    onSingleTap: () -> Unit,
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = true,
    orbDiameterDp: Dp = if (isFullScreen) 220.dp else 68.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sanam_orb_trans")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (mode == SanamOrbMode.LISTENING) 0.95f else 0.98f,
        targetValue = if (mode == SanamOrbMode.LISTENING) 1.08f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mode == SanamOrbMode.LISTENING) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val sparkRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spark_rotation"
    )

    val ringColor = when (mode) {
        SanamOrbMode.MUTED -> Color.Gray
        SanamOrbMode.LISTENING -> Color(0xFF00E5FF)
        SanamOrbMode.SPEAKING -> Color(0xFFFF2092)
        else -> SanamPurpleNeon
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(orbDiameterDp)
            .scale(pulseScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .testTag("sanam_holographic_orb")
    ) {
        // Outer Glowing Purple Neon Ring + Sparkles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) - 8f

            // Neon Outer Ring
            drawCircle(
                color = ringColor.copy(alpha = 0.85f),
                radius = radius,
                style = Stroke(width = if (isFullScreen) 6f else 3f)
            )

            // Translucent Glow Layer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ringColor.copy(alpha = 0.45f), ringColor.copy(alpha = 0.10f), Color.Transparent),
                    center = centerOffset,
                    radius = radius + 12f
                ),
                radius = radius + 10f
            )

            // Animated sparkles around border
            if (isFullScreen) {
                val sparkCount = 8
                for (i in 0 until sparkCount) {
                    val angle = Math.toRadians((i * (360 / sparkCount) + sparkRotation).toDouble())
                    val sparkX = centerOffset.x + (radius + 2f) * cos(angle).toFloat()
                    val sparkY = centerOffset.y + (radius + 2f) * sin(angle).toFloat()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.80f),
                        radius = 2.5f,
                        center = Offset(sparkX, sparkY)
                    )
                }
            }
        }

        // Core Black Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(orbDiameterDp - (if (isFullScreen) 16.dp else 8.dp))
                .clip(CircleShape)
                .background(Color(0xFF0A0A0A))
                .border(
                    width = if (isFullScreen) 2.dp else 1.dp,
                    color = ringColor.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                // RED S LOGO
                SanamRedSLogo(sizeDp = if (isFullScreen) 72.dp else 22.dp)

                if (isFullScreen) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SANAM",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "- AI ASSISTANT -",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        letterSpacing = 1.sp
                    )
                } else {
                    Text(
                        text = "SANAM",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
