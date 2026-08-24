package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.ui.theme.*

import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0.0) return

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val chartColors = listOf(
        Gold, GoldDark, NavyLight, Slate, IncomeGreen, ExpenseRed
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(220.dp)) {
                var startAngle = -90f
                data.values.forEachIndexed { index, value ->
                    val sweepAngle = (value / total * 360).toFloat() * animationProgress.value
                    drawArc(
                        color = chartColors[index % chartColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width - 40.dp.toPx(), size.height - 40.dp.toPx()),
                        topLeft = Offset(20.dp.toPx(), 20.dp.toPx())
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = String.format(Locale.getDefault(), "₹ %.0f", total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Legend in a grid-like flow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            maxItemsInEachRow = 3
        ) {
            data.keys.forEachIndexed { index, key ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = CircleShape,
                        color = chartColors[index % chartColors.size]
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val maxVal = data.values.maxOrNull() ?: 1.0
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            val barWidth = size.width / (data.size * 2).coerceAtLeast(1)
            var x = barWidth / 2
            
            data.values.forEach { value ->
                val barHeight = (value / maxVal * size.height).toFloat() * animationProgress.value
                drawRect(
                    color = Gold,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
                // Background bar
                drawRect(
                    color = NavyLight.copy(alpha = 0.1f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height)
                )
                x += barWidth * 2
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            data.keys.forEach { key ->
                Text(
                    text = key.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun LineChart(
    data: Map<Long, Double>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val maxVal = data.values.maxOrNull() ?: 1.0
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val values = data.values.toList()
    
    Canvas(modifier = modifier.height(150.dp).fillMaxWidth()) {
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        
        val points = values.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = size.height - (value / maxVal * size.height).toFloat() * animationProgress.value
            )
        }
        
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Gold,
                start = points[i],
                end = points[i+1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Fill area under line
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points[i].x, size.height)
                lineTo(points[i].x, points[i].y)
                lineTo(points[i+1].x, points[i+1].y)
                lineTo(points[i+1].x, size.height)
                close()
            }
            drawPath(
                path = path,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Gold.copy(alpha = 0.3f), Color.Transparent)
                )
            )
        }
    }
}
