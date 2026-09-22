package com.example.myexpenditureapp.data.repository

import com.example.myexpenditureapp.data.dao.SavingGoalDao
import com.example.myexpenditureapp.data.entity.SavingGoal
import com.example.myexpenditureapp.domain.repository.SavingGoalRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class SavingGoalRepositoryImpl(
    private val goalDao: SavingGoalDao
) : SavingGoalRepository {

    override fun getAllGoals(): Flow<List<SavingGoal>> = goalDao.getAllGoals()

    override suspend fun getAllGoalsSync(): List<SavingGoal> = goalDao.getAllGoalsSync()

    override suspend fun getGoalById(id: Long): SavingGoal? = goalDao.getGoalById(id)

    override suspend fun saveGoal(goal: SavingGoal): Long {
        return if (goal.id != 0L) {
            goalDao.updateGoal(goal)
            goal.id
        } else {
            goalDao.insertGoal(goal)
        }
    }

    override suspend fun depositFunds(goalId: Long, amount: BigDecimal) {
        val existing = goalDao.getGoalById(goalId) ?: return
        val newAmount = existing.currentAmount.add(amount)
        val isCompleted = newAmount >= existing.targetAmount
        goalDao.updateGoal(existing.copy(currentAmount = newAmount, isCompleted = isCompleted))
    }

    override suspend fun withdrawFunds(goalId: Long, amount: BigDecimal) {
        val existing = goalDao.getGoalById(goalId) ?: return
        val newAmount = existing.currentAmount.subtract(amount).max(BigDecimal.ZERO)
        val isCompleted = newAmount >= existing.targetAmount
        goalDao.updateGoal(existing.copy(currentAmount = newAmount, isCompleted = isCompleted))
    }

    override suspend fun deleteGoal(goal: SavingGoal) {
        goalDao.deleteGoal(goal)
    }

    override suspend fun deleteGoalById(id: Long) {
        goalDao.deleteGoalById(id)
    }
}
