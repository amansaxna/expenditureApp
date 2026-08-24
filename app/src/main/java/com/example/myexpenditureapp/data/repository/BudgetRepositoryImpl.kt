package com.example.myexpenditureapp.data.repository

import com.example.myexpenditureapp.data.dao.BudgetDao
import com.example.myexpenditureapp.data.dao.CategoryDao
import com.example.myexpenditureapp.data.dao.TransactionDao
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import com.example.myexpenditureapp.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.*
import java.util.*

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getBudgetsWithProgress(): Flow<List<BudgetWithProgress>> {
        return combine(
            budgetDao.getAllBudgets(),
            categoryDao.getAllCategories(),
            transactionDao.getAllTransactions()
        ) { budgets, categories, transactions ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            budgets.map { budget ->
                val category = categories.find { it.id == budget.categoryId }
                val currentSpending = transactions
                    .filter { 
                        it.categoryId == budget.categoryId && 
                        it.timestamp >= startOfMonth && 
                        it.type == "Expense" 
                    }
                    .sumOf { it.amount }
                
                BudgetWithProgress(
                    budget = budget,
                    category = category,
                    currentSpending = currentSpending,
                    progress = if (budget.limitAmount > 0) (currentSpending / budget.limitAmount).toFloat() else 0f
                )
            }
        }
    }

    override suspend fun saveBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
}
