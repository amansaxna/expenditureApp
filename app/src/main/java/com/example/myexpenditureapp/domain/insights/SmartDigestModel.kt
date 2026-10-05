package com.example.myexpenditureapp.domain.insights

import java.math.BigDecimal

enum class HealthStatus(val label: String, val emoji: String) {
    OPTIMAL("Optimal", "🟢"),
    GUARDED("Guarded", "🟡"),
    STRETCHED("Stretched", "🟠"),
    CRITICAL("Critical", "🔴")
}

data class LeakAlert(
    val categoryName: String,
    val categoryIcon: String = "⚠️",
    val spentThisWeek: BigDecimal,
    val spikePercentage: Int,
    val microTransactionCount: Int
)

data class TacticalAction(
    val title: String,
    val description: String,
    val buttonText: String,
    val actionType: ActionType,
    val targetGoalId: Long? = null,
    val suggestedAmount: BigDecimal? = null
)

enum class ActionType {
    SWEEP_TO_GOAL,
    REDUCE_BUDGET,
    REVIEW_TRANSACTIONS,
    VIEW_SUBSCRIPTIONS
}

data class SmartDigestModel(
    val healthStatus: HealthStatus,
    val healthScore: Int, // 0..100
    val totalLiquidBalance: BigDecimal = BigDecimal.ZERO,
    val safeDailySpend: BigDecimal,
    val daysRemainingInMonth: Int,
    val totalDaysInMonth: Int,
    val currentDayOfMonth: Int,
    val monthName: String,
    val totalSpentThisMonth: BigDecimal,
    val todayBurn: BigDecimal = BigDecimal.ZERO,
    val totalBudgetThisMonth: BigDecimal,
    val totalIncomeThisMonth: BigDecimal,
    val budgetConsumedPercent: Int,
    val expectedTimeElapsedPercent: Int,
    val pacingDeltaPercent: Int, // positive = overspending vs timeline, negative = spending slower
    val pacingBufferAmount: BigDecimal, // buffer left or overspent amount
    val isSpendingSlower: Boolean,
    val leakAlert: LeakAlert? = null,
    val upcomingSubscriptionsCount: Int = 0,
    val upcomingSubscriptionsAmount: BigDecimal = BigDecimal.ZERO,
    val unreviewedTransactionsCount: Int = 0,
    val tacticalAction: TacticalAction? = null
)
