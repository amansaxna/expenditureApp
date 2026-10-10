package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.insights.InsightsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

class YearlyCalculatorsTest {

    @Test
    fun testYearlyAggregationCalculatesAccurateTotals() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 15, 12, 0, 0)
        val janTs = cal.timeInMillis

        cal.set(2026, Calendar.JUNE, 20, 12, 0, 0)
        val junTs = cal.timeInMillis

        cal.set(2025, Calendar.DECEMBER, 31, 12, 0, 0)
        val prevYearTs = cal.timeInMillis

        val txJan = Transaction(id = 1L, accountId = 1L, categoryId = 1L, amount = BigDecimal("20000"), merchant = "Rent", type = "Expense", timestamp = janTs)
        val txJun = Transaction(id = 2L, accountId = 1L, categoryId = 2L, amount = BigDecimal("10000"), merchant = "Travel", type = "Expense", timestamp = junTs)
        val txIncome = Transaction(id = 3L, accountId = 1L, categoryId = 3L, amount = BigDecimal("60000"), merchant = "Bonus", type = "Income", timestamp = junTs)
        val tx2025 = Transaction(id = 4L, accountId = 1L, categoryId = 1L, amount = BigDecimal("50000"), merchant = "Old Car", type = "Expense", timestamp = prevYearTs)

        val allTxs = listOf(txJan, txJun, txIncome, tx2025)

        val year2026Txs = allTxs.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.YEAR) == 2026
        }

        val totalExpense = year2026Txs.filter { it.type == "Expense" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val totalIncome = year2026Txs.filter { it.type == "Income" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val netSavings = totalIncome.subtract(totalExpense)

        // 2026 expense = 20,000 + 10,000 = 30,000 (2025's 50,000 is excluded)
        assertEquals(BigDecimal("30000"), totalExpense)
        assertEquals(BigDecimal("60000"), totalIncome)
        assertEquals(BigDecimal("30000"), netSavings)

        val savingsRate = netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
        assertEquals(50, savingsRate)
    }

    @Test
    fun testOnlyAvailableMonthsWithDataAreExtracted() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MARCH, 10, 12, 0, 0)
        val marTs = cal.timeInMillis

        cal.set(2026, Calendar.OCTOBER, 5, 12, 0, 0)
        val octTs = cal.timeInMillis

        val tx1 = Transaction(id = 1L, accountId = 1L, categoryId = 1L, amount = BigDecimal("1000"), merchant = "M1", type = "Expense", timestamp = marTs)
        val tx2 = Transaction(id = 2L, accountId = 1L, categoryId = 1L, amount = BigDecimal("2000"), merchant = "M2", type = "Expense", timestamp = octTs)

        val txs = listOf(tx1, tx2)
        val targetYear = 2026

        val monthsWithData = txs.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.YEAR) == targetYear
        }.map {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) + 1
        }.toSet()

        // Only March (3) and October (10) should have data
        assertEquals(setOf(3, 10), monthsWithData)
        assertTrue(1 !in monthsWithData) // January has no data
        assertTrue(2 !in monthsWithData) // February has no data
    }

    @Test
    fun testYearlyInsightGeneratedWhenYearDataExists() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.APRIL, 15, 12, 0, 0)
        val ts = cal.timeInMillis

        val txs = listOf(
            Transaction(id = 1L, accountId = 1L, categoryId = 1L, amount = BigDecimal("80000"), merchant = "Flights", type = "Expense", timestamp = ts)
        )
        val categories = listOf(Category(id = 1L, name = "Travel", icon = "✈️"))

        val yearlyInsights = InsightsEngine.generateYearlyInsights(
            transactions = txs,
            categories = categories,
            targetYear = 2026
        )

        assertTrue(yearlyInsights.isNotEmpty())
        assertTrue(yearlyInsights.any { it.title.contains("Travel") || it.title.contains("Annual") || it.title.contains("2026") })
    }

    @Test
    fun testYearlyInsightEmptyWhenNoDataForYear() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.APRIL, 15, 12, 0, 0)
        val ts = cal.timeInMillis

        val txs = listOf(
            Transaction(id = 1L, accountId = 1L, categoryId = 1L, amount = BigDecimal("80000"), merchant = "Flights", type = "Expense", timestamp = ts)
        )

        // For year 2024, there is no data
        val yearlyInsights = InsightsEngine.generateYearlyInsights(
            transactions = txs,
            categories = emptyList(),
            targetYear = 2024
        )

        assertTrue(yearlyInsights.isEmpty())
    }
}
