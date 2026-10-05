package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
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
            .clip(RoundedCornerShape(24.dp))
            .clickable { onCardClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "SMART FINANCE DIGEST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(7.dp),
                            shape = CircleShape,
                            color = statusColor
                        ) {}
                        Text(
                            text = "${digest.healthStatus.label.uppercase()} • ${digest.healthScore}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Beat 1: The Vital Pulse (Safe Daily Spend)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "SAFE DAILY ALLOWANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = digest.safeDailySpend.formatIndian(includeSymbol = true, includeDecimals = false),
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = MonospaceFont),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = "Calculated for the remaining ${digest.daysRemainingInMonth} days in ${digest.monthName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )

            // Beat 2: Timeline Velocity & Pacing
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

                // Visual Pacing Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // Budget Consumed Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((digest.budgetConsumedPercent / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        if (digest.isSpendingSlower) IncomeGreen else ExpenseRed,
                                        if (digest.isSpendingSlower) IncomeGreen.copy(alpha = 0.8f) else ExpenseRed.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }

                // Pacing Insight Pill
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

            // Beat 3: Bento Micro-Insights (Leak Alert & Upcoming Bills)
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
