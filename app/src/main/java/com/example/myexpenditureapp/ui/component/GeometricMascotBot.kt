package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myexpenditureapp.ui.theme.ExpenseRed
import com.example.myexpenditureapp.ui.theme.IncomeGreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Expressive emotional states for Nomi, the minimal geometric expenditure bot.
 */
enum class MascotMood {
    NEUTRAL,   // Normal steady tracking (Cyan pill LEDs)
    OPTIMAL,   // Budget under control / surplus (Mint green smiling arcs)
    ALERT,     // High burn / leak detected / critical budget (Coral red sharp eyes)
    SCANNING,  // Auto-parsing SMS / calculation in progress
    SLEEPING   // Idle / empty state / late night
}

/**
 * Nomi — A minimal, hardware-accelerated geometric companion inspired by
 * Teenage Engineering, Grok Bot, and Nothing OS aesthetics.
 *
 * Cost: 0 KB image weight, hardware-accelerated, pure math/geometry.
 */
@Composable
fun GeometricMascotBot(
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.NEUTRAL,
    size: Dp = 48.dp,
    animated: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nomi_animations")

    // Organic micro-floating/breathing movement (±2.5dp)
    val floatOffset by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nomi_float"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Natural periodic blink cycle (stays open most of the time, blinks quickly every ~4s)
    val blinkScale by if (animated && (mood == MascotMood.NEUTRAL || mood == MascotMood.OPTIMAL)) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "nomi_blink"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    // Scanning radar line progress for SCANNING mood
    val scanProgress by if (animated && mood == MascotMood.SCANNING) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "nomi_scan"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Subtle breathing glow intensity
    val glowAlpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nomi_glow"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .offset(y = floatOffset.dp)
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cornerRadius = w * 0.26f

            // 1. Outer Chassis: Matte Obsidian Dark Titanium Body
            val chassisBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E293B), // Slate 800
                    Color(0xFF0F172A), // Slate 900
                    Color(0xFF020617)  // Obsidian deep dark
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            drawRoundRect(
                brush = chassisBrush,
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // 2. Beveled Metallic Chamfer Rim (Titanium accent edge)
            drawRoundRect(
                color = Color(0xFF475569).copy(alpha = 0.55f),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = w * 0.045f)
            )

            // 3. Inner Dark Glass Visor Display (High-contrast recessed screen)
            val visorInset = w * 0.12f
            val visorSize = w - (visorInset * 2)
            val visorCorner = cornerRadius * 0.75f

            drawRoundRect(
                color = Color(0xFF080C14),
                topLeft = Offset(visorInset, visorInset),
                size = Size(visorSize, visorSize),
                cornerRadius = CornerRadius(visorCorner, visorCorner)
            )

            // Subtle glass reflection highlight across the top edge
            val glassReflection = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                startY = visorInset,
                endY = visorInset + (visorSize * 0.45f)
            )
            drawRoundRect(
                brush = glassReflection,
                topLeft = Offset(visorInset, visorInset),
                size = Size(visorSize, visorSize * 0.45f),
                cornerRadius = CornerRadius(visorCorner, visorCorner)
            )

            // 4. Expressive Digital LED Matrix Eyes
            val ledColor = when (mood) {
                MascotMood.OPTIMAL -> IncomeGreen.copy(alpha = glowAlpha)
                MascotMood.ALERT -> ExpenseRed.copy(alpha = glowAlpha)
                MascotMood.NEUTRAL -> Color(0xFF38BDF8).copy(alpha = glowAlpha) // Neon Cyan
                MascotMood.SCANNING -> Color(0xFF818CF8).copy(alpha = glowAlpha) // Indigo
                MascotMood.SLEEPING -> Color(0xFF64748B).copy(alpha = 0.6f)     // Dim Slate
            }

            // Glow bloom behind eyes
            drawCircle(
                color = ledColor.copy(alpha = 0.15f * glowAlpha),
                radius = visorSize * 0.32f,
                center = Offset(w / 2f, h / 2f)
            )

            drawMoodEyes(
                mood = mood,
                color = ledColor,
                w = w,
                h = h,
                visorInset = visorInset,
                visorSize = visorSize,
                blinkScale = blinkScale,
                scanProgress = scanProgress
            )
        }
    }
}

/**
 * Draws the minimalist geometric eye matrices according to mood.
 */
private fun DrawScope.drawMoodEyes(
    mood: MascotMood,
    color: Color,
    w: Float,
    h: Float,
    visorInset: Float,
    visorSize: Float,
    blinkScale: Float,
    scanProgress: Float
) {
    val centerY = h * 0.50f
    val eyeSpacing = w * 0.14f
    val leftEyeCenterX = (w / 2f) - eyeSpacing
    val rightEyeCenterX = (w / 2f) + eyeSpacing

    when (mood) {
        MascotMood.NEUTRAL -> {
            // Minimal vertical cyan LED capsules
            val pillWidth = w * 0.075f
            val pillHeight = (w * 0.18f) * blinkScale

            drawRoundRect(
                color = color,
                topLeft = Offset(leftEyeCenterX - (pillWidth / 2f), centerY - (pillHeight / 2f)),
                size = Size(pillWidth, pillHeight.coerceAtLeast(w * 0.02f)),
                cornerRadius = CornerRadius(pillWidth / 2f, pillWidth / 2f)
            )

            drawRoundRect(
                color = color,
                topLeft = Offset(rightEyeCenterX - (pillWidth / 2f), centerY - (pillHeight / 2f)),
                size = Size(pillWidth, pillHeight.coerceAtLeast(w * 0.02f)),
                cornerRadius = CornerRadius(pillWidth / 2f, pillWidth / 2f)
            )
        }

        MascotMood.OPTIMAL -> {
            // Cheerful mint inverted-V arcs (^ ^)
            val strokeWidth = w * 0.065f
            val arcSpan = w * 0.09f
            val arcHeight = w * 0.08f

            val leftPath = Path().apply {
                moveTo(leftEyeCenterX - arcSpan, centerY + (arcHeight * 0.5f))
                lineTo(leftEyeCenterX, centerY - (arcHeight * 0.5f))
                lineTo(leftEyeCenterX + arcSpan, centerY + (arcHeight * 0.5f))
            }

            val rightPath = Path().apply {
                moveTo(rightEyeCenterX - arcSpan, centerY + (arcHeight * 0.5f))
                lineTo(rightEyeCenterX, centerY - (arcHeight * 0.5f))
                lineTo(rightEyeCenterX + arcSpan, centerY + (arcHeight * 0.5f))
            }

            drawPath(
                path = leftPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawPath(
                path = rightPath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        MascotMood.ALERT -> {
            // Focused, vigilant angled slits (\ /)
            val strokeWidth = w * 0.065f
            val slitSpan = w * 0.085f
            val slitHeight = w * 0.06f

            drawLine(
                color = color,
                start = Offset(leftEyeCenterX - slitSpan, centerY - (slitHeight * 0.5f)),
                end = Offset(leftEyeCenterX + slitSpan, centerY + (slitHeight * 0.5f)),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = color,
                start = Offset(rightEyeCenterX - slitSpan, centerY + (slitHeight * 0.5f)),
                end = Offset(rightEyeCenterX + slitSpan, centerY - (slitHeight * 0.5f)),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        MascotMood.SCANNING -> {
            // Horizontal radar laser beam sweeping down the visor
            val beamY = visorInset + (visorSize * scanProgress)
            val beamHeight = w * 0.05f

            drawLine(
                color = color,
                start = Offset(visorInset + (w * 0.04f), beamY),
                end = Offset(w - visorInset - (w * 0.04f), beamY),
                strokeWidth = beamHeight,
                cap = StrokeCap.Round
            )

            // Central pulsing dot
            drawCircle(
                color = color,
                radius = w * 0.04f,
                center = Offset(w / 2f, centerY)
            )
        }

        MascotMood.SLEEPING -> {
            // Calm horizontal resting dashes (- -)
            val dashWidth = w * 0.12f
            val strokeWidth = w * 0.05f

            drawLine(
                color = color,
                start = Offset(leftEyeCenterX - (dashWidth / 2f), centerY),
                end = Offset(leftEyeCenterX + (dashWidth / 2f), centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = color,
                start = Offset(rightEyeCenterX - (dashWidth / 2f), centerY),
                end = Offset(rightEyeCenterX + (dashWidth / 2f), centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Animated Mascot Awakening / Boot Sequence:
 * 1. Chassis scale spring & visor power-on expansion.
 * 2. Dual lidar radar scan across visor.
 * 3. Curious left/right eye glance and playful wink.
 * 4. Hands off seamlessly to live steady-state floating/blinking mascot behavior.
 */
@Composable
fun BootAnimatedMascotBot(
    modifier: Modifier = Modifier,
    targetMood: MascotMood = MascotMood.OPTIMAL,
    size: Dp = 96.dp,
    onBootComplete: (() -> Unit)? = null
) {
    var bootStage by remember { mutableStateOf(0) } // 0: power, 1: scan, 2: glance, 3: wink, 4: steady
    val bootScale = remember { androidx.compose.animation.core.Animatable(0.5f) }
    val visorExpansion = remember { androidx.compose.animation.core.Animatable(0.05f) }
    val scanSweep = remember { androidx.compose.animation.core.Animatable(0f) }
    val eyeLookX = remember { androidx.compose.animation.core.Animatable(0f) } // -1f = left, 1f = right
    val rightEyeWink = remember { androidx.compose.animation.core.Animatable(1f) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Stage 0 -> 1: Power On & Spring Chassis (0 - 450ms)
        coroutineScope {
            launch {
                bootScale.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                )
            }
            launch {
                visorExpansion.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            }
        }
        bootStage = 1

        // Stage 1: Laser Scan Sweep (450ms - 950ms)
        scanSweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = LinearEasing)
        )
        bootStage = 2

        // Stage 2: Curious Glance Left then Right (950ms - 1500ms)
        eyeLookX.animateTo(-1f, tween(250, easing = FastOutSlowInEasing))
        eyeLookX.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        eyeLookX.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        bootStage = 3

        // Stage 3: Playful Wink (1500ms - 1850ms)
        rightEyeWink.animateTo(0.1f, tween(150, easing = LinearEasing))
        rightEyeWink.animateTo(1f, tween(150, easing = LinearEasing))

        // Stage 4: Boot complete -> steady state behavior!
        bootStage = 4
        onBootComplete?.invoke()
    }

    if (bootStage == 4) {
        // Settles directly into the standard live interactive mascot behavior
        GeometricMascotBot(
            modifier = modifier,
            mood = targetMood,
            size = size,
            animated = true
        )
    } else {
        // Boot Sequence Canvas
        Box(
            modifier = modifier
                .size(size)
                .scale(bootScale.value),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height
                val cornerRadius = w * 0.26f

                // Chassis
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    ),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )

                // Luminous Titanium Chamfer Edge
                drawRoundRect(
                    color = Color(0xFF38BDF8).copy(alpha = 0.4f * visorExpansion.value),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = w * 0.045f)
                )

                // Visor with CRT power-on vertical expansion
                val visorInset = w * 0.12f
                val fullVisorSize = w - (visorInset * 2)
                val currentVisorH = fullVisorSize * visorExpansion.value
                val visorTop = (h / 2f) - (currentVisorH / 2f)

                drawRoundRect(
                    color = Color(0xFF080C14),
                    topLeft = Offset(visorInset, visorTop),
                    size = Size(fullVisorSize, currentVisorH),
                    cornerRadius = CornerRadius(cornerRadius * 0.75f, cornerRadius * 0.75f)
                )

                if (bootStage >= 1) {
                    val centerY = h * 0.50f
                    val eyeSpacing = w * 0.14f
                    val glanceShift = (w * 0.05f) * eyeLookX.value
                    val leftCenter = (w / 2f) - eyeSpacing + glanceShift
                    val rightCenter = (w / 2f) + eyeSpacing + glanceShift
                    val cyanColor = Color(0xFF38BDF8)

                    if (bootStage == 1) {
                        // Scanning laser line
                        val laserY = visorTop + (currentVisorH * scanSweep.value)
                        drawLine(
                            color = cyanColor,
                            start = Offset(visorInset + (w * 0.05f), laserY),
                            end = Offset(w - visorInset - (w * 0.05f), laserY),
                            strokeWidth = w * 0.06f,
                            cap = StrokeCap.Round
                        )
                    }

                    if (bootStage >= 2) {
                        // Eye capsules during glance/wink
                        val pillW = w * 0.075f
                        val pillH = w * 0.18f

                        // Left Eye
                        drawRoundRect(
                            color = cyanColor,
                            topLeft = Offset(leftCenter - (pillW / 2f), centerY - (pillH / 2f)),
                            size = Size(pillW, pillH),
                            cornerRadius = CornerRadius(pillW / 2f, pillW / 2f)
                        )

                        // Right Eye (winks in stage 3)
                        val rightH = (pillH * rightEyeWink.value).coerceAtLeast(w * 0.02f)
                        drawRoundRect(
                            color = cyanColor,
                            topLeft = Offset(rightCenter - (pillW / 2f), centerY - (rightH / 2f)),
                            size = Size(pillW, rightH),
                            cornerRadius = CornerRadius(pillW / 2f, pillW / 2f)
                        )
                    }
                }
            }
        }
    }
}

