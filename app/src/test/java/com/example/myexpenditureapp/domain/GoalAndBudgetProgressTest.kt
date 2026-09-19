package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.SavingGoal
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class GoalAndBudgetProgressTest {

    @Test
    fun testGoalProgressCalculation() {
        val goal = SavingGoal(
            id = 1L,
            name = "Emergency Fund",
            targetAmount = BigDecimal("100000.00"),
            currentAmount = BigDecimal("35000.00"),
            colorHex = "#4CAF50",
            icon = "🛡️",
            targetDateEpochMs = System.currentTimeMillis() + 100000000L
        )

        val progress = goal.currentAmount.divide(goal.targetAmount, 4, RoundingMode.HALF_UP).toFloat()
        assertEquals(0.35f, progress, 0.001f)

        val remaining = goal.targetAmount.subtract(goal.currentAmount)
        assertEquals(BigDecimal("65000.00"), remaining)
    }

    @Test
    fun testGoalCompletedProgress() {
        val goal = SavingGoal(
            id = 2L,
            name = "New Phone",
            targetAmount = BigDecimal("50000.00"),
            currentAmount = BigDecimal("55000.00"),
            colorHex = "#2196F3",
            icon = "📱",
            isCompleted = true
        )

        val progress = (goal.currentAmount.divide(goal.targetAmount, 4, RoundingMode.HALF_UP).toFloat()).coerceAtMost(1.0f)
        assertEquals(1.0f, progress, 0.001f)
        assertTrue(goal.isCompleted)
    }

    @Test
    fun testBudgetProgressCalculation() {
        val budget = Budget(
            id = 1L,
            categoryId = 5L,
            limitAmount = BigDecimal("10000.00"),
            period = "MONTHLY",
            month = 8,
            year = 2026
        )

        val spent = BigDecimal("7500.00")
        val progress = spent.divide(budget.limitAmount, 4, RoundingMode.HALF_UP).toFloat()
        val remaining = budget.limitAmount.subtract(spent)

        assertEquals(0.75f, progress, 0.001f)
        assertEquals(BigDecimal("2500.00"), remaining)
        assertFalse("Not over budget", spent > budget.limitAmount)
    }

    @Test
    fun testBudgetOverspentDetection() {
        val budget = Budget(
            id = 2L,
            categoryId = 6L,
            limitAmount = BigDecimal("5000.00"),
            period = "MONTHLY",
            month = 8,
            year = 2026
        )

        val spent = BigDecimal("6200.00")
        val isOverBudget = spent > budget.limitAmount
        val overAmount = spent.subtract(budget.limitAmount)

        assertTrue("Should be over budget", isOverBudget)
        assertEquals(BigDecimal("1200.00"), overAmount)
    }
}
