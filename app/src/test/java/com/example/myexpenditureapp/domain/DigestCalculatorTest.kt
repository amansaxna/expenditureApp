package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Subscription
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.insights.DigestCalculator
import com.example.myexpenditureapp.domain.insights.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

class DigestCalculatorTest {

    @Test
    fun testSafeDailySpendCalculatedAccurately() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 16, 12, 0, 0) // October has 31 days. Day 16 -> 16 days remaining (31 - 16 + 1 = 16)
        val fixedTimestamp = cal.timeInMillis

        val budgets = listOf(
            Budget(id = 1L, categoryId = 1L, limitAmount = BigDecimal("32000"), period = "Monthly", month = 10, year = 2026)
        )

        // User spent 16,000 so far
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                amount = BigDecimal("16000"),
                merchant = "Groceries",
                type = "Expense",
                timestamp = fixedTimestamp
            )
        )

        val digest = DigestCalculator.calculateDigest(
            transactions = transactions,
            categories = listOf(Category(id = 1L, name = "Food")),
            budgets = budgets,
            subscriptions = emptyList(),
            savingGoals = emptyList(),
            nowTimestamp = fixedTimestamp
        )

        // Remaining = 32000 - 16000 = 16000
        // Days remaining = 31 - 16 + 1 = 16
        // Safe daily spend = 16000 / 16 = 1000
        assertEquals(BigDecimal("1000"), digest.safeDailySpend)
        assertEquals(16, digest.daysRemainingInMonth)
        assertEquals(31, digest.totalDaysInMonth)
        assertEquals(16, digest.currentDayOfMonth)
    }

    @Test
    fun testPacingDeltaDetectsSpendingSlowerThanTimeline() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0) // September has 30 days. Day 15 -> exactly 50% time elapsed
        val fixedTimestamp = cal.timeInMillis

        val budgets = listOf(
            Budget(id = 1L, categoryId = 1L, limitAmount = BigDecimal("30000"), period = "Monthly", month = 9, year = 2026)
        )

        // User spent 12,000 (40% of budget) while 50% of month has passed
        val transactions = listOf(
            Transaction(
                id = 1L,
                accountId = 1L,
                categoryId = 1L,
                amount = BigDecimal("12000"),
                merchant = "General",
                type = "Expense",
                timestamp = fixedTimestamp
            )
        )

        val digest = DigestCalculator.calculateDigest(
            transactions = transactions,
            categories = listOf(Category(id = 1L, name = "General")),
            budgets = budgets,
            subscriptions = emptyList(),
            savingGoals = emptyList(),
            nowTimestamp = fixedTimestamp
        )

        assertEquals(40, digest.budgetConsumedPercent)
        assertEquals(50, digest.expectedTimeElapsedPercent)
        assertTrue(digest.isSpendingSlower)
        assertEquals(HealthStatus.OPTIMAL, digest.healthStatus)
    }

    @Test
    fun testUpcomingSubscriptionsAggregatedWithinNext7Days() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 10, 12, 0, 0) // Day 10
        val fixedTimestamp = cal.timeInMillis

        val subscriptions = listOf(
            Subscription(id = 1L, name = "Netflix", amount = BigDecimal("649"), dueDayOfMonth = 14, isActive = true), // In 4 days (within 7)
            Subscription(id = 2L, name = "Spotify", amount = BigDecimal("119"), dueDayOfMonth = 16, isActive = true), // In 6 days (within 7)
            Subscription(id = 3L, name = "Gym", amount = BigDecimal("2500"), dueDayOfMonth = 25, isActive = true)     // In 15 days (outside 7)
        )

        val digest = DigestCalculator.calculateDigest(
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            subscriptions = subscriptions,
            savingGoals = emptyList(),
            nowTimestamp = fixedTimestamp
        )

        assertEquals(2, digest.upcomingSubscriptionsCount)
        assertEquals(BigDecimal("768"), digest.upcomingSubscriptionsAmount)
    }
}
