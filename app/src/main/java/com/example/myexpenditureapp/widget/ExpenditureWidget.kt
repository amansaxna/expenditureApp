package com.example.myexpenditureapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.GlanceTheme
import androidx.compose.ui.graphics.Color
import com.example.myexpenditureapp.MainActivity
import com.example.myexpenditureapp.data.Graph
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class ExpenditureWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.provide(context)
        
        val accounts = Graph.accountRepository.getAllAccounts().first()
        val transactions = Graph.transactionRepository.getAllTransactions().first()
        
        val totalBalance = accounts.fold(BigDecimal.ZERO) { acc, account -> acc.add(account.balance) }
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        
        val monthlyIncome = transactions.filter { it.type == "Income" && it.timestamp >= monthStart }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val monthlyExpense = transactions.filter { it.type == "Expense" && it.timestamp >= monthStart }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val netFlow = monthlyIncome.subtract(monthlyExpense)

        provideContent {
            WidgetContent(totalBalance, netFlow)
        }
    }

    @Composable
    private fun WidgetContent(totalBalance: BigDecimal, netFlow: BigDecimal) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Balance",
                    style = TextStyle(
                        fontSize = 12.sp, 
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "₹${totalBalance.setScale(0, RoundingMode.HALF_UP)}",
                    style = TextStyle(
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "Monthly Net Flow",
                    style = TextStyle(
                        fontSize = 10.sp, 
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
                Text(
                    text = "${if (netFlow >= BigDecimal.ZERO) "+" else ""}₹${netFlow.setScale(0, RoundingMode.HALF_UP)}",
                    style = TextStyle(
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (netFlow >= BigDecimal.ZERO) 
                            ColorProvider(android.R.color.holo_green_light) 
                        else 
                            ColorProvider(android.R.color.holo_red_light)
                    )
                )
            }
        }
    }
}
