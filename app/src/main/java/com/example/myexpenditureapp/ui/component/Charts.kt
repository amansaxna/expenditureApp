package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.ui.theme.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChart(
    data: Map<String, BigDecimal>,
    modifier: Modifier = Modifier
) {
    val total = data.values.fold(BigDecimal.ZERO) { acc, b -> acc.add(b) }
    if (total <= BigDecimal.ZERO) return

    // Group small slices as "Others"
    val threshold = total.multiply(BigDecimal("0.05")) // 5%
    val sortedData = data.toList().sortedByDescending { it.second }
    val mainData = sortedData.filter { it.second >= threshold }.toMap()
    val otherValue = sortedData.filter { it.second < threshold }.fold(BigDecimal.ZERO) { acc, pair -> acc.add(pair.second) }
    
    val finalData = if (otherValue > BigDecimal.ZERO) {
        mainData + ("Others" to otherValue)
    } else {
        mainData
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    val chartColors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald Green
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF97316)  // Orange
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                var startAngle = -90f
                finalData.values.forEachIndexed { index, value ->
                    val sweepAngle = (value.divide(total, 4, RoundingMode.HALF_UP).toFloat() * 360) * animationProgress.value
                    drawArc(
                        color = chartColors[index % chartColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width - 32.dp.toPx(), size.height - 32.dp.toPx()),
                        topLeft = Offset(16.dp.toPx(), 16.dp.toPx())
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TOTAL SPENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "₹${total.setScale(0, RoundingMode.HALF_UP)}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonospaceFont),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            finalData.keys.forEachIndexed { index, key ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = CircleShape,
                        color = chartColors[index % chartColors.size]
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BarChart(
    data: Map<String, BigDecimal>,
    modifier: Modifier = Modifier
) {
    val maxVal = data.values.maxOrNull()?.coerceAtLeast(BigDecimal.ONE) ?: BigDecimal.ONE
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            val barCount = data.size
            val spacing = 24.dp.toPx()
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = (size.width - totalSpacing) / barCount.coerceAtLeast(1)
            
            var currentX = spacing
            
            data.values.forEach { value ->
                val barHeight = (value.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat() * size.height) * animationProgress.value
                
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(currentX, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                
                // Actual bar with gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(AccentVibrant, AccentVibrantGradient)
                    ),
                    topLeft = Offset(currentX, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                
                currentX += barWidth + spacing
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            val barWidthPercent = 1f / data.size.coerceAtLeast(1)
            data.keys.forEach { key ->
                Box(modifier = Modifier.weight(barWidthPercent), contentAlignment = Alignment.Center) {
                    Text(
                        text = key.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: Map<Long, BigDecimal>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    dotCenterColor: Color = MaterialTheme.colorScheme.surface,
    projectedData: Map<Long, BigDecimal>? = null
) {
    if (data.isEmpty()) return
    
    val allValues = data.values.toList() + (projectedData?.values?.toList() ?: emptyList())
    val maxVal = allValues.maxOrNull()?.coerceAtLeast(BigDecimal.ONE) ?: BigDecimal.ONE
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500)
        )
    }

    val values = data.values.toList()
    
    Canvas(modifier = modifier.height(180.dp).fillMaxWidth()) {
        if (values.size < 2) return@Canvas
        
        val stepX = size.width / (values.size - 1)
        
        val points = values.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = size.height - (value.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat() * size.height) * animationProgress.value
            )
        }
        
        val path = Path()
        val fillPath = Path()
        
        path.moveTo(points[0].x, points[0].y)
        fillPath.moveTo(points[0].x, size.height)
        fillPath.lineTo(points[0].x, points[0].y)
        
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
            
            path.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p2.x, p2.y
            )
            fillPath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p2.x, p2.y
            )
        }
        
        fillPath.lineTo(points.last().x, size.height)
        fillPath.close()
        
        // Draw fill gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )
        
        // Draw the smooth line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw projected line if available
        projectedData?.let { proj ->
            val projValues = proj.values.toList()
            if (projValues.size >= 2) {
                val projStepX = size.width / (projValues.size - 1)
                val projPoints = projValues.mapIndexed { index, value ->
                    Offset(
                        x = index * projStepX,
                        y = size.height - (value.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat() * size.height) * animationProgress.value
                    )
                }
                val projPath = Path()
                projPath.moveTo(projPoints[0].x, projPoints[0].y)
                for (i in 0 until projPoints.size - 1) {
                    val p1 = projPoints[i]
                    val p2 = projPoints[i + 1]
                    val cp1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                    val cp2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
                    projPath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                }
                drawPath(
                    path = projPath,
                    color = lineColor.copy(alpha = 0.5f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
            }
        }
        
        // Optional: Draw dots for each point (actual data only)
        points.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = point
            )
            drawCircle(
                color = dotCenterColor,
                radius = 2.dp.toPx(),
                center = point
            )
        }
    }
}
