package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

class BudgetViewModel : ViewModel() {
    private val budgetRepository = Graph.budgetRepository
    private val categoryRepository = Graph.categoryRepository

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear = _selectedYear.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetsWithProgress: StateFlow<List<BudgetWithProgress>> = 
        combine(_selectedMonth, _selectedYear) { month, year -> month to year }
            .flatMapLatest { (month, year) ->
                budgetRepository.getBudgetsWithProgress(month, year)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passedBudgetsCount: StateFlow<Int> = budgetsWithProgress.map { list ->
        list.count { it.progress <= 1.0f }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedBudgetsCount: StateFlow<Int> = budgetsWithProgress.map { list ->
        list.count { it.progress > 1.0f }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onMonthYearChanged(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun saveBudget(budget: Budget) {
        if (budget.limitAmount <= BigDecimal.ZERO) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Budget limit must be greater than zero")) }
            return
        }
        viewModelScope.launch {
            try {
                // Ensure budget has the correct month/year if it's new
                val budgetToSave = if (budget.id == 0L) {
                    budget.copy(month = _selectedMonth.value, year = _selectedYear.value)
                } else budget
                
                budgetRepository.saveBudget(budgetToSave)
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar("Budget saved"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error saving budget: ${e.message}"))
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budget)
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar("Budget deleted"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error deleting budget: ${e.message}"))
            }
        }
    }
}
