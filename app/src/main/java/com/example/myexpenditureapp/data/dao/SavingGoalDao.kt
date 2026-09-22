package com.example.myexpenditureapp.data.dao

import androidx.room.*
import com.example.myexpenditureapp.data.entity.SavingGoal
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals ORDER BY isCompleted ASC, id DESC")
    fun getAllGoals(): Flow<List<SavingGoal>>

    @Query("SELECT * FROM saving_goals")
    suspend fun getAllGoalsSync(): List<SavingGoal>

    @Query("SELECT * FROM saving_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): SavingGoal?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoal(goal: SavingGoal): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(goals: List<SavingGoal>)

    @Update
    suspend fun updateGoal(goal: SavingGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingGoal)

    @Query("DELETE FROM saving_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    @Query("DELETE FROM saving_goals")
    suspend fun deleteAllGoals()
}
