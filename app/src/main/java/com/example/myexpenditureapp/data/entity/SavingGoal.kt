package com.example.myexpenditureapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "saving_goals")
data class SavingGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal = BigDecimal.ZERO,
    val targetDateEpochMs: Long? = null,
    val icon: String = "🎯",
    val colorHex: String = "#3B82F6",
    val isCompleted: Boolean = false
)
