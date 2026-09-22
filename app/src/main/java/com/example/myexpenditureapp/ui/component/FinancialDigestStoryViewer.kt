package com.example.myexpenditureapp.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myexpenditureapp.domain.insights.InsightType
import com.example.myexpenditureapp.domain.insights.SmartInsight
import com.example.myexpenditureapp.ui.theme.MonospaceFont
import kotlinx.coroutines.delay

private const val STORY_DURATION_MS = 6000

@Composable
fun FinancialDigestStoryViewer(
    insights: List<SmartInsight>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onInsightAction: ((SmartInsight) -> Unit)? = null
) {
    if (insights.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, insights.lastIndex)) }
    var isPaused by remember { mutableStateOf(false) }
    var storyProgress by remember { mutableFloatStateOf(0f) }
    val view = LocalView.current

    // Auto-advancing story ticker
    LaunchedEffect(currentIndex, isPaused) {
        val stepIntervalMs = 40L
        val totalSteps = STORY_DURATION_MS / stepIntervalMs
        storyProgress = 0f

        while (storyProgress < 1f) {
            if (!isPaused) {
                delay(stepIntervalMs)
                storyProgress = (storyProgress + (1f / totalSteps)).coerceAtMost(1f)
            } else {
                delay(80L)
            }
        }

        if (currentIndex < insights.lastIndex) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    val currentInsight = insights[currentIndex]

    // Brandkit & Taste-Skill Palette
    val (accentColor, gradientColors) = when (currentInsight.type) {
        InsightType.POSITIVE -> Color(0xFF10B981) to listOf(
            Color(0xFF03241B),
            Color(0xFF063D2F),
            Color(0xFF021610),
            Color(0xFF010A07)
        )
        InsightType.WARNING -> Color(0xFFF43F5E) to listOf(
            Color(0xFF380613),
            Color(0xFF57091D),
            Color(0xFF22030B),
            Color(0xFF0D0104)
        )
        InsightType.TIP -> Color(0xFF818CF8) to listOf(
            Color(0xFF16143C),
            Color(0xFF282467),
            Color(0xFF0F0E28),
            Color(0xFF060611)
        )
        InsightType.NEUTRAL -> Color(0xFFF59E0B) to listOf(
            Color(0xFF331802),
            Color(0xFF542804),
            Color(0xFF1F0E01),
            Color(0xFF0A0500)
        )
    }

    fun goNext() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (currentIndex < insights.lastIndex) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    fun goPrevious() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (storyProgress > 0.2f) {
            storyProgress = 0f
        } else if (currentIndex > 0) {
            currentIndex--
        } else {
            storyProgress = 0f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .systemBarsPadding()
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            if (offset.x < size.width * 0.33f) {
                                goPrevious()
                            } else {
                                goNext()
                            }
                        }
                    )
                }
                .pointerInput(currentIndex) {
                    var totalDragY = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            totalDragY += dragAmount
                            if (totalDragY > 140f) onDismiss()
                        },
                        onDragEnd = { totalDragY = 0f }
                    )
                }
                .pointerInput(currentIndex) {
                    var totalDragX = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                        onDragEnd = {
                            if (totalDragX < -80f) goNext()
                            else if (totalDragX > 80f) goPrevious()
                            totalDragX = 0f
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP PROGRESS & CHANNEL BAR
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        insights.forEachIndexed { index, _ ->
                            val segmentProgress = when {
                                index < currentIndex -> 1f
                                index == currentIndex -> storyProgress
                                else -> 0f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(segmentProgress)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, accentColor)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    InsightVectorIcon(
                                        iconKey = currentInsight.icon,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Monthly Financial Wrapped",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Insight ${currentIndex + 1} of ${insights.size} • Analytics in Depth",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isPaused = !isPaused },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Play" else "Pause",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // FULL-HEIGHT EXPANDED STORY BODY (top-to-bottom layout, utilizing full vertical height)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Category Eyebrow
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = when (currentInsight.type) {
                                    InsightType.POSITIVE -> "WEALTH & SAVINGS"
                                    InsightType.WARNING -> "EXPENSE OUTFLOW"
                                    InsightType.TIP -> "LIFESTYLE METRICS"
                                    InsightType.NEUTRAL -> "CASHFLOW VELOCITY"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Headline Title
                        Text(
                            text = currentInsight.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 32.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // ANIMATED IN-DEPTH ANALYTICAL GRAPH (Fills available height dynamically)
                    StoryAnimatedGraph(
                        insight = currentInsight,
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    )

                    // Structured Breakdown Card (Expanded to display full analytical depth)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentInsight.metric != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ANALYTICS SUMMARY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = currentInsight.metric,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = MonospaceFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.12f)
                                )
                            }

                            Text(
                                text = currentInsight.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Normal
                            )

                            // Quick Telemetry Status Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accentColor))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Real-Time Telemetry", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Pacing Status: Verified", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM ACTION & TAP GUIDE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (onInsightAction != null && (currentInsight.drillDownCategoryId != null || currentInsight.drillDownMerchant != null || currentInsight.drillDownType != null)) {
                        Button(
                            onClick = {
                                onDismiss()
                                onInsightAction(currentInsight)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "View Related Transactions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = "Hold to pause • Tap sides to flip • Swipe down to close",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Animated In-Depth Financial Data Graphs for each story type
 */
@Composable
fun StoryAnimatedGraph(
    insight: SmartInsight,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "graphProgress"
    )

    LaunchedEffect(insight.id) {
        animationPlayed = false
        delay(60)
        animationPlayed = true
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                // 1. Savings Rate Donut Gauge
                insight.id.startsWith("savings_rate") -> {
                    SavingsRateAnimatedGauge(
                        progress = animatedProgress,
                        accentColor = accentColor
                    )
                }
                // 2. Financial Runway & Survival Buffer
                insight.id.startsWith("runway") -> {
                    RunwayAnimatedHorizonGauge(
                        progress = animatedProgress,
                        accentColor = accentColor
                    )
                }
                // 3. No-Spend Days & Zen Score
                insight.id.startsWith("zen") -> {
                    ZenCalendarAnimatedGrid(
                        progress = animatedProgress,
                        accentColor = accentColor
                    )
                }
                // 4. Late-Night / Time of Day Heatmap
                insight.id.startsWith("impulse") -> {
                    TimeOfDayAnimatedHeatmap(
                        progress = animatedProgress,
                        accentColor = accentColor
                    )
                }
                // 5. Weekend Spike 7-Day Bar Chart
                insight.id.startsWith("weekend") -> {
                    WeekendAnimatedBarChart(
                        progress = animatedProgress,
                        accentColor = accentColor
                    )
                }
                // 6. Daily Burn & Trajectory Curve
                insight.id.startsWith("daily_burn") -> {
                    DailyBurnAnimatedCurve(
                        progress = animatedProgress,
                        accentColor = accentColor
                    )
                }
                // 7. Top Merchant / Category Concentration
                insight.id.startsWith("top_") -> {
                    CategoryConcentrationChart(
                        progress = animatedProgress,
                        accentColor = accentColor,
                        title = insight.metric ?: "48%"
                    )
                }
                // 8. Budget Pacing / Limits
                else -> {
                    BudgetPacingAnimatedMeter(
                        progress = animatedProgress,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsRateAnimatedGauge(progress: Float, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val radius = diameter / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background track
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Savings Arc (75% default animated)
                val sweepAngle = 270f * progress
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.6f), accentColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(75 * progress).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontFamily = MonospaceFont,
                    color = Color.White
                )
                Text(
                    text = "SAVED",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Saved Net: +75%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Spent: ~25%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Text(
                    "Grade A+ Wealth Accumulation",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun WeekendAnimatedBarChart(progress: Float, accentColor: Color) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val heights = listOf(0.25f, 0.3f, 0.2f, 0.4f, 0.35f, 0.85f, 0.95f)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DAILY OUTFLOW PROFILE", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Text("Sat-Sun: 62% Spike", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val isWeekend = index >= 5
                val targetHeight = heights[index]
                val barColor = if (isWeekend) accentColor else Color.White.copy(alpha = 0.25f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight(targetHeight * progress)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barColor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isWeekend) FontWeight.Bold else FontWeight.Normal,
                        color = if (isWeekend) accentColor else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyBurnAnimatedCurve(progress: Float, accentColor: Color) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PACE TRAJECTORY (DAY 1-30)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text("Safe Pacing Zone", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePath = Path()
                val fillPath = Path()
                val w = size.width
                val h = size.height

                // Draw budget ceiling dashed line
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(0f, h * 0.22f),
                    end = Offset(w, h * 0.22f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Curve geometry
                val startY = h * 0.9f
                val endX = w * progress
                val endY = h * (0.9f - 0.65f * progress)
                val ctrl1X = w * 0.35f * progress
                val ctrl1Y = h * (0.9f - 0.25f * progress)
                val ctrl2X = w * 0.65f * progress
                val ctrl2Y = h * (0.8f - 0.5f * progress)

                strokePath.moveTo(0f, startY)
                strokePath.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY)

                fillPath.moveTo(0f, startY)
                fillPath.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY)
                fillPath.lineTo(endX, h)
                fillPath.lineTo(0f, h)
                fillPath.close()

                // Area gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            accentColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )

                // Main stroke
                drawPath(
                    path = strokePath,
                    color = accentColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Current point indicator with glowing halo
                if (progress > 0.03f) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.3f),
                        radius = 8.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 4.5.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(endX, endY)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Day 1", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Text("Day 15 (Mid)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Text("Day 30 (Goal)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryConcentrationChart(progress: Float, accentColor: Color, title: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OUTFLOW SHARE METRICS", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Text("Dominant Outflow", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Primary Commitment", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(title, style = MaterialTheme.typography.bodySmall, color = accentColor, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.48f * progress)
                        .background(accentColor)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("All Other Spend", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Text("52%", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), fontFamily = MonospaceFont)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.52f * progress)
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
fun BudgetPacingAnimatedMeter(progress: Float, accentColor: Color) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PACE VS TIME ELAPSED", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Text("On Track", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Month Elapsed", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                Text("70%", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), fontFamily = MonospaceFont)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.70f * progress)
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Budget Spent", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("52%", style = MaterialTheme.typography.bodySmall, color = accentColor, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.52f * progress)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
fun RunwayAnimatedHorizonGauge(progress: Float, accentColor: Color) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SURVIVAL HORIZON", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Text("Tier 1 Buffer", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = accentColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Reserve Coverage", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${(5.4f * progress).toString().take(3)} Months", style = MaterialTheme.typography.bodySmall, color = accentColor, fontFamily = MonospaceFont, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.75f * progress)
                        .background(accentColor)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0 Mo", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
            Text("3 Mo (Safe Target)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Text("6+ Mo (Strong)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ZenCalendarAnimatedGrid(progress: Float, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: 30-Day Dot Matrix
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("ZERO-SPEND CALENDAR", style = MaterialTheme.typography.labelSmall, fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (col in 0 until 7) {
                        val dayIndex = row * 7 + col + 1
                        val isNoSpend = (dayIndex % 3 == 0 || dayIndex % 5 == 0) && dayIndex <= 21
                        val isPast = dayIndex <= 21
                        val dotColor = when {
                            !isPast -> Color.White.copy(alpha = 0.1f)
                            isNoSpend -> accentColor.copy(alpha = progress)
                            else -> Color.White.copy(alpha = 0.3f)
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }

        // Right: Circular Zen Discipline Meter
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 6.dp.toPx()
                    drawCircle(color = Color.White.copy(alpha = 0.12f), style = Stroke(stroke))
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 310f * progress,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(88 * progress).toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = MonospaceFont,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text("ZEN SCORE", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun TimeOfDayAnimatedHeatmap(progress: Float, accentColor: Color) {
    val quadrants = listOf(
        Pair("Morning (6-12)", 0.15f),
        Pair("Afternoon (12-18)", 0.35f),
        Pair("Evening (18-23)", 0.30f),
        Pair("Late-Night (23-6)", 0.20f)
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("24-HOUR OUTFLOW HEATMAP", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Text("Peak: 11 PM - 2 AM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            quadrants.forEachIndexed { index, (label, share) ->
                val isLateNight = index == 3
                val barColor = if (isLateNight) accentColor else Color.White.copy(alpha = 0.3f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight(share * 2.5f * progress)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barColor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLateNight) "🌙 Night" else label.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = if (isLateNight) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLateNight) accentColor else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightVectorIcon(iconKey: String, tint: Color, modifier: Modifier = Modifier) {
    val iconVector: ImageVector = when (iconKey) {
        "savings" -> Icons.Default.Savings
        "runway" -> Icons.Default.Shield
        "zen" -> Icons.Default.Spa
        "time" -> Icons.Default.Nightlight
        "alert" -> Icons.Default.WarningAmber
        "budget" -> Icons.Default.CheckCircleOutline
        "velocity" -> Icons.Default.Speed
        "weekend" -> Icons.Default.Celebration
        "burn" -> Icons.Default.LocalFireDepartment
        "category" -> Icons.Default.PieChart
        "merchant" -> Icons.Default.Storefront
        else -> Icons.Default.Insights
    }
    Icon(
        imageVector = iconVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}
