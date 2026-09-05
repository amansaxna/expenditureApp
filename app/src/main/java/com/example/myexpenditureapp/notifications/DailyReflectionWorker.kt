package com.example.myexpenditureapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myexpenditureapp.data.Graph
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.*

class DailyReflectionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Graph.provide(applicationContext)
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.timeInMillis

        val todayTransactions = Graph.transactionRepository.getFilteredTransactions(startDate = startOfToday).first()
        val todaySpend = todayTransactions
            .filter { it.type == "Expense" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }

        val monthTransactions = Graph.transactionRepository.getFilteredTransactions(startDate = startOfMonth).first()
        val income = monthTransactions
            .filter { it.type == "Income" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val expense = monthTransactions
            .filter { it.type == "Expense" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val netFlow = income.subtract(expense)

        NotificationHelper.showDailyReflection(
            applicationContext,
            todaySpend.stripTrailingZeros().toPlainString(),
            netFlow.stripTrailingZeros().toPlainString()
        )

        // If it's the last day of the month, show monthly summary too
        val today = Calendar.getInstance()
        if (today.get(Calendar.DAY_OF_MONTH) == today.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            NotificationHelper.showMonthlySummary(applicationContext, netFlow.stripTrailingZeros().toPlainString())
        }

        return Result.success()
    }
}
