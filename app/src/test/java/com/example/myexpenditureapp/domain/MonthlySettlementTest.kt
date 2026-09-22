package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.settlement.MonthlySettlementManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

class MonthlySettlementTest {

    @Test
    fun `calculateMonthSummary calculates surplus and savings rate accurately`() {
        val targetMonth = 8
        val targetYear = 2026

        val cal = Calendar.getInstance()
        cal.set(targetYear, targetMonth - 1, 15, 12, 0, 0)
        val midMonthTimestamp = cal.timeInMillis

        val categories = listOf(
            Category(id = 1L, name = "Dining", icon = "🍔"),
            Category(id = 2L, name = "Groceries", icon = "🛒"),
            Category(id = 3L, name = "Salary", icon = "💰")
        )

        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 3L,
                amount = BigDecimal("100000"),
                merchant = "Company Salary",
                type = "Income",
                timestamp = midMonthTimestamp
            ),
            Transaction(
                id = 2L,
                accountId = 1L,
                categoryId = 1L,
                amount = BigDecimal("15000"),
                merchant = "Restaurant",
                type = "Expense",
                timestamp = midMonthTimestamp
            ),
            Transaction(
                id = 3L,
                accountId = 1L,
                categoryId = 2L,
                amount = BigDecimal("25000"),
                merchant = "Supermarket",
                type = "Expense",
                timestamp = midMonthTimestamp
            )
        )

        val budgets = listOf(
            Budget(id = 1L, categoryId = 1L, limitAmount = BigDecimal("20000"), period = "Monthly", month = targetMonth, year = targetYear),
            Budget(id = 2L, categoryId = 2L, limitAmount = BigDecimal("30000"), period = "Monthly", month = targetMonth, year = targetYear)
        )

        val summary = MonthlySettlementManager.calculateMonthSummary(
            month = targetMonth,
            year = targetYear,
            transactions = transactions,
            categories = categories,
            budgets = budgets
        )

        assertEquals(BigDecimal("100000"), summary.totalIncome)
        assertEquals(BigDecimal("40000"), summary.totalExpense)
        assertEquals(BigDecimal("60000"), summary.netSurplus)
        assertTrue(summary.isSurplus)
        assertEquals(60, summary.savingsRatePercent) // 60,000 / 100,000 = 60%
        assertEquals(BigDecimal("50000"), summary.budgetTotal)
        assertEquals(BigDecimal("10000"), summary.budgetSurplus) // 50,000 - 40,000 = 10,000
        assertEquals("Groceries", summary.topExpenseCategoryName)
        assertEquals(3, summary.transactionCount)
    }

    @Test
    fun `calculateMonthSummary handles deficit cleanly`() {
        val targetMonth = 8
        val targetYear = 2026

        val cal = Calendar.getInstance()
        cal.set(targetYear, targetMonth - 1, 10, 10, 0, 0)
        val midMonthTimestamp = cal.timeInMillis

        val categories = listOf(
            Category(id = 1L, name = "Rent", icon = "🏠"),
            Category(id = 2L, name = "Freelance", icon = "💼")
        )

        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 2L,
                amount = BigDecimal("30000"),
                merchant = "Client Payment",
                type = "Income",
                timestamp = midMonthTimestamp
            ),
            Transaction(
                id = 2L,
                accountId = 1L,
                categoryId = 1L,
                amount = BigDecimal("45000"),
                merchant = "Landlord",
                type = "Expense",
                timestamp = midMonthTimestamp
            )
        )

        val summary = MonthlySettlementManager.calculateMonthSummary(
            month = targetMonth,
            year = targetYear,
            transactions = transactions,
            categories = categories,
            budgets = emptyList()
        )

        assertEquals(BigDecimal("30000"), summary.totalIncome)
        assertEquals(BigDecimal("45000"), summary.totalExpense)
        assertEquals(BigDecimal("-15000"), summary.netSurplus)
        assertFalse(summary.isSurplus)
        assertEquals(0, summary.savingsRatePercent)
    }
}
