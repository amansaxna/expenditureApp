package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
    onTotalBalanceClick: () -> Unit = {},
    onSpentMonthClick: () -> Unit = {},
    onTodayBurnClick: () -> Unit = {},
    onSafeAllowanceClick: () -> Unit = {},
    onHealthBadgeClick: () -> Unit = {},
    onLeakRadarClick: () -> Unit = {},
    onUpcomingBillsClick: () -> Unit = {},
    onSaveBudgetLimit: ((BigDecimal) -> Unit)? = null,
    onTacticalActionClick: (ActionType) -> Unit = {},
    onCardClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var showEditAllowanceDialog by remember { mutableStateOf(false) }
    var showHealthInfoDialog by remember { mutableStateOf(false) }

    val statusColor = when (digest.healthStatus) {
        HealthStatus.OPTIMAL -> IncomeGreen
        HealthStatus.GUARDED -> AmberWarning
        HealthStatus.STRETCHED -> Color(0xFFFF9800)
        HealthStatus.CRITICAL -> ExpenseRed
    }

    if (showEditAllowanceDialog) {
        EditSafeAllowanceDialog(
            digest = digest,
            onSaveBudgetLimit = { newLimit ->
                onSaveBudgetLimit?.invoke(newLimit)
            },
            onDismiss = { showEditAllowanceDialog = false }
        )
    }

    if (showHealthInfoDialog) {
        HealthScoreBreakdownDialog(
            digest = digest,
            onDismiss = { showHealthInfoDialog = false }
        )
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTotalBalanceClick()
                    }
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
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showHealthInfoDialog = true
                        onHealthBadgeClick()
                    },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTotalBalanceClick()
                    }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSpentMonthClick()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
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
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTodayBurnClick()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
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
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showEditAllowanceDialog = true
                                onSafeAllowanceClick()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
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
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showEditAllowanceDialog = true
                            onSafeAllowanceClick()
                        }
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
                    color = if (digest.isSpendingSlower) IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f),
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showHealthInfoDialog = true
                        onHealthBadgeClick()
                    }
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
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLeakRadarClick()
                        },
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
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpcomingBillsClick()
                        },
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

@Composable
fun EditSafeAllowanceDialog(
    digest: SmartDigestModel,
    onSaveBudgetLimit: (BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    var inputAmountText by remember { 
        mutableStateOf(
            if (digest.totalBudgetThisMonth > BigDecimal.ZERO) 
                digest.totalBudgetThisMonth.stripTrailingZeros().toPlainString()
            else ""
        ) 
    }
    
    val currentSpent = digest.totalSpentThisMonth
    val remainingDays = digest.daysRemainingInMonth.coerceAtLeast(1)
    
    val parsedInput = inputAmountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val remainingBudget = parsedInput.subtract(currentSpent).max(BigDecimal.ZERO)
    val calculatedDailyAllowance = if (remainingDays > 0 && remainingBudget > BigDecimal.ZERO) {
        remainingBudget.divide(BigDecimal(remainingDays), 0, java.math.RoundingMode.DOWN)
    } else BigDecimal.ZERO

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Edit Safe Daily Allowance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Adjust your monthly target budget limit to recalculate your daily safe spend pace across the remaining $remainingDays days of ${digest.monthName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputAmountText,
                    onValueChange = { inputAmountText = it },
                    label = { Text("Monthly Target Budget (₹)") },
                    placeholder = { Text("e.g. 25000") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15000, 25000, 35000, 50000).forEach { preset ->
                        FilterChip(
                            selected = inputAmountText == preset.toString(),
                            onClick = { inputAmountText = preset.toString() },
                            label = { Text("₹${preset.formatIndian()}") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text(
                            text = "CALCULATED SAFE ALLOWANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${calculatedDailyAllowance.formatIndian(includeSymbol = true)} / day",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonospaceFont),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Based on ${remainingBudget.formatIndian(includeSymbol = true)} left over $remainingDays days",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedInput > BigDecimal.ZERO) {
                        onSaveBudgetLimit(parsedInput)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Allowance Target", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HealthScoreBreakdownDialog(
    digest: SmartDigestModel,
    onDismiss: () -> Unit
) {
    val statusColor = when (digest.healthStatus) {
        HealthStatus.OPTIMAL -> IncomeGreen
        HealthStatus.GUARDED -> AmberWarning
        HealthStatus.STRETCHED -> Color(0xFFFF9800)
        HealthStatus.CRITICAL -> ExpenseRed
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = statusColor)
                Text("Financial Health Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("STATUS: ${digest.healthStatus.label.uppercase()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                            Text("${digest.healthScore} / 100", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonospaceFont), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(digest.healthStatus.emoji, fontSize = 28.sp)
                    }
                }

                Text(
                    text = if (digest.isSpendingSlower) {
                        "🟢 Your financial health is OPTIMAL! You are spending ${kotlin.math.abs(digest.pacingDeltaPercent)}% slower than your expected monthly timeline."
                    } else {
                        "⚠️ Your spending is pacing ${digest.pacingDeltaPercent}% faster than your monthly timeline. Tapering non-essential expenses will improve your health score."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• Budget Consumed: ${digest.budgetConsumedPercent}%", style = MaterialTheme.typography.labelMedium)
                    Text("• Month Elapsed: Day ${digest.currentDayOfMonth} of ${digest.totalDaysInMonth} (${digest.expectedTimeElapsedPercent}%)", style = MaterialTheme.typography.labelMedium)
                    Text("• Safe Buffer Left: ${digest.pacingBufferAmount.formatIndian(includeSymbol = true)}", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
}
