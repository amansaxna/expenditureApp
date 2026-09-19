package com.example.myexpenditureapp.domain.radar

import com.example.myexpenditureapp.data.entity.Subscription
import com.example.myexpenditureapp.data.entity.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class RadarStatus {
    OVERDUE,
    DUE_TODAY,
    DUE_SOON, // 1 to 3 days
    UPCOMING, // > 3 days
    PAID_THIS_CYCLE
}

data class RadarBillItem(
    val subscription: Subscription,
    val nextDueDate: Long,
    val daysRemaining: Int,
    val status: RadarStatus,
    val isPaidThisCycle: Boolean
)

data class SubscriptionSuggestion(
    val merchantName: String,
    val estimatedAmount: BigDecimal,
    val detectedDueDay: Int,
    val occurrences: Int,
    val confidence: String, // "High", "Medium"
    val suggestedCategoryId: Long?,
    val suggestedAccountId: Long?
)

data class SubscriptionRadarSummary(
    val totalMonthlyCommitment: BigDecimal,
    val upcomingNext7Days: BigDecimal,
    val paidThisMonth: BigDecimal,
    val overdueAmount: BigDecimal,
    val billItems: List<RadarBillItem>,
    val detectedSuggestions: List<SubscriptionSuggestion>
)

object SubscriptionRadarEngine {

    /**
     * Calculates radar schedule, status, and summary metrics for active subscriptions.
     */
    fun computeRadar(
        subscriptions: List<Subscription>,
        transactions: List<Transaction>,
        now: Long = System.currentTimeMillis()
    ): SubscriptionRadarSummary {
        val activeSubscriptions = subscriptions.filter { it.isActive }
        
        val billItems = activeSubscriptions.map { sub ->
            calculateBillItem(sub, now)
        }.sortedWith(
            compareBy(
                {
                    when (it.status) {
                        RadarStatus.OVERDUE -> 0
                        RadarStatus.DUE_TODAY -> 1
                        RadarStatus.DUE_SOON -> 2
                        RadarStatus.UPCOMING -> 3
                        RadarStatus.PAID_THIS_CYCLE -> 4
                    }
                },
                { it.daysRemaining }
            )
        )

        var totalMonthlyCommitment = BigDecimal.ZERO
        var upcomingNext7Days = BigDecimal.ZERO
        var paidThisMonth = BigDecimal.ZERO
        var overdueAmount = BigDecimal.ZERO

        for (item in billItems) {
            val monthlyAmt = when (item.subscription.billingCycle) {
                "Quarterly" -> item.subscription.amount.divide(BigDecimal("3"), 2, RoundingMode.HALF_UP)
                "Yearly" -> item.subscription.amount.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP)
                "Weekly" -> item.subscription.amount.multiply(BigDecimal("4.33")).setScale(2, RoundingMode.HALF_UP)
                else -> item.subscription.amount
            }
            totalMonthlyCommitment = totalMonthlyCommitment.add(monthlyAmt)

            if (item.isPaidThisCycle) {
                paidThisMonth = paidThisMonth.add(item.subscription.amount)
            } else {
                if (item.status == RadarStatus.OVERDUE) {
                    overdueAmount = overdueAmount.add(item.subscription.amount)
                }
                if (item.daysRemaining in 0..7) {
                    upcomingNext7Days = upcomingNext7Days.add(item.subscription.amount)
                }
            }
        }

        val detectedSuggestions = detectRecurringSubscriptions(transactions, subscriptions)

        return SubscriptionRadarSummary(
            totalMonthlyCommitment = totalMonthlyCommitment.setScale(2, RoundingMode.HALF_UP),
            upcomingNext7Days = upcomingNext7Days.setScale(2, RoundingMode.HALF_UP),
            paidThisMonth = paidThisMonth.setScale(2, RoundingMode.HALF_UP),
            overdueAmount = overdueAmount.setScale(2, RoundingMode.HALF_UP),
            billItems = billItems,
            detectedSuggestions = detectedSuggestions
        )
    }

    /**
     * Calculates due date, days remaining, and status for a single subscription.
     */
    fun calculateBillItem(subscription: Subscription, now: Long): RadarBillItem {
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)

        // Determine if already paid in the current month
        val isPaidThisCycle = if (subscription.lastPaidDate != null) {
            val paidCal = Calendar.getInstance().apply { timeInMillis = subscription.lastPaidDate }
            paidCal.get(Calendar.YEAR) == currentYear && paidCal.get(Calendar.MONTH) == currentMonth
        } else {
            false
        }

        // Build target due date in current month
        val dueCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            val maxDays = getActualMaximum(Calendar.DAY_OF_MONTH)
            val clampedDay = subscription.dueDayOfMonth.coerceIn(1, maxDays)
            set(Calendar.DAY_OF_MONTH, clampedDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val daysDiff = if (isPaidThisCycle) {
            // Next cycle is next month
            val nextMonthCal = (dueCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            val diffMs = nextMonthCal.timeInMillis - now
            TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
        } else {
            val diffMs = dueCal.timeInMillis - now
            val rawDays = (dueCal.get(Calendar.DAY_OF_MONTH) - currentDay)
            rawDays
        }

        val status = when {
            isPaidThisCycle -> RadarStatus.PAID_THIS_CYCLE
            daysDiff < 0 -> RadarStatus.OVERDUE
            daysDiff == 0 -> RadarStatus.DUE_TODAY
            daysDiff in 1..3 -> RadarStatus.DUE_SOON
            else -> RadarStatus.UPCOMING
        }

        return RadarBillItem(
            subscription = subscription,
            nextDueDate = dueCal.timeInMillis,
            daysRemaining = daysDiff,
            status = status,
            isPaidThisCycle = isPaidThisCycle
        )
    }

    /**
     * Auto-detects recurring payments from transaction history.
     */
    fun detectRecurringSubscriptions(
        transactions: List<Transaction>,
        existingSubscriptions: List<Subscription>
    ): List<SubscriptionSuggestion> {
        val existingNames = existingSubscriptions.map { it.name.trim().lowercase() }.toSet()

        val expenseTx = transactions.filter { it.type == "Expense" && it.merchant.isNotBlank() }

        // Group by normalized merchant
        val groupedByMerchant = expenseTx.groupBy { normalizeMerchant(it.merchant) }

        val suggestions = mutableListOf<SubscriptionSuggestion>()

        for ((normalizedMerchant, txList) in groupedByMerchant) {
            if (normalizedMerchant.length < 3) continue
            if (existingNames.any { it.contains(normalizedMerchant) || normalizedMerchant.contains(it) }) continue

            if (txList.size >= 2) {
                // Sort chronologically
                val sorted = txList.sortedBy { it.timestamp }

                // Check average interval between consecutive transactions
                val intervalsDays = mutableListOf<Long>()
                for (i in 0 until sorted.size - 1) {
                    val diffDays = TimeUnit.MILLISECONDS.toDays(sorted[i + 1].timestamp - sorted[i].timestamp)
                    if (diffDays in 20..35) {
                        intervalsDays.add(diffDays)
                    }
                }

                // If at least one interval is ~monthly (20..35 days)
                if (intervalsDays.isNotEmpty() || sorted.size >= 3) {
                    val avgAmount = sorted.map { it.amount }
                        .fold(BigDecimal.ZERO) { acc, a -> acc.add(a) }
                        .divide(BigDecimal(sorted.size), 2, RoundingMode.HALF_UP)

                    // Find most frequent day of month
                    val cal = Calendar.getInstance()
                    val days = sorted.map {
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.DAY_OF_MONTH)
                    }
                    val detectedDay = days.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 1

                    val confidence = if (intervalsDays.size >= 2) "High" else "Medium"
                    val mostCommonCategory = sorted.mapNotNull { it.categoryId }.groupBy { it }.maxByOrNull { it.value.size }?.key
                    val mostCommonAccount = sorted.map { it.accountId }.groupBy { it }.maxByOrNull { it.value.size }?.key

                    suggestions.add(
                        SubscriptionSuggestion(
                            merchantName = capitalizeWords(sorted.last().merchant),
                            estimatedAmount = avgAmount,
                            detectedDueDay = detectedDay,
                            occurrences = sorted.size,
                            confidence = confidence,
                            suggestedCategoryId = mostCommonCategory,
                            suggestedAccountId = mostCommonAccount
                        )
                    )
                }
            }
        }

        return suggestions
    }

    private fun normalizeMerchant(merchant: String): String {
        val cleaned = merchant.trim().lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
        val tokens = cleaned.split(" ").filter { 
            it.length > 2 && it !in setOf("ltd", "pvt", "india", "corp", "inc", "com", "app", "pay", "upi", "bill", "sub") 
        }
        return tokens.firstOrNull() ?: cleaned
    }

    private fun capitalizeWords(str: String): String {
        return str.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
