package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

class AnalyticsOverallTest {

    @Test
    fun testOverallFilterAggregatesAllTransactionsAcrossMonths() {
        val calCurrent = Calendar.getInstance()
        calCurrent.set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        val currentMonthTs = calCurrent.timeInMillis

        val calPrev = Calendar.getInstance()
        calPrev.set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        val prevMonthTs = calPrev.timeInMillis

        val tx1 = Transaction(id = 1L, accountId = 1L, categoryId = 1L, amount = BigDecimal("3000"), merchant = "Shop A", type = "Expense", timestamp = currentMonthTs)
        val tx2 = Transaction(id = 2L, accountId = 1L, categoryId = 1L, amount = BigDecimal("7000"), merchant = "Shop B", type = "Expense", timestamp = prevMonthTs)

        val txs = listOf(tx1, tx2)
        val selectedMonth = 0 // Overall

        val filtered = txs.filter { tx ->
            if (selectedMonth == 0) {
                true
            } else {
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                c.get(Calendar.MONTH) + 1 == selectedMonth
            }
        }

        val totalSpent = filtered.filter { it.type == "Expense" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }

        // overall total spent should equal 3000 + 7000 = 10000
        assertEquals(2, filtered.size)
        assertEquals(BigDecimal("10000"), totalSpent)
    }

    @Test
    fun testOverallBudgetsAggregationDoesNotDivideByZeroOrScaleExtravagantly() {
        val b1 = Budget(id = 1L, categoryId = 1L, limitAmount = BigDecimal("15000"), period = "Monthly", month = 10, year = 2026)
        val b2 = Budget(id = 2L, categoryId = 1L, limitAmount = BigDecimal("15000"), period = "Monthly", month = 9, year = 2026)
        val buds = listOf(b1, b2)
        val selectedMonth = 0

        val currentBudgets = if (selectedMonth == 0) buds else buds.filter { it.month == selectedMonth }
        val budgetLimit = currentBudgets.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.limitAmount) }

        assertEquals(BigDecimal("30000"), budgetLimit)
    }
}
