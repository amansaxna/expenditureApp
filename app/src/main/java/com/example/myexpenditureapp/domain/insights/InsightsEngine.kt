package com.example.myexpenditureapp.domain.insights

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

data class SmartInsight(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val type: InsightType,
    val metric: String? = null,
    val drillDownCategoryId: Long? = null,
    val drillDownMerchant: String? = null,
    val drillDownType: String? = null
)

enum class InsightType {
    POSITIVE,
    WARNING,
    NEUTRAL,
    TIP
}

object InsightsEngine {

    fun generateInsights(
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>
    ): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()
        val categoryMap = categories.associateBy { it.id }

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Filter this month's transactions
        val thisMonthTxs = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.MONTH) + 1 == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }

        val thisMonthExpenses = thisMonthTxs.filter { it.type == "Expense" }
        val thisMonthIncome = thisMonthTxs.filter { it.type == "Income" }

        val totalMonthExpense = thisMonthExpenses.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val totalMonthIncome = thisMonthIncome.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val totalBudget = budgets.filter { it.month == currentMonth && it.year == currentYear }
            .fold(BigDecimal.ZERO) { acc, b -> acc.add(b.limitAmount) }

        // 1. Savings Rate / Wealth Building
        if (totalMonthIncome > BigDecimal.ZERO) {
            val netSavings = totalMonthIncome.subtract(totalMonthExpense)
            val savingsRate = netSavings.divide(totalMonthIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()

            if (savingsRate >= 20) {
                insights.add(
                    SmartInsight(
                        id = "savings_rate_healthy",
                        title = "Strong Savings Rate: $savingsRate%",
                        description = "You are saving $savingsRate% of your income (₹${netSavings.setScale(0, RoundingMode.HALF_UP).toPlainString()}) this month. Consistent positive cashflow.",
                        icon = "💰",
                        type = InsightType.POSITIVE,
                        metric = "$savingsRate% Saved",
                        drillDownType = "Income"
                    )
                )
            } else if (savingsRate < 0) {
                val deficit = netSavings.abs()
                insights.add(
                    SmartInsight(
                        id = "savings_rate_negative",
                        title = "Negative Cashflow Alert",
                        description = "Expenditures exceed your total logged income by ₹${deficit.setScale(0, RoundingMode.HALF_UP).toPlainString()} this month.",
                        icon = "🚨",
                        type = InsightType.WARNING,
                        metric = "-₹${deficit.toPlainString()}",
                        drillDownType = "Expense"
                    )
                )
            }
        }

        // 2. Budget Pacing / Safety Score Insight
        if (totalBudget > BigDecimal.ZERO && totalMonthExpense > BigDecimal.ZERO) {
            val expectedPace = BigDecimal(dayOfMonth).divide(BigDecimal(daysInMonth), 4, RoundingMode.HALF_UP)
            val expectedSpendAtThisDay = totalBudget.multiply(expectedPace)

            if (totalMonthExpense > totalBudget) {
                val overspend = totalMonthExpense.subtract(totalBudget)
                insights.add(
                    SmartInsight(
                        id = "budget_exceeded",
                        title = "Monthly Budget Exceeded",
                        description = "You have exceeded your total monthly budget by ₹${overspend.setScale(0, RoundingMode.HALF_UP).toPlainString()}.",
                        icon = "⚠️",
                        type = InsightType.WARNING,
                        metric = "₹${totalMonthExpense.toPlainString()} / ₹${totalBudget.toPlainString()}",
                        drillDownType = "Expense"
                    )
                )
            } else if (totalMonthExpense < expectedSpendAtThisDay) {
                val savings = expectedSpendAtThisDay.subtract(totalMonthExpense)
                insights.add(
                    SmartInsight(
                        id = "budget_pacing_good",
                        title = "Under Budget Pacing",
                        description = "You're spending ~₹${savings.setScale(0, RoundingMode.HALF_UP).toPlainString()} below expected pace for day $dayOfMonth of $daysInMonth. Excellent discipline!",
                        icon = "🌟",
                        type = InsightType.POSITIVE,
                        metric = "On Track",
                        drillDownType = "Expense"
                    )
                )
            } else {
                insights.add(
                    SmartInsight(
                        id = "budget_pacing_caution",
                        title = "High Spending Velocity",
                        description = "Your current spending pace is running slightly ahead of the midpoint for this month.",
                        icon = "⚡",
                        type = InsightType.WARNING,
                        metric = "${(totalMonthExpense.divide(totalBudget, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100))).toInt()}% spent",
                        drillDownType = "Expense"
                    )
                )
            }
        }

        // 3. Weekend vs Weekday Lifestyle Behavior
        if (thisMonthExpenses.size >= 4 && totalMonthExpense > BigDecimal.ZERO) {
            var weekendSpend = BigDecimal.ZERO
            var weekdaySpend = BigDecimal.ZERO

            thisMonthExpenses.forEach { tx ->
                val dayOfWeek = Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    weekendSpend = weekendSpend.add(tx.amount)
                } else {
                    weekdaySpend = weekdaySpend.add(tx.amount)
                }
            }

            val weekendPct = weekendSpend.divide(totalMonthExpense, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
            if (weekendPct >= 50) {
                insights.add(
                    SmartInsight(
                        id = "weekend_heavy",
                        title = "Weekend Lifestyle Spike: $weekendPct%",
                        description = "Over half of your monthly outlays (₹${weekendSpend.setScale(0, RoundingMode.HALF_UP).toPlainString()}) occur on Saturdays & Sundays.",
                        icon = "🎉",
                        type = InsightType.TIP,
                        metric = "$weekendPct% on Weekends",
                        drillDownType = "Expense"
                    )
                )
            }
        }

        // 4. Daily Burn Rate & Projected Total
        if (dayOfMonth > 0 && totalMonthExpense > BigDecimal.ZERO) {
            val dailyBurnRate = totalMonthExpense.divide(BigDecimal(dayOfMonth), 2, RoundingMode.HALF_UP)
            val projectedMonthEnd = dailyBurnRate.multiply(BigDecimal(daysInMonth)).setScale(0, RoundingMode.HALF_UP)
            insights.add(
                SmartInsight(
                    id = "daily_burn",
                    title = "Daily Burn: ₹${dailyBurnRate.setScale(0, RoundingMode.HALF_UP).toPlainString()}/day",
                    description = "At this trajectory, your projected month-end total expense will be approx ₹${projectedMonthEnd.toPlainString()}.",
                    icon = "🔥",
                    type = InsightType.NEUTRAL,
                    metric = "₹${dailyBurnRate.setScale(0, RoundingMode.HALF_UP).toPlainString()}/d",
                    drillDownType = "Expense"
                )
            )
        }

        // 5. Top Spending Category & Concentration
        val spendByCategory = thisMonthExpenses
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, txs) -> txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }

        val topCategoryEntry = spendByCategory.maxByOrNull { it.value }
        if (topCategoryEntry != null) {
            val cat = categoryMap[topCategoryEntry.key]
            val catName = cat?.name ?: "General"
            val catIcon = cat?.icon ?: "💳"
            val catPct = if (totalMonthExpense > BigDecimal.ZERO) {
                topCategoryEntry.value.divide(totalMonthExpense, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
            } else 0

            insights.add(
                SmartInsight(
                    id = "top_category",
                    title = "$catIcon $catName is Your Top Expense",
                    description = "Accounts for $catPct% (₹${topCategoryEntry.value.setScale(0, RoundingMode.HALF_UP).toPlainString()}) of your total monthly expenditures.",
                    icon = catIcon,
                    type = if (catPct >= 40) InsightType.WARNING else InsightType.TIP,
                    metric = "$catPct% of total",
                    drillDownCategoryId = topCategoryEntry.key
                )
            )
        }

        // 6. Top Merchant Drain & Frequency
        val merchantGroups = thisMonthExpenses
            .filter { it.merchant.isNotBlank() }
            .groupBy { it.merchant.trim().uppercase() }
            .mapValues { (_, txs) ->
                Pair(txs.size, txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) })
            }

        val topMerchantByAmount = merchantGroups.maxByOrNull { it.value.second }
        if (topMerchantByAmount != null && topMerchantByAmount.value.second > BigDecimal("500")) {
            val (count, total) = topMerchantByAmount.value
            insights.add(
                SmartInsight(
                    id = "top_merchant",
                    title = "Top Merchant: ${topMerchantByAmount.key}",
                    description = "₹${total.setScale(0, RoundingMode.HALF_UP).toPlainString()} spent across $count transaction${if (count > 1) "s" else ""} this month.",
                    icon = "🛍️",
                    type = InsightType.NEUTRAL,
                    metric = "₹${total.setScale(0, RoundingMode.HALF_UP).toPlainString()}",
                    drillDownMerchant = topMerchantByAmount.key
                )
            )
        }

        return insights
    }
}
