package com.example.myexpenditureapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myexpenditureapp.data.Graph
import kotlinx.coroutines.flow.first

class BudgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Graph.provide(applicationContext)
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val year = calendar.get(java.util.Calendar.YEAR)
        
        val budgets = Graph.budgetRepository.getBudgetsWithProgress(month, year).first()
        
        for (budgetProgress in budgets) {
            val progress = budgetProgress.progress
            val categoryName = budgetProgress.category?.name ?: "General"
            
            if (progress >= 1.0f) {
                NotificationHelper.showBudgetAlert(applicationContext, categoryName, 100)
            } else if (progress >= 0.8f) {
                NotificationHelper.showBudgetAlert(applicationContext, categoryName, 80)
            }
        }
        
        return Result.success()
    }
}
