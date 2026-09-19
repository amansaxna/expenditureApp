package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.SavingGoal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class GoalViewModel : ViewModel() {
    private val goalRepo = Graph.savingGoalRepository

    val goals: StateFlow<List<SavingGoal>> = goalRepo.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventChannel = Channel<UiEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    fun saveGoal(
        name: String,
        targetAmount: BigDecimal,
        initialAmount: BigDecimal = BigDecimal.ZERO,
        targetDateEpochMs: Long? = null,
        icon: String = "🎯",
        colorHex: String = "#3B82F6",
        id: Long = 0
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _eventChannel.send(UiEvent.ShowSnackbar("Goal name cannot be empty"))
                return@launch
            }
            if (targetAmount <= BigDecimal.ZERO) {
                _eventChannel.send(UiEvent.ShowSnackbar("Target amount must be greater than 0"))
                return@launch
            }
            try {
                val goal = SavingGoal(
                    id = id,
                    name = name.trim(),
                    targetAmount = targetAmount,
                    currentAmount = initialAmount,
                    targetDateEpochMs = targetDateEpochMs,
                    icon = icon,
                    colorHex = colorHex,
                    isCompleted = initialAmount >= targetAmount
                )
                goalRepo.saveGoal(goal)
                _eventChannel.send(UiEvent.ShowSnackbar("Goal saved!"))
                _eventChannel.send(UiEvent.Success)
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error saving goal: ${e.localizedMessage}"))
            }
        }
    }

    fun depositFunds(goalId: Long, amount: BigDecimal) {
        viewModelScope.launch {
            if (amount <= BigDecimal.ZERO) {
                _eventChannel.send(UiEvent.ShowSnackbar("Amount must be greater than 0"))
                return@launch
            }
            try {
                goalRepo.depositFunds(goalId, amount)
                _eventChannel.send(UiEvent.ShowSnackbar("Deposited ₹${amount.toPlainString()} to goal!"))
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun withdrawFunds(goalId: Long, amount: BigDecimal) {
        viewModelScope.launch {
            if (amount <= BigDecimal.ZERO) {
                _eventChannel.send(UiEvent.ShowSnackbar("Amount must be greater than 0"))
                return@launch
            }
            try {
                goalRepo.withdrawFunds(goalId, amount)
                _eventChannel.send(UiEvent.ShowSnackbar("Withdrew ₹${amount.toPlainString()} from goal"))
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteGoal(goal: SavingGoal) {
        viewModelScope.launch {
            goalRepo.deleteGoal(goal)
            _eventChannel.send(UiEvent.ShowSnackbar("Goal deleted"))
        }
    }
}
