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

        provideContent {
            WidgetContent(
                totalBalance = totalBalance,
                monthlyExpense = monthlyExpense,
                netFlow = netFlow,
                todayExpense = todayExpense,
                dailyAverage = dailyAverage,
                pendingReviewCount = unreviewed.size
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
        pendingReviewCount: Int
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
                            text = "₹${totalBalance.setScale(0, RoundingMode.HALF_UP)}",
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
                            text = "${if (netFlow >= BigDecimal.ZERO) "+" else ""}₹${netFlow.setScale(0, RoundingMode.HALF_UP)}",
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
                            text = "₹${todayExpense.setScale(0, RoundingMode.HALF_UP)}",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                        )
                    }

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Daily Avg",
                            style = TextStyle(fontSize = 9.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                        Text(
                            text = "₹${dailyAverage.setScale(0, RoundingMode.HALF_UP)}/d",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                        )
                    }

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Month Total",
                            style = TextStyle(fontSize = 9.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                        Text(
                            text = "₹${monthlyExpense.setScale(0, RoundingMode.HALF_UP)}",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                        )
                    }
                }
            }
        }
    }
}
