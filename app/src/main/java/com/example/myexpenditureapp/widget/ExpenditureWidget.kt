package com.example.myexpenditureapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.GlanceTheme
import com.example.myexpenditureapp.MainActivity
import com.example.myexpenditureapp.data.Graph
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
        val unreviewed = transactions.filter { !it.isReviewed }
        
        val totalBalance = accounts.fold(BigDecimal.ZERO) { acc, account -> acc.add(account.balance) }
        
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

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

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(28)

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
                pendingReviewCount = unreviewed.size,
                currentDay = currentDay,
                daysInMonth = daysInMonth,
                normalized7Days = normalized7Days
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
        pendingReviewCount: Int,
        currentDay: Int,
        daysInMonth: Int,
        normalized7Days: List<Pair<String, Float>>
    ) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Header Bar with Quick Add Action
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPENDITURE",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    
                    // + Quick Add button
                    Row(
                        modifier = GlanceModifier
                            .background(GlanceTheme.colors.primary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                                color = GlanceTheme.colors.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Balance & Net Flow Row
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
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Text(
                            text = "Net Balance",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (netFlow >= BigDecimal.ZERO) "+" else ""}${netFlow.formatIndian()}",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netFlow >= BigDecimal.ZERO)
                                    ColorProvider(android.R.color.holo_green_light)
                                else
                                    ColorProvider(android.R.color.holo_red_light)
                            )
                        )
                        Text(
                            text = "Net Flow",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // Daily Burn Rate Box
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Today's Burn",
                            style = TextStyle(fontSize = 9.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                        Text(
                            text = todayExpense.formatIndian(),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                        )
                    }

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Daily Avg",
                            style = TextStyle(fontSize = 9.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                        Text(
                            text = "${dailyAverage.formatIndian()}/d",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                        )
                    }

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Month Total",
                            style = TextStyle(fontSize = 9.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                        Text(
                            text = monthlyExpense.formatIndian(),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Minimalist 7-Day Outflow Trend & Pacing Card for Widget
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.surfaceVariant)
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
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "Day $currentDay of $daysInMonth",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Minimalist 7-Day Bars
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(34.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        normalized7Days.forEachIndexed { index, pair ->
                            val label = pair.first
                            val ratio = pair.second
                            val isToday = index == normalized7Days.lastIndex
                            val barHeightDp = (ratio * 24).toInt().coerceIn(4, 24).dp

                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .width(10.dp)
                                        .height(barHeightDp)
                                        .background(if (isToday) GlanceTheme.colors.primary else GlanceTheme.colors.outline)
                                ) {}
                                Spacer(modifier = GlanceModifier.height(3.dp))
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
