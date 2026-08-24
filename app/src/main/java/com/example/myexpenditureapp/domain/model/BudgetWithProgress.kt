package com.example.myexpenditureapp.domain.model

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category

data class BudgetWithProgress(
    val budget: Budget,
    val category: Category?,
    val currentSpending: Double,
    val progress: Float // 0.0 to 1.0 or more
)
