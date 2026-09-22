package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.data.entity.SavingGoal
import com.example.myexpenditureapp.domain.settlement.MonthClosureSummary
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.math.RoundingMode

enum class SettlementChoice {
    SWEEP_TO_GOAL,
    ROLLOVER_TO_BUDGET,
    CLEAN_SLATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthEndSettlementBottomSheet(
    summary: MonthClosureSummary,
    activeGoals: List<SavingGoal>,
    onDismissRequest: () -> Unit,
    onSweepToGoal: (goalId: Long, amount: BigDecimal) -> Unit,
    onRolloverToBudget: (amount: BigDecimal) -> Unit,
    onCleanSlate: (amount: BigDecimal) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedChoice by remember {
        mutableStateOf(
            if (summary.isSurplus && activeGoals.isNotEmpty()) SettlementChoice.SWEEP_TO_GOAL
            else if (summary.isSurplus) SettlementChoice.ROLLOVER_TO_BUDGET
            else SettlementChoice.CLEAN_SLATE
        )
    }

    var selectedGoalId by remember {
        mutableStateOf(activeGoals.firstOrNull()?.id ?: 0L)
    }

    val surplusAmount = summary.netSurplus.max(BigDecimal.ZERO)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Badge & Title
            Surface(
                shape = CircleShape,
                color = if (summary.isSurplus) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (summary.isSurplus) "🏆" else "⚖️",
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${summary.monthName} Financial Wrap-Up",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Close your monthly ledger and choose what to do with your balance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Metric Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TOTAL INCOME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = summary.totalIncome.formatIndian(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL EXPENSES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = summary.totalExpense.formatIndian(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (summary.isSurplus) "NET SURPLUS (SAVINGS)" else "NET DEFICIT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.isSurplus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = summary.netSurplus.abs().formatIndian(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.isSurplus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        if (summary.isSurplus) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${summary.savingsRatePercent}% Saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT SETTLEMENT ACTION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CHOICE 1: Sweep to Saving Goal
            if (summary.isSurplus) {
                SettlementOptionCard(
                    title = "🎯 Move Surplus to Savings Goal",
                    description = "Deposit ${surplusAmount.formatIndian()} directly into your saving goals & pockets.",
                    isSelected = selectedChoice == SettlementChoice.SWEEP_TO_GOAL,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedChoice = SettlementChoice.SWEEP_TO_GOAL
                    }
                ) {
                    if (activeGoals.isEmpty()) {
                        Text(
                            text = "No active saving goals found. Create a goal from the Goals screen or choose another action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Select Target Goal:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            activeGoals.forEach { goal ->
                                val isGoalSelected = goal.id == selectedGoalId
                                val newAmount = goal.currentAmount.add(surplusAmount)
                                val currentPct = if (goal.targetAmount > BigDecimal.ZERO) {
                                    goal.currentAmount.multiply(BigDecimal(100)).divide(goal.targetAmount, 0, RoundingMode.HALF_UP).toInt()
                                } else 0
                                val newPct = if (goal.targetAmount > BigDecimal.ZERO) {
                                    newAmount.multiply(BigDecimal(100)).divide(goal.targetAmount, 0, RoundingMode.HALF_UP).toInt().coerceAtMost(100)
                                } else 0

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isGoalSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isGoalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedGoalId = goal.id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(goal.icon, fontSize = 20.sp)
                                            Column {
                                                Text(
                                                    text = goal.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${goal.currentAmount.formatIndian()} → ${newAmount.formatIndian()} ($newPct%)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        RadioButton(
                                            selected = isGoalSelected,
                                            onClick = { selectedGoalId = goal.id }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CHOICE 2: Rollover to Current Month Budget
                SettlementOptionCard(
                    title = "🔄 Rollover to This Month's Budget",
                    description = "Add ${surplusAmount.formatIndian()} to your current month's budget pool as bonus allowance.",
                    isSelected = selectedChoice == SettlementChoice.ROLLOVER_TO_BUDGET,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedChoice = SettlementChoice.ROLLOVER_TO_BUDGET
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CHOICE 3: Clean Slate
            SettlementOptionCard(
                title = "🍃 Clean Slate (Retain in Accounts)",
                description = "Keep real bank & cash balances continuous. Start this month with normal unadjusted budget limits.",
                isSelected = selectedChoice == SettlementChoice.CLEAN_SLATE,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedChoice = SettlementChoice.CLEAN_SLATE
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (selectedChoice) {
                        SettlementChoice.SWEEP_TO_GOAL -> {
                            if (selectedGoalId != 0L) {
                                onSweepToGoal(selectedGoalId, surplusAmount)
                            } else {
                                onCleanSlate(surplusAmount)
                            }
                        }
                        SettlementChoice.ROLLOVER_TO_BUDGET -> {
                            onRolloverToBudget(surplusAmount)
                        }
                        SettlementChoice.CLEAN_SLATE -> {
                            onCleanSlate(surplusAmount)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Complete Settlement & Close Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Remind Me Later",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettlementOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
            }

            if (content != null && isSelected) {
                content()
            }
        }
    }
}
