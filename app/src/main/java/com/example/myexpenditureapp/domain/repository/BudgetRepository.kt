package com.example.myexpenditureapp.domain.repository

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsWithProgress(): Flow<List<BudgetWithProgress>>
    suspend fun saveBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
}
