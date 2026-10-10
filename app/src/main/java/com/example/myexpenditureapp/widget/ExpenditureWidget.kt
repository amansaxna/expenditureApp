package com.example.myexpenditureapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.myexpenditureapp.MainActivity
import com.example.myexpenditureapp.R
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.domain.insights.DigestCalculator
import com.example.myexpenditureapp.domain.insights.HealthStatus
import com.example.myexpenditureapp.domain.insights.MicroSpendAnalyzer
import com.example.myexpenditureapp.domain.insights.MicroSpendSummary
import com.example.myexpenditureapp.utils.formatIndian
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

private val QuickAddKey = ActionParameters.Key<String>("ACTION")

class ExpenditureWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.provide(context)
        
        val accounts = Graph.accountRepository.getAllAccounts().first()
        val transactions = Graph.transactionRepository.getAllTransactions().first()
        val budgets = Graph.budgetRepository.getAllBudgets().first()
        val subscriptions = Graph.subscriptionRepository.getAllSubscriptions().first()
        val savingGoals = Graph.savingGoalRepository.getAllGoals().first()
        val categories = Graph.categoryRepository.getAllCategories().first()

        val unreviewed = transactions.filter { !it.isReviewed }
        val totalBalance = accounts.fold(BigDecimal.ZERO) { acc, account -> acc.add(account.balance) }
        
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val currentMonthNum = calendar.get(Calendar.MONTH) + 1
        val currentYearNum = calendar.get(Calendar.YEAR)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(28)
        val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)

        // Month start
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        // Today start
        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        val todayStart = todayCal.timeInMillis
        
        val monthlyIncome = transactions.filter { it.type == "Income" && it.timestamp >= monthStart }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val monthlyExpense = transactions.filter { it.type == "Expense" && it.timestamp >= monthStart }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val netFlow = monthlyIncome.subtract(monthlyExpense)

        val todayExpense = transactions.filter { it.type == "Expense" && it.timestamp >= todayStart }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val dailyAverage = if (currentDay > 0) {
            monthlyExpense.divide(BigDecimal(currentDay), 0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val totalBudget = budgets
            .filter { it.month == currentMonthNum && it.year == currentYearNum }
            .fold(BigDecimal.ZERO) { acc, b -> acc.add(b.limitAmount) }

        val safeDailySpend = if (totalBudget > BigDecimal.ZERO) {
            totalBudget.subtract(monthlyExpense).divide(BigDecimal(daysRemaining), 0, RoundingMode.HALF_UP).coerceAtLeast(BigDecimal.ZERO)
        } else if (monthlyIncome > BigDecimal.ZERO) {
            monthlyIncome.subtract(monthlyExpense).divide(BigDecimal(daysRemaining), 0, RoundingMode.HALF_UP).coerceAtLeast(BigDecimal.ZERO)
        } else {
            BigDecimal.ZERO
        }

        val upcoming7DayBills = subscriptions.filter { sub ->
            sub.isActive && (sub.dueDayOfMonth in currentDay..(currentDay + 7))
        }
        val upcomingBillsCount = upcoming7DayBills.size
        val upcomingBillsTotal = upcoming7DayBills.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amount) }

        val digest = DigestCalculator.calculateDigest(
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            subscriptions = subscriptions,
            savingGoals = savingGoals,
            totalLiquidBalance = totalBalance
        )

        val microSummary = MicroSpendAnalyzer.analyze(
            transactions = transactions,
            categories = categories,
            safeDailyAllowance = safeDailySpend
        )

        val last7Days = (6 downTo 0).map { dayOffset ->
            val dCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -dayOffset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dStart = dCal.timeInMillis
            val dEnd = dStart + 86400000L
            val daySpend = transactions.filter { it.type == "Expense" && it.timestamp in dStart until dEnd }
                .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            val dayLabel = dCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())?.take(1) ?: "D"
            Pair(dayLabel, daySpend)
        }
        val max7DaySpend = last7Days.maxOfOrNull { it.second }?.coerceAtLeast(BigDecimal("500")) ?: BigDecimal("500")
        val normalized7Days = last7Days.map { (label, spend) ->
            val ratio = spend.divide(max7DaySpend, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0.12f, 1.0f)
            Pair(label, ratio)
        }

        provideContent {
            WidgetContent(
                totalBalance = totalBalance,
                monthlyExpense = monthlyExpense,
                netFlow = netFlow,
                todayExpense = todayExpense,
                dailyAverage = dailyAverage,
                safeDailySpend = safeDailySpend,
                pendingReviewCount = unreviewed.size,
                upcomingBillsCount = upcomingBillsCount,
                upcomingBillsTotal = upcomingBillsTotal,
                currentDay = currentDay,
                daysInMonth = daysInMonth,
                healthStatusLabel = digest.healthStatus.label.uppercase(),
                healthScore = digest.healthScore,
                isOptimal = digest.healthStatus == HealthStatus.OPTIMAL,
                expectedTimeElapsedPercent = digest.expectedTimeElapsedPercent,
                budgetConsumedPercent = digest.budgetConsumedPercent,
                isSpendingSlower = digest.isSpendingSlower,
                microSummary = microSummary,
                normalized7Days = normalized7Days,
                max7DaySpend = max7DaySpend
            )
        }
    }

    @Composable
    private fun WidgetContent(
        totalBalance: BigDecimal,
        monthlyExpense: BigDecimal,
        netFlow: BigDecimal,
        todayExpense: BigDecimal,
        dailyAverage: BigDecimal,
        safeDailySpend: BigDecimal,
        pendingReviewCount: Int,
        upcomingBillsCount: Int,
        upcomingBillsTotal: BigDecimal,
        currentDay: Int,
        daysInMonth: Int,
        healthStatusLabel: String,
        healthScore: Int,
        isOptimal: Boolean,
        expectedTimeElapsedPercent: Int,
        budgetConsumedPercent: Int,
        isSpendingSlower: Boolean,
        microSummary: MicroSpendSummary,
        normalized7Days: List<Pair<String, Float>>,
        max7DaySpend: BigDecimal
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.glance_glass_card_bg))
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header Bar: Title + Health Badge + Glassy Quick Add Button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title Branding & Health Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPENDITURE",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFFF8FAFC))
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    
                    // Health Status Glass Pill
                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(if (isOptimal) R.drawable.glance_glass_badge_green else R.drawable.glance_glass_badge_amber))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "● $healthStatusLabel • $healthScore",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(if (isOptimal) Color(0xFF34D399) else Color(0xFFFBBF24))
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // + Quick Add Glass Button
                Row(
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.glance_glass_button_bg))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(QuickAddKey to "quick_add")
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Quick Add",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Primary Hero: Balance & Net Flow Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = totalBalance.formatIndian(),
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                    Text(
                        text = "Net Liquid Balance",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color(0xFF94A3B8))
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(if (netFlow >= BigDecimal.ZERO) R.drawable.glance_glass_badge_green else R.drawable.glance_glass_badge_amber))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (netFlow >= BigDecimal.ZERO) "+" else ""}${netFlow.formatIndian()}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(if (netFlow >= BigDecimal.ZERO) Color(0xFF34D399) else Color(0xFFF87171))
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "Net Flow",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color(0xFF94A3B8))
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // 4-Metric Glass Bento Grid Container
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ImageProvider(R.drawable.glance_glass_inner_box))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today's Burn
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Today's Burn",
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
                    )
                    Spacer(modifier = GlanceModifier.height(1.dp))
                    Text(
                        text = todayExpense.formatIndian(),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White))
                    )
                }

                // Daily Avg
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Daily Avg",
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
                    )
                    Spacer(modifier = GlanceModifier.height(1.dp))
                    Text(
                        text = "${dailyAverage.formatIndian()}/d",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White))
                    )
                }

                // Safe Allowance
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Safe/d",
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
                    )
                    Spacer(modifier = GlanceModifier.height(1.dp))
                    Text(
                        text = "${safeDailySpend.formatIndian()}/d",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFF34D399)))
                    )
                }

                // Month Total
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Month Total",
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
                    )
                    Spacer(modifier = GlanceModifier.height(1.dp))
                    Text(
                        text = monthlyExpense.formatIndian(),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White))
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // 7-Day Outflow Trend & Pacing Card
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ImageProvider(R.drawable.glance_glass_inner_box))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7-DAY OUTFLOW TREND",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFFCBD5E1))
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    
                    if (pendingReviewCount > 0) {
                        Text(
                            text = "📬 $pendingReviewCount Pending",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFFBBF24))
                            )
                        )
                    } else if (upcomingBillsCount > 0) {
                        Text(
                            text = "📅 $upcomingBillsCount Bill (${upcomingBillsTotal.formatIndian()})",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF818CF8))
                            )
                        )
                    } else {
                        Text(
                            text = "Day $currentDay of $daysInMonth",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF818CF8))
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // 7-Day Micro Bars
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Bottom
                ) {
                    normalized7Days.forEachIndexed { index, pair ->
                        val label = pair.first
                        val ratio = pair.second
                        val isToday = index == normalized7Days.lastIndex
                        val barHeightDp = (ratio * 22).toInt().coerceIn(4, 22).dp

                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .width(8.dp)
                                    .height(barHeightDp)
                                    .background(
                                        if (isToday)
                                            ColorProvider(Color(0xFF818CF8))
                                        else
                                            ColorProvider(Color(0xFF475569))
                                    )
                            ) {}
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontSize = 8.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) ColorProvider(Color(0xFF818CF8)) else ColorProvider(Color(0xFF94A3B8))
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // NEW: Micro-Expenditure Leakage & Pacing Breakdown Block (Filling Lower Widget Canvas)
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ImageProvider(R.drawable.glance_glass_inner_box))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MICRO-LEAK RADAR (<${microSummary.effectiveThreshold.formatIndian(includeSymbol = true, includeDecimals = false)})",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFFFBBF24))
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "${microSummary.microTxCount} Small Purchases",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF94A3B8))
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "${microSummary.totalMicroSpend.formatIndian()} (${microSummary.microSpendPercentage}% of spend)",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.White)
                            )
                        )
                        Text(
                            text = "${microSummary.topMicroCategoryIcon} Top Leak: ${microSummary.topMicroCategoryName}",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = ColorProvider(Color(0xFFCBD5E1))
                            )
                        )
                    }

                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(if (isSpendingSlower) R.drawable.glance_glass_badge_green else R.drawable.glance_glass_badge_amber))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSpendingSlower) "🛡️ Controlled" else "⚡ High Pacing",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(if (isSpendingSlower) Color(0xFF34D399) else Color(0xFFFBBF24))
                            )
                        )
                    }
                }
            }
        }
    }
}
