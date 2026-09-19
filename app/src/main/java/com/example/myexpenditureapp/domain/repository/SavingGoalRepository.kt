package com.example.myexpenditureapp.domain.repository

import com.example.myexpenditureapp.data.entity.SavingGoal
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface SavingGoalRepository {
    fun getAllGoals(): Flow<List<SavingGoal>>
    suspend fun getAllGoalsSync(): List<SavingGoal>
    suspend fun getGoalById(id: Long): SavingGoal?
    suspend fun saveGoal(goal: SavingGoal): Long
    suspend fun depositFunds(goalId: Long, amount: BigDecimal)
    suspend fun withdrawFunds(goalId: Long, amount: BigDecimal)
    suspend fun deleteGoal(goal: SavingGoal)
    suspend fun deleteGoalById(id: Long)
}
