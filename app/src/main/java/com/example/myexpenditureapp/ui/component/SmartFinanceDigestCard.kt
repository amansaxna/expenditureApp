package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import java.text.SimpleDateFormat
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
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.insights.ActionType
import com.example.myexpenditureapp.domain.insights.DigestCalculator
import com.example.myexpenditureapp.domain.insights.HealthStatus
import com.example.myexpenditureapp.domain.insights.MicroSpendAnalyzer
import com.example.myexpenditureapp.domain.insights.SmartDigestModel
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

@Composable
fun SmartFinanceDigestCard(
    digest: SmartDigestModel,
    modifier: Modifier = Modifier,
    allTransactions: List<Transaction> = emptyList(),
    allCategories: List<Category> = emptyList(),
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
    var showMicroDetailDialog by remember { mutableStateOf(false) }
    var showMajorDetailDialog by remember { mutableStateOf(false) }
    var showThresholdEditorDialog by remember { mutableStateOf(false) }
    
    var isOverallHistory by remember { mutableStateOf(false) }
    var customThresholdOverride by remember { mutableStateOf<BigDecimal?>(null) }

    val activeDigest = remember(digest, isOverallHistory, allTransactions, allCategories) {
        if (isOverallHistory && allTransactions.isNotEmpty()) {
            DigestCalculator.calculateDigest(
                transactions = allTransactions,
                categories = allCategories,
                budgets = emptyList<com.example.myexpenditureapp.data.entity.Budget>(),
                subscriptions = emptyList<com.example.myexpenditureapp.data.entity.Subscription>(),
                savingGoals = emptyList<com.example.myexpenditureapp.data.entity.SavingGoal>(),
                totalLiquidBalance = digest.totalLiquidBalance,
                isOverallHistory = true
            )
        } else {
            digest
        }
    }

    val statusColor = when (activeDigest.healthStatus) {
        HealthStatus.OPTIMAL -> IncomeGreen
        HealthStatus.GUARDED -> AmberWarning
        HealthStatus.STRETCHED -> Color(0xFFFF9800)
        HealthStatus.CRITICAL -> ExpenseRed
    }

    val currentCal = Calendar.getInstance()
    val currentMonth = currentCal.get(Calendar.MONTH) + 1
    val currentYear = currentCal.get(Calendar.YEAR)

    val targetTransactions = remember(allTransactions, isOverallHistory) {
        if (isOverallHistory) {
            allTransactions
        } else {
            allTransactions.filter { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                c.get(Calendar.MONTH) + 1 == currentMonth && c.get(Calendar.YEAR) == currentYear
            }
        }
    }

    val microSummary = remember(targetTransactions, allCategories, activeDigest.safeDailySpend, customThresholdOverride) {
        MicroSpendAnalyzer.analyze(
            transactions = targetTransactions,
            categories = allCategories,
            safeDailyAllowance = activeDigest.safeDailySpend,
            customThresholdOverride = customThresholdOverride
        )
    }

    if (showEditAllowanceDialog) {
        EditSafeAllowanceDialog(
            digest = activeDigest,
            onSaveBudgetLimit = { newLimit ->
                onSaveBudgetLimit?.invoke(newLimit)
            },
            onDismiss = { showEditAllowanceDialog = false }
        )
    }

    if (showHealthInfoDialog) {
        HealthScoreBreakdownDialog(
            digest = activeDigest,
            onDismiss = { showHealthInfoDialog = false }
        )
    }

    if (showMicroDetailDialog) {
        MicroExpenditureDetailDialog(
            allTransactions = allTransactions,
            allCategories = allCategories,
            safeDailySpend = activeDigest.safeDailySpend,
            onEditThresholdClick = { showThresholdEditorDialog = true },
            onViewAllClick = onSpentMonthClick,
            onDismiss = { showMicroDetailDialog = false }
        )
    }

    if (showMajorDetailDialog) {
        MajorExpenditureDetailDialog(
            allTransactions = allTransactions,
            allCategories = allCategories,
            safeDailySpend = activeDigest.safeDailySpend,
            onViewAllClick = onSpentMonthClick,
            onDismiss = { showMajorDetailDialog = false }
        )
    }

    if (showThresholdEditorDialog) {
        BaselineThresholdEditorDialog(
            currentBaseline = microSummary.effectiveThreshold,
            onSaveThreshold = { newThreshold -> customThresholdOverride = newThreshold },
            onDismiss = { showThresholdEditorDialog = false }
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
            // Header Row: Title + Time Horizon Toggle + Health Badge
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
                        text = if (isOverallHistory) "OVERALL DIGEST" else "MONTH DIGEST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Segmented Switch Control (Month vs Overall)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month Segment Pill
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isOverallHistory) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isOverallHistory = false
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isOverallHistory) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Text(
                                    text = digest.monthName.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (!isOverallHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            // Overall Segment Pill
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (!isOverallHistory) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isOverallHistory = true
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isOverallHistory) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Text(
                                    text = "Overall",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOverallHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
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
                                text = "${activeDigest.healthStatus.label.uppercase()} • ${activeDigest.healthScore}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                        }
                    }
                }
            }

            // PRIMARY HERO: Monthly / Cumulative Outflow
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpentMonthClick()
                    }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = activeDigest.totalSpentThisMonth.formatIndian(includeSymbol = true, includeDecimals = false),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = MonospaceFont,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ExpenseRed.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, ExpenseRed.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (isOverallHistory) "ALL-TIME SPENT" else "${activeDigest.monthName.take(3).uppercase()} SPENT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = ExpenseRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = if (isOverallHistory) "Total outflow across all recorded transactions" else "Total outflow in ${activeDigest.monthName} across all accounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3-Metric Snapshot Row (Net Balance • Today's Burn • Safe Allowance)
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
                    // Net Balance (Liquid Funds across all accounts)
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTotalBalanceClick()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Net Balance",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeDigest.totalLiquidBalance.formatIndian(includeSymbol = true, includeDecimals = false),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (activeDigest.totalLiquidBalance < BigDecimal.ZERO) ExpenseRed else MaterialTheme.colorScheme.onSurface
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
                            text = activeDigest.todayBurn.formatIndian(includeSymbol = true, includeDecimals = false),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (activeDigest.todayBurn > BigDecimal.ZERO) ExpenseRed else MaterialTheme.colorScheme.onSurface
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
                            text = "${activeDigest.safeDailySpend.formatIndian(includeSymbol = true, includeDecimals = false)}/d",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (activeDigest.isSpendingSlower) IncomeGreen else statusColor
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
                        text = if (isOverallHistory) "All-Time Spending Performance" else "Day ${activeDigest.currentDayOfMonth} of ${activeDigest.totalDaysInMonth} (${activeDigest.expectedTimeElapsedPercent}% month elapsed)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isOverallHistory) "Inflow: ${activeDigest.totalIncomeThisMonth.formatIndian(includeSymbol = true)}" else "Budget Used: ${activeDigest.budgetConsumedPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activeDigest.isSpendingSlower) IncomeGreen else ExpenseRed
                    )
                }

                // Dual Progress Track
                val timeRatio = (activeDigest.expectedTimeElapsedPercent / 100f).coerceIn(0f, 1f)
                val budgetRatio = (activeDigest.budgetConsumedPercent / 100f).coerceIn(0f, 1f)
                val activeColor = if (activeDigest.isSpendingSlower) IncomeGreen else ExpenseRed
                val trackBg = MaterialTheme.colorScheme.surfaceVariant
                val timelineTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                val markerColor = MaterialTheme.colorScheme.primary

                Canvas(
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

                    drawRoundRect(color = trackBg, size = size, cornerRadius = cornerRadius)
                    if (timeRatio > 0f) {
                        drawRoundRect(
                            color = timelineTrackColor,
                            size = androidx.compose.ui.geometry.Size(w * timeRatio, h),
                            cornerRadius = cornerRadius
                        )
                    }
                    if (budgetRatio > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(activeColor, activeColor.copy(alpha = 0.85f))),
                            size = androidx.compose.ui.geometry.Size(w * budgetRatio, h),
                            cornerRadius = cornerRadius
                        )
                    }
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
            }

            // NEW: Micro & Major Expenditure Analysis Bento Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Micro Spend Bento Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showMicroDetailDialog = true
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "☕", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MICRO (<${microSummary.effectiveThreshold.formatIndian(includeSymbol = true, includeDecimals = false)})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = microSummary.totalMicroSpend.formatIndian(includeSymbol = true, includeDecimals = false),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonospaceFont),
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        Text(
                            text = "${microSummary.microSpendPercentage}% share (${microSummary.microTxCount} txs)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Major Spend Bento Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showMajorDetailDialog = true
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🐘", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MAJOR (>${microSummary.effectiveThreshold.formatIndian(includeSymbol = true, includeDecimals = false)})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = microSummary.totalMacroSpend.formatIndian(includeSymbol = true, includeDecimals = false),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonospaceFont),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${100 - microSummary.microSpendPercentage}% share (${microSummary.macroTxCount} txs)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            showMicroDetailDialog = true
                            onLeakRadarClick()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = activeDigest.leakAlert?.categoryIcon ?: "🔍", fontSize = 14.sp)
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
                            text = activeDigest.leakAlert?.categoryName ?: "No Leaks",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeDigest.leakAlert != null) {
                                "${activeDigest.leakAlert.spentThisWeek.formatIndian(includeSymbol = true)} (${activeDigest.leakAlert.microTransactionCount} txs)"
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
                            text = if (activeDigest.upcomingSubscriptionsCount > 0) "${activeDigest.upcomingSubscriptionsCount} Bills Due" else "No Bills Due",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeDigest.upcomingSubscriptionsCount > 0) {
                                activeDigest.upcomingSubscriptionsAmount.formatIndian(includeSymbol = true)
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
            if (activeDigest.tacticalAction != null) {
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
                                onTacticalActionClick(activeDigest.tacticalAction.actionType)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeDigest.tacticalAction.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeDigest.tacticalAction.description,
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
                                contentDescription = activeDigest.tacticalAction.buttonText,
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
fun MicroExpenditureDetailDialog(
    allTransactions: List<Transaction>,
    allCategories: List<Category>,
    safeDailySpend: BigDecimal,
    onEditThresholdClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var isOverallHistory by remember { mutableStateOf(false) }
    val currentCal = Calendar.getInstance()
    val currentMonth = currentCal.get(Calendar.MONTH) + 1
    val currentYear = currentCal.get(Calendar.YEAR)

    val targetTxs = remember(allTransactions, isOverallHistory) {
        if (isOverallHistory) allTransactions else allTransactions.filter { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            c.get(Calendar.MONTH) + 1 == currentMonth && c.get(Calendar.YEAR) == currentYear
        }
    }

    val summary = remember(targetTxs, allCategories, safeDailySpend) {
        MicroSpendAnalyzer.analyze(
            transactions = targetTxs,
            categories = allCategories,
            safeDailyAllowance = safeDailySpend
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("☕", fontSize = 18.sp)
                    Text("Small Expenditures", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { isOverallHistory = !isOverallHistory }
                ) {
                    Text(
                        text = if (isOverallHistory) "All-Time" else "This Month",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MICRO THRESHOLD LIMIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("<${summary.effectiveThreshold.formatIndian(includeSymbol = true, includeDecimals = false)} per purchase", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onEditThresholdClick) {
                            Text("Edit Baseline", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Small Spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${summary.totalMicroSpend.formatIndian(includeSymbol = true)} (${summary.microSpendPercentage}%)",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonospaceFont),
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text("TOP 3 SMALL TRANSACTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                if (summary.microTransactions.isEmpty()) {
                    Text("No small transactions recorded.", style = MaterialTheme.typography.bodySmall)
                } else {
                    summary.microTransactions.take(3).forEach { tx ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(tx.merchant, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(java.util.Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(tx.amount.formatIndian(includeSymbol = true), style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text("ENTIRE CATEGORY BREAKDOWN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.topMicroCategories.forEach { cat ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${cat.categoryIcon} ${cat.categoryName} (${cat.txCount})", style = MaterialTheme.typography.bodySmall)
                            Text(cat.microTotal.formatIndian(includeSymbol = true), style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onViewAllClick(); onDismiss() }, shape = RoundedCornerShape(10.dp)) {
                Text("View All Transactions", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun MajorExpenditureDetailDialog(
    allTransactions: List<Transaction>,
    allCategories: List<Category>,
    safeDailySpend: BigDecimal,
    onViewAllClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var isOverallHistory by remember { mutableStateOf(false) }
    val currentCal = Calendar.getInstance()
    val currentMonth = currentCal.get(Calendar.MONTH) + 1
    val currentYear = currentCal.get(Calendar.YEAR)

    val targetTxs = remember(allTransactions, isOverallHistory) {
        if (isOverallHistory) allTransactions else allTransactions.filter { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            c.get(Calendar.MONTH) + 1 == currentMonth && c.get(Calendar.YEAR) == currentYear
        }
    }

    val summary = remember(targetTxs, allCategories, safeDailySpend) {
        MicroSpendAnalyzer.analyze(
            transactions = targetTxs,
            categories = allCategories,
            safeDailyAllowance = safeDailySpend
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🐘", fontSize = 18.sp)
                    Text("Major Expenditures", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { isOverallHistory = !isOverallHistory }
                ) {
                    Text(
                        text = if (isOverallHistory) "All-Time" else "This Month",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Major Outflow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${summary.totalMacroSpend.formatIndian(includeSymbol = true)} (${100 - summary.microSpendPercentage}%)",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonospaceFont),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text("TOP 3 MAJOR TRANSACTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                if (summary.macroTransactions.isEmpty()) {
                    Text("No major transactions recorded above threshold.", style = MaterialTheme.typography.bodySmall)
                } else {
                    summary.macroTransactions.take(3).forEach { tx ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(tx.merchant, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(java.util.Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(tx.amount.formatIndian(includeSymbol = true), style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text("ENTIRE MAJOR CATEGORY BREAKDOWN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.allMacroCategories.forEach { cat ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${cat.categoryIcon} ${cat.categoryName} (${cat.txCount})", style = MaterialTheme.typography.bodySmall)
                            Text(cat.microTotal.formatIndian(includeSymbol = true), style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onViewAllClick(); onDismiss() }, shape = RoundedCornerShape(10.dp)) {
                Text("View All Transactions", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun BaselineThresholdEditorDialog(
    currentBaseline: BigDecimal,
    onSaveThreshold: (BigDecimal?) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf(currentBaseline.stripTrailingZeros().toPlainString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Edit Baseline Micro Threshold", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Purchases under this amount are automatically categorized as Micro expenditures. Dynamic default is Safe Daily Allowance / 2.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Custom Threshold Limit (₹)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(150, 250, 500, 1000).forEach { preset ->
                        FilterChip(
                            selected = inputText == preset.toString(),
                            onClick = { inputText = preset.toString() },
                            label = { Text(preset.formatIndian(includeSymbol = true)) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputText.toBigDecimalOrNull()
                    onSaveThreshold(parsed)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Override", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onSaveThreshold(null); onDismiss() }) {
                Text("Reset to Auto")
            }
        }
    )
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
                    placeholder = { Text("e.g. 25,000") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15000, 25000, 35000, 50000).forEach { preset ->
                        FilterChip(
                            selected = inputAmountText == preset.toString(),
                            onClick = { inputAmountText = preset.toString() },
                            label = { Text(preset.formatIndian(includeSymbol = true)) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

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
