package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.insights.MicroSpendSummary
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * 1. INTEGRATIVE MONTHLY SPENDING CURVE CARD
 * Features:
 * - Smooth bezier cubic spline with area glow gradient
 * - Peak outlay anomaly marker with pill badge ("Peak: ₹X")
 * - Safe daily allowance dotted baseline
 * - Interactive touch scrubber with floating inspection tooltip
 */
@Composable
fun IntegrativeSpendingCurveCard(
    spendingTrend: Map<Long, BigDecimal>,
    safeDailyAllowance: BigDecimal = BigDecimal.ZERO,
    projectedTotal: BigDecimal = BigDecimal.ZERO,
    modifier: Modifier = Modifier
) {
    if (spendingTrend.isEmpty()) return

    val sortedEntries = remember(spendingTrend) { spendingTrend.entries.sortedBy { it.key } }
    val maxSpend = remember(sortedEntries) {
        sortedEntries.maxOfOrNull { it.value }?.coerceAtLeast(BigDecimal.ONE) ?: BigDecimal.ONE
    }
    val peakEntry = remember(sortedEntries) { sortedEntries.maxByOrNull { it.value } }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(spendingTrend) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
    }

    val lineColor = Color(0xFF10B981) // Emerald Green
    val baselineColor = Color(0xFF6366F1) // Indigo Primary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header with Title & Peak Alert Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = lineColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "MONTHLY SPENDING CURVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = lineColor,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        "Daily trajectory vs baseline threshold",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (peakEntry != null && peakEntry.value > BigDecimal.ZERO) {
                    val cal = Calendar.getInstance().apply { timeInMillis = peakEntry.key }
                    val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ExpenseRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Peak: ${peakEntry.value.formatIndian()} (Day $dayNum)",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonospaceFont,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Interactive Tooltip Callout if User Touched a Node
            selectedIndex?.let { idx ->
                val entry = sortedEntries.getOrNull(idx)
                if (entry != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = entry.key }
                    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, lineColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = entry.value.formatIndian(),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.ExtraBold,
                                color = lineColor
                            )
                        }
                    }
                }
            }

            // Spline Canvas with Touch Detection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sortedEntries) {
                            detectTapGestures { tapOffset ->
                                if (sortedEntries.size >= 2) {
                                    val stepX = size.width / (sortedEntries.size - 1)
                                    val nearestIndex = ((tapOffset.x + (stepX / 2f)) / stepX).toInt()
                                        .coerceIn(0, sortedEntries.lastIndex)
                                    selectedIndex = nearestIndex
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val n = sortedEntries.size
                    if (n < 2) return@Canvas

                    val stepX = w / (n - 1)
                    val points = sortedEntries.mapIndexed { i, entry ->
                        val ratio = (entry.value.divide(maxSpend, 4, RoundingMode.HALF_UP).toFloat()) * animProgress.value
                        val px = i * stepX
                        val py = (h * 0.90f) - (ratio * (h * 0.78f))
                        Offset(px, py)
                    }

                    // 1. Draw Safe Daily Allowance Dashed Baseline
                    if (safeDailyAllowance > BigDecimal.ZERO) {
                        val safeRatio = (safeDailyAllowance.divide(maxSpend, 4, RoundingMode.HALF_UP).toFloat()).coerceIn(0.05f, 0.95f)
                        val baselineY = (h * 0.90f) - (safeRatio * (h * 0.78f))
                        drawLine(
                            color = baselineColor.copy(alpha = 0.55f),
                            start = Offset(0f, baselineY),
                            end = Offset(w, baselineY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    }

                    // 2. Build Smooth Cubic Spline Path & Area Fill Path
                    val linePath = Path()
                    val fillPath = Path()

                    linePath.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, h)
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until n - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cp1 = Offset(p1.x + (p2.x - p1.x) * 0.5f, p1.y)
                        val cp2 = Offset(p1.x + (p2.x - p1.x) * 0.5f, p2.y)
                        linePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                        fillPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                    }

                    fillPath.lineTo(points.last().x, h)
                    fillPath.close()

                    // Draw area gradient fill
                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.28f * animProgress.value),
                            lineColor.copy(alpha = 0.08f * animProgress.value),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = h
                    )
                    drawPath(path = fillPath, brush = gradientBrush)

                    // Draw luminous outer glow on curve
                    drawPath(
                        path = linePath,
                        color = lineColor.copy(alpha = 0.35f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw primary spline curve
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 3. Draw Data Point Nodes
                    points.forEachIndexed { i, pt ->
                        val isSelected = selectedIndex == i
                        val isPeak = sortedEntries[i].key == peakEntry?.key
                        val nodeColor = if (isPeak) ExpenseRed else lineColor

                        // Outer bloom on selected or peak
                        if (isSelected || isPeak) {
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.35f),
                                radius = 7.dp.toPx(),
                                center = pt
                            )
                        }

                        // Node ring & white center
                        drawCircle(
                            color = nodeColor,
                            radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 2.5.dp.toPx() else 1.8.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Legend / Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(lineColor))
                    Text("Daily Outflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (safeDailyAllowance > BigDecimal.ZERO) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.width(12.dp).height(2.dp).background(baselineColor))
                        Text("Safe Pace: ${safeDailyAllowance.formatIndian()}/d", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * 2. INTEGRATIVE EXPENSE CATEGORIES DONUT CHART CARD
 * Features:
 * - Thick multi-color donut chart with animated radial sweep
 * - Centered total spend display
 * - Interactive slice highlight
 * - Compact bento breakdown chips with percentages
 */
@Composable
fun ExpenseCategoryDonutCard(
    categorySpending: Map<Category?, BigDecimal>,
    totalSpent: BigDecimal,
    onCategoryClick: ((Category?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (categorySpending.isEmpty() || totalSpent <= BigDecimal.ZERO) return

    val sortedCategories = remember(categorySpending) {
        categorySpending.toList().sortedByDescending { it.second }
    }

    val palette = listOf(
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF6366F1), // Indigo
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF97316), // Orange
        Color(0xFF64748B)  // Slate
    )

    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(categorySpending) {
        sweepAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    var selectedCat by remember { mutableStateOf<Category?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.DonutLarge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "EXPENSE CATEGORIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text = "${sortedCategories.size} Categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Donut Graphic with Centered Balance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 26.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val arcTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    var startAngle = -90f
                    sortedCategories.forEachIndexed { i, pair ->
                        val cat = pair.first
                        val amt = pair.second
                        val sliceColor = palette[i % palette.size]
                        val isHighlighted = selectedCat == null || selectedCat == cat
                        val alpha = if (isHighlighted) 1.0f else 0.35f

                        val sweep = (amt.divide(totalSpent, 4, RoundingMode.HALF_UP).toFloat() * 360f) * sweepAnim.value
                        val gap = if (sortedCategories.size > 1) 3.5f else 0f
                        val actualSweep = (sweep - gap).coerceAtLeast(1f)

                        drawArc(
                            color = sliceColor.copy(alpha = alpha),
                            startAngle = startAngle,
                            sweepAngle = actualSweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }

                // Center Content: Total Spend Outlay
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL SPENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = totalSpent.formatIndian(),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = MonospaceFont,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedCat != null) {
                        val selAmt = categorySpending[selectedCat] ?: BigDecimal.ZERO
                        val pct = selAmt.multiply(BigDecimal(100)).divide(totalSpent, 0, RoundingMode.HALF_UP).toInt()
                        Text(
                            text = "${selectedCat?.name}: $pct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Category Legend Grid with Percentages & Amounts
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortedCategories.take(5).forEachIndexed { i, pair ->
                    val cat = pair.first
                    val amt = pair.second
                    val pct = amt.multiply(BigDecimal(100)).divide(totalSpent, 0, RoundingMode.HALF_UP).toInt()
                    val sliceColor = palette[i % palette.size]
                    val isSelected = selectedCat == cat

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) sliceColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isSelected) sliceColor else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedCat = if (selectedCat == cat) null else cat
                                onCategoryClick?.invoke(cat)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(sliceColor))
                                Text(cat?.icon ?: "💳", fontSize = 14.sp)
                                Text(
                                    text = cat?.name ?: "General",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = sliceColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = sliceColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = amt.formatIndian(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = MonospaceFont,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. DAILY CASH BURN RUNWAY & SOLVENCY AREA CHART CARD
 * Features:
 * - Liquid reserves depleting over projected timeline
 * - Dual-zone safe vs warning buffer zones
 * - Solvency status badge (Resilient > 6m, Safe 3-6m, Critical < 3m)
 * - Average daily burn rate velocity
 */
@Composable
fun CashBurnRunwayCard(
    liquidReserves: BigDecimal,
    monthlyExpense: BigDecimal,
    dailyBurnAverage: BigDecimal,
    modifier: Modifier = Modifier
) {
    val runwayMonths = if (monthlyExpense > BigDecimal.ZERO) {
        liquidReserves.divide(monthlyExpense, 1, RoundingMode.HALF_UP).toDouble()
    } else 6.0

    val (gradeLabel, gradeColor) = when {
        runwayMonths >= 6.0 -> "RESILIENT" to IncomeGreen
        runwayMonths >= 3.0 -> "SAFE" to IncomeGreen
        runwayMonths >= 1.5 -> "CAUTION" to AmberWarning
        else -> "CRITICAL" to ExpenseRed
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(liquidReserves, monthlyExpense) {
        animProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = gradeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "DAILY CASH BURN RUNWAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = gradeColor,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = gradeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$runwayMonths Months ($gradeLabel)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = gradeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Liquid Reserves", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = liquidReserves.formatIndian(),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MonospaceFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Daily Burn Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${dailyBurnAverage.formatIndian()}/d",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MonospaceFont,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }

            // Runway Projection Graph Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val pointsCount = 30
                    val stepX = w / (pointsCount - 1)

                    // Draw runway depletion curve over 30 days
                    val initialRes = liquidReserves.toFloat().coerceAtLeast(1000f)
                    val dailyBurn = dailyBurnAverage.toFloat().coerceAtLeast(10f)

                    val curvePoints = (0 until pointsCount).map { day ->
                        val projectedRes = (initialRes - (day * dailyBurn)).coerceAtLeast(0f)
                        val ratio = (projectedRes / initialRes) * animProgress.value
                        val px = day * stepX
                        val py = (h * 0.90f) - (ratio * (h * 0.75f))
                        Offset(px, py)
                    }

                    // Build Area Fill Path
                    val fillPath = Path().apply {
                        moveTo(curvePoints[0].x, h)
                        lineTo(curvePoints[0].x, curvePoints[0].y)
                        for (i in 0 until pointsCount - 1) {
                            val p1 = curvePoints[i]
                            val p2 = curvePoints[i + 1]
                            val cp1 = Offset(p1.x + (p2.x - p1.x) * 0.5f, p1.y)
                            val cp2 = Offset(p1.x + (p2.x - p1.x) * 0.5f, p2.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                        }
                        lineTo(curvePoints.last().x, h)
                        close()
                    }

                    // Fill with dual green-to-amber gradient
                    val areaBrush = Brush.verticalGradient(
                        colors = listOf(
                            gradeColor.copy(alpha = 0.25f),
                            gradeColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                    drawPath(fillPath, brush = areaBrush)

                    // Curve Stroke
                    val strokePath = Path().apply {
                        moveTo(curvePoints[0].x, curvePoints[0].y)
                        for (i in 0 until pointsCount - 1) {
                            val p1 = curvePoints[i]
                            val p2 = curvePoints[i + 1]
                            val cp1 = Offset(p1.x + (p2.x - p1.x) * 0.5f, p1.y)
                            val cp2 = Offset(p1.x + (p2.x - p1.x) * 0.5f, p2.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = gradeColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw end buffer dot
                    val lastPoint = curvePoints.last()
                    drawCircle(color = gradeColor, radius = 4.dp.toPx(), center = lastPoint)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = lastPoint)
                }
            }

            // Solvency Gauge Linear Indicator
            LinearProgressIndicator(
                progress = { (runwayMonths.toFloat() / 6f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = gradeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0m Depleted", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = ExpenseRed)
                Text("3m Safe Buffer", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AmberWarning)
                Text("6m+ Fortress Solvency", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = IncomeGreen)
            }
        }
    }
}

/**
 * 4. INTEGRATIVE 5-AXIS MICRO-LEAK RADAR & ANOMALY SCANNER CARD
 * Features:
 * - 5-Axis polygonal spiderweb chart:
 *   1. Frequency (Small spend count)
 *   2. Impulse Velocity (Night/fast spend)
 *   3. Food & Q-Commerce (Delivery apps)
 *   4. Weekend Surge (Fri-Sun vs Weekday ratio)
 *   5. Recurring Leaks (Repeated micro-charges)
 * - Concentric polygonal guide rings
 * - Glowing green/coral illuminated polygon with vertex nodes
 * - Anomaly diagnostics & leak risk score
 */
@Composable
fun MicroLeakRadarCard(
    transactions: List<Transaction>,
    categories: List<Category>,
    microSummary: MicroSpendSummary,
    modifier: Modifier = Modifier
) {
    // 1. Calculate 5-Axis Leak Dimensions (normalized 0.15f to 1.0f)
    val totalExpenseTxs = remember(transactions) { transactions.filter { it.type == "Expense" } }
    val totalExpenseCount = totalExpenseTxs.size.coerceAtLeast(1)

    // Axis 1: Frequency Ratio
    val frequencyRatio = remember(microSummary, totalExpenseCount) {
        (microSummary.microTxCount.toFloat() / totalExpenseCount).coerceIn(0.15f, 1.0f)
    }

    // Axis 2: Impulse (Transactions after 9 PM or before 5 AM)
    val impulseRatio = remember(totalExpenseTxs) {
        val cal = Calendar.getInstance()
        val nightCount = totalExpenseTxs.count {
            cal.timeInMillis = it.timestamp
            val hr = cal.get(Calendar.HOUR_OF_DAY)
            hr >= 21 || hr < 5
        }
        (nightCount.toFloat() / totalExpenseCount.toFloat() * 2f).coerceIn(0.15f, 1.0f)
    }

    // Axis 3: Food & Quick Commerce
    val foodQCommerceRatio = remember(totalExpenseTxs) {
        val qCommerceKeywords = listOf("swiggy", "zomato", "zepto", "blinkit", "instamart", "dunzo", "eats")
        val foodCount = totalExpenseTxs.count { tx ->
            qCommerceKeywords.any { kw -> tx.merchant.lowercase().contains(kw) }
        }
        (foodCount.toFloat() / totalExpenseCount.toFloat() * 2.5f).coerceIn(0.15f, 1.0f)
    }

    // Axis 4: Weekend Surge
    val weekendSurgeRatio = remember(totalExpenseTxs) {
        val cal = Calendar.getInstance()
        val weekendTxs = totalExpenseTxs.filter {
            cal.timeInMillis = it.timestamp
            val day = cal.get(Calendar.DAY_OF_WEEK)
            day == Calendar.SATURDAY || day == Calendar.SUNDAY
        }
        val weekendRate = if (weekendTxs.isNotEmpty()) weekendTxs.size.toFloat() / totalExpenseCount else 0.2f
        (weekendRate * 2.2f).coerceIn(0.15f, 1.0f)
    }

    // Axis 5: Recurring Small Leaks (Transactions appearing >= 2 times with identical amount < ₹250)
    val recurringLeakRatio = remember(totalExpenseTxs) {
        val smallRepeats = totalExpenseTxs.filter { it.amount < BigDecimal(250) }
            .groupBy { it.merchant.lowercase() }
            .count { it.value.size >= 2 }
        (smallRepeats.toFloat() / 5f).coerceIn(0.15f, 1.0f)
    }

    val axisValues = remember(frequencyRatio, impulseRatio, foodQCommerceRatio, weekendSurgeRatio, recurringLeakRatio) {
        listOf(
            "Frequency" to frequencyRatio,
            "Impulse" to impulseRatio,
            "Q-Commerce" to foodQCommerceRatio,
            "Weekend" to weekendSurgeRatio,
            "Recurring" to recurringLeakRatio
        )
    }

    val compositeRisk = remember(axisValues) {
        (axisValues.map { it.second }.average().toFloat() * 100).toInt()
    }

    val isElevatedRisk = compositeRisk >= 45
    val radarColor = if (isElevatedRisk) ExpenseRed else IncomeGreen

    val radarAnim = remember { Animatable(0f) }
    LaunchedEffect(axisValues) {
        radarAnim.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.CrisisAlert,
                        contentDescription = null,
                        tint = radarColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "MICRO-LEAK RADAR ALERTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = radarColor,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = radarColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, radarColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Leak Risk: $compositeRisk%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = radarColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 5-Axis Spiderweb Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxRadius = size.width * 0.38f
                    val numAxes = 5
                    val angleStep = (2 * Math.PI / numAxes).toFloat()

                    // 1. Draw Concentric Polygonal Guide Rings (25%, 50%, 75%, 100%)
                    val rings = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                    rings.forEach { rFraction ->
                        val ringPath = Path()
                        for (i in 0 until numAxes) {
                            val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                            val px = cx + (maxRadius * rFraction * cos(angle))
                            val py = cy + (maxRadius * rFraction * sin(angle))
                            if (i == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
                        }
                        ringPath.close()
                        drawPath(
                            path = ringPath,
                            color = Color(0xFF334155).copy(alpha = 0.45f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 2. Draw Radiating Spokes
                    for (i in 0 until numAxes) {
                        val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                        val ex = cx + (maxRadius * cos(angle))
                        val ey = cy + (maxRadius * sin(angle))
                        drawLine(
                            color = Color(0xFF475569).copy(alpha = 0.40f),
                            start = Offset(cx, cy),
                            end = Offset(ex, ey),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 3. Build Animated Radar Polygon
                    val polygonPath = Path()
                    val vertexPoints = mutableListOf<Offset>()

                    axisValues.forEachIndexed { i, pair ->
                        val value = pair.second * radarAnim.value
                        val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                        val r = maxRadius * value
                        val px = cx + (r * cos(angle))
                        val py = cy + (r * sin(angle))
                        vertexPoints.add(Offset(px, py))
                        if (i == 0) polygonPath.moveTo(px, py) else polygonPath.lineTo(px, py)
                    }
                    polygonPath.close()

                    // Fill polygon with translucent glowing brush
                    val polygonBrush = Brush.radialGradient(
                        colors = listOf(
                            radarColor.copy(alpha = 0.38f),
                            radarColor.copy(alpha = 0.12f)
                        ),
                        center = Offset(cx, cy),
                        radius = maxRadius
                    )
                    drawPath(polygonPath, brush = polygonBrush)

                    // Draw polygon border
                    drawPath(
                        path = polygonPath,
                        color = radarColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw Vertex Dots
                    vertexPoints.forEach { pt ->
                        drawCircle(color = radarColor, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                    }
                }

                // Axis Label Tags around the perimeter
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "📊 Frequency",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    Text(
                        "⚡ Impulse",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                    )
                    Text(
                        "🍔 Q-Commerce",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 6.dp)
                    )
                    Text(
                        "🎉 Weekend",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 6.dp)
                    )
                    Text(
                        "🔄 Recurring",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                    )
                }
            }

            // Diagnostic Alert Summary Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isElevatedRisk) ExpenseRed.copy(alpha = 0.10f) else IncomeGreen.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, if (isElevatedRisk) ExpenseRed.copy(alpha = 0.25f) else IncomeGreen.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isElevatedRisk) "⚠️" else "🛡️", fontSize = 16.sp)
                    Text(
                        text = if (isElevatedRisk)
                            "Micro-expenditure leakage is elevated in food delivery & weekend impulse spends."
                        else
                            "Micro-leaks are well disciplined (<20% of net outflow). Optimal savings velocity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
