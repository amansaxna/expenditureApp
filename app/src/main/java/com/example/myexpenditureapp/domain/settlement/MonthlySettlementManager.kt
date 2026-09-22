package com.example.myexpenditureapp.domain.settlement

import android.content.Context
import android.content.SharedPreferences
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.SavingGoal
import com.example.myexpenditureapp.data.entity.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormatSymbols
import java.util.Calendar

data class MonthClosureSummary(
    val month: Int, // 1 - 12
    val year: Int,
    val monthName: String,
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val netSurplus: BigDecimal,
    val savingsRatePercent: Int,
    val budgetTotal: BigDecimal,
    val budgetSpent: BigDecimal,
    val budgetSurplus: BigDecimal,
    val topExpenseCategoryName: String?,
    val topExpenseCategoryIcon: String?,
    val transactionCount: Int,
    val isSurplus: Boolean
)

object MonthlySettlementManager {
    private const val PREFS_NAME = "monthly_settlement_prefs"
    private const val KEY_SETTLED_PREFIX = "settled_"
    private const val KEY_DISMISSED_PREFIX = "dismissed_"
    private const val KEY_ROLLOVER_BUDGET_PREFIX = "rollover_budget_"

    private fun getPrefs(context: Context = Graph.appContext): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isMonthSettled(month: Int, year: Int, context: Context = Graph.appContext): Boolean {
        return getPrefs(context).getBoolean("${KEY_SETTLED_PREFIX}${year}_$month", false)
    }

    fun isMonthDismissed(month: Int, year: Int, context: Context = Graph.appContext): Boolean {
        return getPrefs(context).getBoolean("${KEY_DISMISSED_PREFIX}${year}_$month", false)
    }

    fun markMonthDismissed(month: Int, year: Int, context: Context = Graph.appContext) {
        getPrefs(context).edit().putBoolean("${KEY_DISMISSED_PREFIX}${year}_$month", true).apply()
    }

    fun markMonthSettled(
        month: Int,
        year: Int,
        settlementType: String,
        amount: BigDecimal,
        context: Context = Graph.appContext
    ) {
        getPrefs(context).edit()
            .putBoolean("${KEY_SETTLED_PREFIX}${year}_$month", true)
            .putString("settlement_type_${year}_$month", settlementType)
            .putString("settlement_amount_${year}_$month", amount.toPlainString())
            .putLong("settlement_timestamp_${year}_$month", System.currentTimeMillis())
            .apply()
    }

    fun getRolloverBudget(month: Int, year: Int, context: Context = Graph.appContext): BigDecimal {
        val str = getPrefs(context).getString("${KEY_ROLLOVER_BUDGET_PREFIX}${year}_$month", "0") ?: "0"
        return str.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    fun addRolloverBudget(month: Int, year: Int, amount: BigDecimal, context: Context = Graph.appContext) {
        val current = getRolloverBudget(month, year, context)
        val updated = current.add(amount)
        getPrefs(context).edit()
            .putString("${KEY_ROLLOVER_BUDGET_PREFIX}${year}_$month", updated.toPlainString())
            .apply()
    }

    fun calculateMonthSummary(
        month: Int,
        year: Int,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>
    ): MonthClosureSummary {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMillis = cal.timeInMillis

        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMillis = cal.timeInMillis

        val monthTxs = transactions.filter { it.timestamp in startMillis..endMillis }

        var totalIncome = BigDecimal.ZERO
        var totalExpense = BigDecimal.ZERO
        val categoryExpenseMap = mutableMapOf<Long, BigDecimal>()

        monthTxs.forEach { tx ->
            when (tx.type.lowercase()) {
                "income" -> totalIncome = totalIncome.add(tx.amount)
                "expense" -> {
                    totalExpense = totalExpense.add(tx.amount)
                    tx.categoryId?.let { catId ->
                        categoryExpenseMap[catId] =
                            (categoryExpenseMap[catId] ?: BigDecimal.ZERO).add(tx.amount)
                    }
                }
            }
        }

        val netSurplus = totalIncome.subtract(totalExpense)
        val isSurplus = netSurplus > BigDecimal.ZERO

        val savingsRatePercent = if (totalIncome > BigDecimal.ZERO && isSurplus) {
            netSurplus.multiply(BigDecimal(100))
                .divide(totalIncome, 0, RoundingMode.HALF_UP)
                .toInt().coerceIn(0, 100)
        } else {
            0
        }

        val monthBudgets = budgets.filter { it.month == month && it.year == year }
        var budgetTotal = BigDecimal.ZERO
        monthBudgets.forEach { budgetTotal = budgetTotal.add(it.limitAmount) }

        val budgetSurplus = if (budgetTotal > BigDecimal.ZERO) {
            budgetTotal.subtract(totalExpense).max(BigDecimal.ZERO)
        } else BigDecimal.ZERO

        // Find top category
        val topCategoryEntry = categoryExpenseMap.maxByOrNull { it.value }
        val topCategory = topCategoryEntry?.let { entry -> categories.find { it.id == entry.key } }

        val monthNames = DateFormatSymbols().months
        val monthName = "${monthNames.getOrElse(month - 1) { "Month $month" }} $year"

        return MonthClosureSummary(
            month = month,
            year = year,
            monthName = monthName,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSurplus = netSurplus,
            savingsRatePercent = savingsRatePercent,
            budgetTotal = budgetTotal,
            budgetSpent = totalExpense,
            budgetSurplus = budgetSurplus,
            topExpenseCategoryName = topCategory?.name,
            topExpenseCategoryIcon = topCategory?.icon,
            transactionCount = monthTxs.size,
            isSurplus = isSurplus
        )
    }

    /**
     * Finds the previous month (or earliest unsettled recent month).
     */
    fun getPreviousMonthAndYear(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        return Pair(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    fun getCurrentMonthAndYear(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return Pair(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    /**
     * Executes the sweep into a SavingGoal
     */
    suspend fun sweepToSavingGoal(
        goalId: Long,
        amount: BigDecimal,
        month: Int,
        year: Int,
        context: Context = Graph.appContext
    ) {
        if (amount > BigDecimal.ZERO) {
            Graph.savingGoalRepository.depositFunds(goalId, amount)
        }
        markMonthSettled(month, year, "GOAL_SWEEP_$goalId", amount, context)
    }

    /**
     * Executes the budget rollover to the current/next month
     */
    fun rolloverToCurrentMonthBudget(
        amount: BigDecimal,
        month: Int,
        year: Int,
        context: Context = Graph.appContext
    ) {
        val (currMonth, currYear) = getCurrentMonthAndYear()
        if (amount > BigDecimal.ZERO) {
            addRolloverBudget(currMonth, currYear, amount, context)
        }
        markMonthSettled(month, year, "BUDGET_ROLLOVER", amount, context)
    }

    /**
     * Clean slate settlement (retain in checking/savings without changing budgets)
     */
    fun settleCleanSlate(
        amount: BigDecimal,
        month: Int,
        year: Int,
        context: Context = Graph.appContext
    ) {
        markMonthSettled(month, year, "CLEAN_SLATE", amount, context)
    }
}
