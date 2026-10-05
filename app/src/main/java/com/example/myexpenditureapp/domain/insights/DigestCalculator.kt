package com.example.myexpenditureapp.domain.insights

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.SavingGoal
import com.example.myexpenditureapp.data.entity.Subscription
import com.example.myexpenditureapp.data.entity.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DigestCalculator {

    fun calculateDigest(
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        subscriptions: List<Subscription>,
        savingGoals: List<SavingGoal>,
        nowTimestamp: Long = System.currentTimeMillis()
    ): SmartDigestModel {
        val cal = Calendar.getInstance().apply { timeInMillis = nowTimestamp }
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining = (totalDaysInMonth - currentDay + 1).coerceAtLeast(1)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)

        // Filter this month's transactions
        val thisMonthTxs = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.MONTH) + 1 == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }

        val expenses = thisMonthTxs.filter { it.type == "Expense" }
        val incomes = thisMonthTxs.filter { it.type == "Income" }

        val totalSpent = expenses.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val totalIncome = incomes.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }

        val totalBudget = budgets
            .filter { it.month == currentMonth && it.year == currentYear }
            .fold(BigDecimal.ZERO) { acc, b -> acc.add(b.limitAmount) }

        // Pacing metrics
        val effectiveBaseline = if (totalBudget > BigDecimal.ZERO) totalBudget else totalIncome
        val budgetConsumedPercent = if (effectiveBaseline > BigDecimal.ZERO) {
            totalSpent.divide(effectiveBaseline, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100)).toInt()
        } else 0

        val expectedTimePercent = ((currentDay.toDouble() / totalDaysInMonth) * 100).toInt()
        val pacingDeltaPercent = budgetConsumedPercent - expectedTimePercent
        val isSpendingSlower = pacingDeltaPercent <= 0

        val remainingBudget = if (totalBudget > BigDecimal.ZERO) {
            totalBudget.subtract(totalSpent).max(BigDecimal.ZERO)
        } else {
            totalIncome.subtract(totalSpent).max(BigDecimal.ZERO)
        }

        val safeDailySpend = if (daysRemaining > 0 && remainingBudget > BigDecimal.ZERO) {
            remainingBudget.divide(BigDecimal(daysRemaining), 0, RoundingMode.DOWN)
        } else BigDecimal.ZERO

        val pacingBufferAmount = if (effectiveBaseline > BigDecimal.ZERO) {
            val idealSpendToDate = effectiveBaseline.multiply(BigDecimal(currentDay))
                .divide(BigDecimal(totalDaysInMonth), 2, RoundingMode.HALF_UP)
            idealSpendToDate.subtract(totalSpent).abs()
        } else BigDecimal.ZERO

        // Health Status Determination
        val healthStatus: HealthStatus
        val healthScore: Int
        if (totalBudget > BigDecimal.ZERO) {
            when {
                budgetConsumedPercent > 100 -> {
                    healthStatus = HealthStatus.CRITICAL
                    healthScore = (100 - (budgetConsumedPercent - 100) * 2).coerceIn(10, 40)
                }
                pacingDeltaPercent > 15 -> {
                    healthStatus = HealthStatus.STRETCHED
                    healthScore = 55
                }
                pacingDeltaPercent > 5 -> {
                    healthStatus = HealthStatus.GUARDED
                    healthScore = 70
                }
                else -> {
                    healthStatus = HealthStatus.OPTIMAL
                    healthScore = (85 + (expectedTimePercent - budgetConsumedPercent)).coerceIn(80, 98)
                }
            }
        } else {
            if (totalIncome > BigDecimal.ZERO && totalSpent > totalIncome) {
                healthStatus = HealthStatus.STRETCHED
                healthScore = 50
            } else {
                healthStatus = HealthStatus.OPTIMAL
                healthScore = 85
            }
        }

        // Leak Radar: Find category with highest spend in the last 7 days with >= 3 micro-transactions
        val sevenDaysAgo = nowTimestamp - (7L * 24 * 60 * 60 * 1000)
        val recentTxs = expenses.filter { it.timestamp >= sevenDaysAgo }
        val categoryMap = categories.associateBy { it.id }

        val categoryGroups = recentTxs.groupBy { it.categoryId }
        var topLeakAlert: LeakAlert? = null
        for ((catId, txList) in categoryGroups) {
            if (txList.size >= 2) {
                val sum = txList.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
                if (sum > BigDecimal("500")) {
                    val cat = catId?.let { categoryMap[it] }
                    topLeakAlert = LeakAlert(
                        categoryName = cat?.name ?: "Frequent Spends",
                        categoryIcon = cat?.icon ?: "⚡",
                        spentThisWeek = sum,
                        spikePercentage = 25,
                        microTransactionCount = txList.size
                    )
                    break
                }
            }
        }

        // Upcoming subscriptions due in next 7 days
        val upcomingSubs = subscriptions.filter { sub ->
            sub.isActive && sub.dueDayOfMonth in currentDay..(currentDay + 7)
        }
        val upcomingSubsAmount = upcomingSubs.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amount) }

        // Unreviewed count
        val unreviewedCount = transactions.count { !it.isReviewed }

        // Tactical Action Step
        val tacticalAction: TacticalAction? = when {
            unreviewedCount > 0 -> {
                TacticalAction(
                    title = "$unreviewedCount New Spends to Review",
                    description = "Clear unreviewed SMS transactions to keep your daily pace accurate.",
                    buttonText = "Review Now",
                    actionType = ActionType.REVIEW_TRANSACTIONS
                )
            }
            isSpendingSlower && remainingBudget > BigDecimal("3000") && savingGoals.any { !it.isCompleted } -> {
                val targetGoal = savingGoals.firstOrNull { !it.isCompleted }
                val suggestedSweep = remainingBudget.multiply(BigDecimal("0.25")).setScale(0, RoundingMode.DOWN)
                TacticalAction(
                    title = "Lock Surplus to Goal",
                    description = "You're spending slower than planned. Safely sweep funds to '${targetGoal?.name ?: "Savings"}'.",
                    buttonText = "Save Funds",
                    actionType = ActionType.SWEEP_TO_GOAL,
                    targetGoalId = targetGoal?.id,
                    suggestedAmount = suggestedSweep
                )
            }
            healthStatus in listOf(HealthStatus.STRETCHED, HealthStatus.CRITICAL) -> {
                TacticalAction(
                    title = "Tighten Daily Pacing",
                    description = "Adjust category limits or pause non-essential spends for the next 7 days.",
                    buttonText = "Manage Budgets",
                    actionType = ActionType.REDUCE_BUDGET
                )
            }
            upcomingSubs.isNotEmpty() -> {
                TacticalAction(
                    title = "${upcomingSubs.size} Bills Due This Week",
                    description = "Ensure sufficient balance to cover upcoming recurring charges.",
                    buttonText = "View Bills",
                    actionType = ActionType.VIEW_SUBSCRIPTIONS
                )
            }
            else -> null
        }

        return SmartDigestModel(
            healthStatus = healthStatus,
            healthScore = healthScore,
            safeDailySpend = safeDailySpend,
            daysRemainingInMonth = daysRemaining,
            totalDaysInMonth = totalDaysInMonth,
            currentDayOfMonth = currentDay,
            monthName = monthName,
            totalSpentThisMonth = totalSpent,
            totalBudgetThisMonth = totalBudget,
            totalIncomeThisMonth = totalIncome,
            budgetConsumedPercent = budgetConsumedPercent,
            expectedTimeElapsedPercent = expectedTimePercent,
            pacingDeltaPercent = pacingDeltaPercent,
            pacingBufferAmount = pacingBufferAmount,
            isSpendingSlower = isSpendingSlower,
            leakAlert = topLeakAlert,
            upcomingSubscriptionsCount = upcomingSubs.size,
            upcomingSubscriptionsAmount = upcomingSubsAmount,
            unreviewedTransactionsCount = unreviewedCount,
            tacticalAction = tacticalAction
        )
    }
}
