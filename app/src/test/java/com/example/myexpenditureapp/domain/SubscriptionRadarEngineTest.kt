package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.data.entity.Subscription
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.radar.RadarStatus
import com.example.myexpenditureapp.domain.radar.SubscriptionRadarEngine
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

class SubscriptionRadarEngineTest {

    @Test
    fun testRecurringSubscriptionDetection_identifiesMonthlyTransactions() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 15, 12, 0, 0)
        val janTx = Transaction(
            accountId = 1,
            categoryId = 5,
            amount = BigDecimal("649.00"),
            merchant = "Netflix",
            timestamp = cal.timeInMillis,
            type = "Expense"
        )

        cal.set(2026, Calendar.FEBRUARY, 15, 12, 0, 0)
        val febTx = Transaction(
            accountId = 1,
            categoryId = 5,
            amount = BigDecimal("649.00"),
            merchant = "Netflix India",
            timestamp = cal.timeInMillis,
            type = "Expense"
        )

        val transactions = listOf(janTx, febTx)
        val existingSubscriptions = emptyList<Subscription>()

        val suggestions = SubscriptionRadarEngine.detectRecurringSubscriptions(transactions, existingSubscriptions)

        assertEquals(1, suggestions.size)
        assertTrue(suggestions[0].merchantName.contains("Netflix", ignoreCase = true))
        assertEquals(BigDecimal("649.00"), suggestions[0].estimatedAmount)
        assertEquals(15, suggestions[0].detectedDueDay)
    }

    @Test
    fun testComputeRadar_dueStatusAndForecastMetrics() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 10)
        val now = cal.timeInMillis

        // Sub 1: Due on 12th (2 days away -> DUE_SOON, in 7-day upcoming)
        val subSoon = Subscription(
            id = 1,
            name = "Spotify",
            amount = BigDecimal("199.00"),
            billingCycle = "Monthly",
            dueDayOfMonth = 12,
            isActive = true
        )

        // Sub 2: Due on 2nd (8 days ago -> OVERDUE, not paid)
        val subOverdue = Subscription(
            id = 2,
            name = "Wifi Broadband",
            amount = BigDecimal("999.00"),
            billingCycle = "Monthly",
            dueDayOfMonth = 2,
            isActive = true
        )

        // Sub 3: Due on 5th, but paid today -> PAID_THIS_CYCLE
        val subPaid = Subscription(
            id = 3,
            name = "House Rent",
            amount = BigDecimal("15000.00"),
            billingCycle = "Monthly",
            dueDayOfMonth = 5,
            lastPaidDate = now,
            isActive = true
        )

        val summary = SubscriptionRadarEngine.computeRadar(
            subscriptions = listOf(subSoon, subOverdue, subPaid),
            transactions = emptyList(),
            now = now
        )

        assertEquals(BigDecimal("16198.00"), summary.totalMonthlyCommitment)
        assertEquals(BigDecimal("199.00"), summary.upcomingNext7Days)
        assertEquals(BigDecimal("999.00"), summary.overdueAmount)
        assertEquals(BigDecimal("15000.00"), summary.paidThisMonth)

        val soonItem = summary.billItems.find { it.subscription.id == 1L }
        assertNotNull(soonItem)
        assertEquals(RadarStatus.DUE_SOON, soonItem?.status)

        val overdueItem = summary.billItems.find { it.subscription.id == 2L }
        assertNotNull(overdueItem)
        assertEquals(RadarStatus.OVERDUE, overdueItem?.status)

        val paidItem = summary.billItems.find { it.subscription.id == 3L }
        assertNotNull(paidItem)
        assertEquals(RadarStatus.PAID_THIS_CYCLE, paidItem?.status)
    }

    @Test
    fun testComputeRadar_quarterlyAndYearlyCommitmentConversion() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val now = cal.timeInMillis

        val quarterlySub = Subscription(
            id = 1,
            name = "Gym Membership",
            amount = BigDecimal("3000.00"),
            billingCycle = "Quarterly",
            dueDayOfMonth = 15,
            isActive = true
        )

        val yearlySub = Subscription(
            id = 2,
            name = "Amazon Prime Annual",
            amount = BigDecimal("1499.00"),
            billingCycle = "Yearly",
            dueDayOfMonth = 20,
            isActive = true
        )

        val summary = SubscriptionRadarEngine.computeRadar(
            subscriptions = listOf(quarterlySub, yearlySub),
            transactions = emptyList(),
            now = now
        )

        // 3000 / 3 = 1000, 1499 / 12 = 124.92 -> Total = 1124.92
        assertEquals(BigDecimal("1124.92"), summary.totalMonthlyCommitment)
    }

    @Test
    fun testCalculateBillItem_dueTodayTriggersDueTodayStatus() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 15)
        val now = cal.timeInMillis

        val subDueToday = Subscription(
            id = 5,
            name = "Netflix Standard",
            amount = BigDecimal("499.00"),
            billingCycle = "Monthly",
            dueDayOfMonth = 15,
            isActive = true
        )

        val item = SubscriptionRadarEngine.calculateBillItem(subDueToday, now)
        assertEquals(RadarStatus.DUE_TODAY, item.status)
        assertEquals(0, item.daysRemaining)
    }
}
