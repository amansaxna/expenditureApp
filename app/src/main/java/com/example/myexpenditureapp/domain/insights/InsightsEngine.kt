package com.example.myexpenditureapp.domain.insights

import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.utils.formatIndian
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
        budgets: List<Budget>,
        accounts: List<Account> = emptyList()
    ): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()
        val categoryMap = categories.associateBy { it.id }

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        if (transactions.isEmpty()) return emptyList()

        // Filter this month's transactions
        val thisMonthTxs = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.MONTH) + 1 == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }

        if (thisMonthTxs.isEmpty()) return emptyList()

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
                        title = "Strong Savings Rate ($savingsRate%)",
                        description = "You are saving $savingsRate% of your total income (${netSavings.formatIndian()}) this month.",
                        icon = "savings",
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
                        description = "Expenditures exceed your total logged income by ${deficit.formatIndian()} this month.",
                        icon = "alert",
                        type = InsightType.WARNING,
                        metric = "-${deficit.formatIndian()}",
                        drillDownType = "Expense"
                    )
                )
            }
        }

        // 2. Financial Runway & Survival Buffer
        val totalLiquidNet = accounts.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.balance) }
        if (totalLiquidNet > BigDecimal.ZERO && totalMonthExpense > BigDecimal.ZERO && dayOfMonth > 0) {
            val dailyBurn = totalMonthExpense.divide(BigDecimal(dayOfMonth), 2, RoundingMode.HALF_UP)
            val monthlyBurnEstimate = dailyBurn.multiply(BigDecimal(30)).coerceAtLeast(BigDecimal("1000"))
            val runwayMonths = totalLiquidNet.divide(monthlyBurnEstimate, 1, RoundingMode.HALF_UP).toDouble()

            insights.add(
                SmartInsight(
                    id = "runway_buffer",
                    title = "Financial Runway: ${runwayMonths} Mo",
                    description = "Your total liquid reserve of ${totalLiquidNet.formatIndian()} provides ~${runwayMonths} months of survival runway at current burn rate.",
                    icon = "runway",
                    type = if (runwayMonths >= 3.0) InsightType.POSITIVE else InsightType.WARNING,
                    metric = "${runwayMonths} Mo Cover"
                )
            )
        }

        // 3. No-Spend Days & SpendZen Score
        if (dayOfMonth >= 3) {
            val spentDays = thisMonthExpenses.map { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                c.get(Calendar.DAY_OF_MONTH)
            }.toSet()
            val noSpendDaysCount = (1..dayOfMonth).count { it !in spentDays }
            val noSpendRatio = noSpendDaysCount.toFloat() / dayOfMonth.toFloat()
            val score = ((noSpendRatio * 30) + (if (totalBudget > BigDecimal.ZERO && totalMonthExpense <= totalBudget) 40 else 20) + 30).toInt().coerceIn(10, 99)

            insights.add(
                SmartInsight(
                    id = "zen_score",
                    title = "Financial Zen Score: $score/100",
                    description = "Achieved $noSpendDaysCount zero-spend days out of $dayOfMonth days elapsed this month. High impulse control.",
                    icon = "zen",
                    type = InsightType.POSITIVE,
                    metric = "$noSpendDaysCount No-Spend Days"
                )
            )
        }

        // 4. Time of Day / Late-Night Spending Window
        if (thisMonthExpenses.size >= 4) {
            var lateNightSpend = BigDecimal.ZERO
            thisMonthExpenses.forEach { tx ->
                val hour = Calendar.getInstance().apply { timeInMillis = tx.timestamp }.get(Calendar.HOUR_OF_DAY)
                if (hour >= 23 || hour < 6) {
                    lateNightSpend = lateNightSpend.add(tx.amount)
                }
            }
            if (lateNightSpend > BigDecimal.ZERO && totalMonthExpense > BigDecimal.ZERO) {
                val lateNightPct = (lateNightSpend.divide(totalMonthExpense, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100))).toInt()
                if (lateNightPct >= 15) {
                    insights.add(
                        SmartInsight(
                            id = "impulse_window",
                            title = "Late-Night Spending: $lateNightPct%",
                            description = "${lateNightSpend.formatIndian()} spent between 11 PM and 6 AM. Consider reviewing late-night impulse orders.",
                            icon = "time",
                            type = InsightType.TIP,
                            metric = "$lateNightPct% Late Night",
                            drillDownType = "Expense"
                        )
                    )
                }
            }
        }

        // 5. Budget Pacing / Safety Score Insight
        if (totalBudget > BigDecimal.ZERO && totalMonthExpense > BigDecimal.ZERO) {
            val expectedPace = BigDecimal(dayOfMonth).divide(BigDecimal(daysInMonth), 4, RoundingMode.HALF_UP)
            val expectedSpendAtThisDay = totalBudget.multiply(expectedPace)

            if (totalMonthExpense > totalBudget) {
                val overspend = totalMonthExpense.subtract(totalBudget)
                insights.add(
                    SmartInsight(
                        id = "budget_exceeded",
                        title = "Monthly Budget Exceeded",
                        description = "You have exceeded your total monthly budget limit by ${overspend.formatIndian()}.",
                        icon = "alert",
                        type = InsightType.WARNING,
                        metric = "${totalMonthExpense.formatIndian()} / ${totalBudget.formatIndian()}",
                        drillDownType = "Expense"
                    )
                )
            } else if (totalMonthExpense < expectedSpendAtThisDay) {
                val savings = expectedSpendAtThisDay.subtract(totalMonthExpense)
                insights.add(
                    SmartInsight(
                        id = "budget_pacing_good",
                        title = "Under Budget Pacing",
                        description = "You are spending ~${savings.formatIndian()} below expected pace for day $dayOfMonth of $daysInMonth.",
                        icon = "budget",
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
                        description = "Your current spending pace is running ahead of the midpoint for this month.",
                        icon = "velocity",
                        type = InsightType.WARNING,
                        metric = "${(totalMonthExpense.divide(totalBudget, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100))).toInt()}% spent",
                        drillDownType = "Expense"
                    )
                )
            }
        }

        // 6. Weekend vs Weekday Lifestyle Behavior
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
            if (weekendPct >= 40) {
                insights.add(
                    SmartInsight(
                        id = "weekend_heavy",
                        title = "Weekend Lifestyle Outflow ($weekendPct%)",
                        description = "Over $weekendPct% of your monthly spend (${weekendSpend.formatIndian()}) occurs on Saturdays and Sundays.",
                        icon = "weekend",
                        type = InsightType.TIP,
                        metric = "$weekendPct% on Weekends",
                        drillDownType = "Expense"
                    )
                )
            }
        }

        // 7. Daily Burn Rate & Projected Total
        if (dayOfMonth > 0 && totalMonthExpense > BigDecimal.ZERO) {
            val dailyBurnRate = totalMonthExpense.divide(BigDecimal(dayOfMonth), 2, RoundingMode.HALF_UP)
            val projectedMonthEnd = dailyBurnRate.multiply(BigDecimal(daysInMonth)).setScale(0, RoundingMode.HALF_UP)
            insights.add(
                SmartInsight(
                    id = "daily_burn",
                    title = "Daily Burn: ${dailyBurnRate.formatIndian()}/day",
                    description = "At this current trajectory, projected month-end total spend will be approx ${projectedMonthEnd.formatIndian()}.",
                    icon = "burn",
                    type = InsightType.NEUTRAL,
                    metric = "${dailyBurnRate.formatIndian()}/day",
                    drillDownType = "Expense"
                )
            )
        }

        // 8. Top Spending Category & Concentration
        val spendByCategory = thisMonthExpenses
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, txs) -> txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }

        val topCategoryEntry = spendByCategory.maxByOrNull { it.value }
        if (topCategoryEntry != null) {
            val cat = categoryMap[topCategoryEntry.key]
            val catName = cat?.name ?: "General"
            val catPct = if (totalMonthExpense > BigDecimal.ZERO) {
                topCategoryEntry.value.divide(totalMonthExpense, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
            } else 0

            insights.add(
                SmartInsight(
                    id = "top_category",
                    title = "$catName is Your Top Category",
                    description = "Accounts for $catPct% (${topCategoryEntry.value.formatIndian()}) of your total monthly expenditures.",
                    icon = "category",
                    type = if (catPct >= 40) InsightType.WARNING else InsightType.TIP,
                    metric = "$catPct% of total",
                    drillDownCategoryId = topCategoryEntry.key
                )
            )
        }

        // 9. Top Merchant Drain & Frequency
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
                    description = "${total.formatIndian()} spent across $count transaction${if (count > 1) "s" else ""} this month.",
                    icon = "merchant",
                    type = InsightType.NEUTRAL,
                    metric = total.formatIndian(),
                    drillDownMerchant = topMerchantByAmount.key
                )
            )
        }

        return insights
    }
}
