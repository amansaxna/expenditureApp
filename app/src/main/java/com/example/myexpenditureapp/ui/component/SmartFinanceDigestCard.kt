package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.domain.insights.ActionType
import com.example.myexpenditureapp.domain.insights.HealthStatus
import com.example.myexpenditureapp.domain.insights.SmartDigestModel
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal

@Composable
fun SmartFinanceDigestCard(
    digest: SmartDigestModel,
    modifier: Modifier = Modifier,
    onTacticalActionClick: (ActionType) -> Unit = {},
    onCardClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    val statusColor = when (digest.healthStatus) {
        HealthStatus.OPTIMAL -> IncomeGreen
        HealthStatus.GUARDED -> AmberWarning
        HealthStatus.STRETCHED -> Color(0xFFFF9800)
        HealthStatus.CRITICAL -> ExpenseRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable { onCardClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Section Label + Health Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TOTAL LIQUID BALANCE & DIGEST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Health Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(7.dp),
                            shape = CircleShape,
                            color = statusColor
                        ) {}
                        Text(
                            text = "${digest.healthStatus.label.uppercase()} • ${digest.healthScore}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                }
            }

            // PRIMARY HERO: Total Liquid Balance
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = digest.totalLiquidBalance.formatIndian(includeSymbol = true, includeDecimals = false),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = MonospaceFont,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Across all active accounts and wallets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3-Metric Snapshot Row (Spent This Month • Today's Burn • Safe Allowance)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spent This Month
                    Column {
                        Text(
                            text = "Spent (${digest.monthName.take(3)})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = digest.totalSpentThisMonth.formatIndian(includeSymbol = true, includeDecimals = false),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )

                    // Today's Burn
                    Column {
                        Text(
                            text = "Today's Burn",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = digest.todayBurn.formatIndian(includeSymbol = true, includeDecimals = false),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (digest.todayBurn > BigDecimal.ZERO) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )

                    // Safe Daily Allowance
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Safe Allowance",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${digest.safeDailySpend.formatIndian(includeSymbol = true, includeDecimals = false)}/d",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (digest.isSpendingSlower) IncomeGreen else statusColor
                        )
                    }
                }
            }

            // Beat 2: Timeline Velocity & Pacing Track
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Day ${digest.currentDayOfMonth} of ${digest.totalDaysInMonth} (${digest.expectedTimeElapsedPercent}% month elapsed)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Budget Used: ${digest.budgetConsumedPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (digest.isSpendingSlower) IncomeGreen else ExpenseRed
                    )
                }

                // Dual Progress Track (Month Timeline Elapsed vs Budget Consumed)
                val timeRatio = (digest.expectedTimeElapsedPercent / 100f).coerceIn(0f, 1f)
                val budgetRatio = (digest.budgetConsumedPercent / 100f).coerceIn(0f, 1f)
                val activeColor = if (digest.isSpendingSlower) IncomeGreen else ExpenseRed
                val trackBg = MaterialTheme.colorScheme.surfaceVariant
                val timelineTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                val markerColor = MaterialTheme.colorScheme.primary

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    val w = size.width
                    val h = size.height
                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)

                    // 1. Base track background
                    drawRoundRect(
                        color = trackBg,
                        size = size,
                        cornerRadius = cornerRadius
                    )

                    // 2. Month Elapsed Track (shaded up to expectedTimeElapsedPercent)
                    if (timeRatio > 0f) {
                        drawRoundRect(
                            color = timelineTrackColor,
                            size = androidx.compose.ui.geometry.Size(w * timeRatio, h),
                            cornerRadius = cornerRadius
                        )
                    }

                    // 3. Budget Consumed Bar
                    if (budgetRatio > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(activeColor, activeColor.copy(alpha = 0.85f))
                            ),
                            size = androidx.compose.ui.geometry.Size(w * budgetRatio, h),
                            cornerRadius = cornerRadius
                        )
                    }

                    // 4. Timeline Marker Line / Tick at Month Elapsed position
                    if (timeRatio in 0.01f..0.99f) {
                        val markerX = w * timeRatio
                        drawLine(
                            color = markerColor,
                            start = androidx.compose.ui.geometry.Offset(markerX, 0f),
                            end = androidx.compose.ui.geometry.Offset(markerX, h),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Dynamic Pacing Insight Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (digest.isSpendingSlower) IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (digest.isSpendingSlower) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (digest.isSpendingSlower) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (digest.isSpendingSlower) {
                                "Spending ${kotlin.math.abs(digest.pacingDeltaPercent)}% slower than schedule (${digest.pacingBufferAmount.formatIndian(includeSymbol = true)} safe buffer)"
                            } else {
                                "Pacing is ${digest.pacingDeltaPercent}% ahead of month timeline. Recommended to taper non-essentials."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (digest.isSpendingSlower) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Beat 3: Bento Micro-Insights (Leak Radar & Upcoming Bills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Leak Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = digest.leakAlert?.categoryIcon ?: "🔍", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LEAK RADAR",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = digest.leakAlert?.categoryName ?: "No Leaks",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (digest.leakAlert != null) {
                                "${digest.leakAlert.spentThisWeek.formatIndian(includeSymbol = true)} (${digest.leakAlert.microTransactionCount} txs)"
                            } else {
                                "Well controlled"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = MonospaceFont),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Subscriptions / Bills Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UPCOMING (7D)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (digest.upcomingSubscriptionsCount > 0) "${digest.upcomingSubscriptionsCount} Bills Due" else "No Bills Due",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (digest.upcomingSubscriptionsCount > 0) {
                                digest.upcomingSubscriptionsAmount.formatIndian(includeSymbol = true)
                            } else {
                                "Zero recurring"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = MonospaceFont),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Beat 4: Tactical Action Step
            if (digest.tacticalAction != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTacticalActionClick(digest.tacticalAction.actionType)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = digest.tacticalAction.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = digest.tacticalAction.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = digest.tacticalAction.buttonText,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
