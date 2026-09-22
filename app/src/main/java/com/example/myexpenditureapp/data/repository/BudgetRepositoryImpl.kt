package com.example.myexpenditureapp.data.repository

import com.example.myexpenditureapp.data.dao.BudgetDao
import com.example.myexpenditureapp.data.dao.CategoryDao
import com.example.myexpenditureapp.data.dao.TransactionDao
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import com.example.myexpenditureapp.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.*
import java.util.*
import java.math.BigDecimal
import java.math.RoundingMode

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getAllBudgets(month: Int?, year: Int?): Flow<List<Budget>> {
        return if (month != null && year != null) {
            budgetDao.getBudgetsForMonth(month, year)
        } else {
            budgetDao.getAllBudgets()
        }
    }

    override fun getBudgetsWithProgress(month: Int, year: Int): Flow<List<BudgetWithProgress>> {
        return combine(
            budgetDao.getBudgetsForMonth(month, year),
            categoryDao.getAllCategories(),
            transactionDao.getAllTransactions()
        ) { budgets, categories, transactions ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis

            budgets.map { budget ->
                val category = categories.find { it.id == budget.categoryId }
                val currentSpending = transactions
                    .filter { 
                        it.categoryId == budget.categoryId && 
                        it.timestamp in startOfMonth..endOfMonth && 
                        it.type == "Expense" 
                    }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
                
                BudgetWithProgress(
                    budget = budget,
                    category = category,
                    currentSpending = currentSpending,
                    progress = if (budget.limitAmount > BigDecimal.ZERO) 
                        currentSpending.divide(budget.limitAmount, 4, RoundingMode.HALF_UP).toFloat() 
                    else 0f
                )
            }
        }
    }

    override suspend fun saveBudget(budget: Budget) {
        if (budget.id != 0L) {
            budgetDao.updateBudget(budget)
        } else {
            budgetDao.insertBudget(budget)
        }
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
}
