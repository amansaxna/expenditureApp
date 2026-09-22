package com.example.myexpenditureapp.notifications

import android.content.Context
import com.example.myexpenditureapp.data.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

object LiveStatusNotificationManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun refresh(context: Context = Graph.appContext) {
        if (!NotificationHelper.isLiveStatusEnabled(context)) {
            NotificationHelper.cancelLiveStatusNotification(context)
            return
        }

        scope.launch {
            try {
                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val remainingDays = (daysInMonth - currentDay).coerceAtLeast(0)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)

                // Today start (00:00:00)
                val todayCal = Calendar.getInstance()
                todayCal.set(Calendar.HOUR_OF_DAY, 0)
                todayCal.set(Calendar.MINUTE, 0)
                todayCal.set(Calendar.SECOND, 0)
                todayCal.set(Calendar.MILLISECOND, 0)
                val todayStart = todayCal.timeInMillis

                // Month start
                val monthCal = Calendar.getInstance()
                monthCal.set(Calendar.DAY_OF_MONTH, 1)
                monthCal.set(Calendar.HOUR_OF_DAY, 0)
                monthCal.set(Calendar.MINUTE, 0)
                monthCal.set(Calendar.SECOND, 0)
                monthCal.set(Calendar.MILLISECOND, 0)
                val monthStart = monthCal.timeInMillis

                val transactions = Graph.transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()
                val budgets = Graph.budgetRepository.getAllBudgets().firstOrNull() ?: emptyList()

                var todaySpent = BigDecimal.ZERO
                var monthlySpent = BigDecimal.ZERO

                transactions.forEach { tx ->
                    if (tx.type.equals("Expense", ignoreCase = true)) {
                        if (tx.timestamp >= monthStart) {
                            monthlySpent = monthlySpent.add(tx.amount)
                        }
                        if (tx.timestamp >= todayStart) {
                            todaySpent = todaySpent.add(tx.amount)
                        }
                    }
                }

                val thisMonthBudgets = budgets.filter { it.month == currentMonth && it.year == currentYear }
                var totalBudget = BigDecimal.ZERO
                thisMonthBudgets.forEach { totalBudget = totalBudget.add(it.limitAmount) }

                NotificationHelper.showLiveStatusNotification(
                    context = context,
                    todaySpent = todaySpent,
                    monthlySpent = monthlySpent,
                    monthlyBudget = totalBudget,
                    remainingDays = remainingDays
                )
            } catch (e: Exception) {
                // Ignore background errors
            }
        }
    }
}
