package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {
    private val budgetRepository = Graph.budgetRepository
    private val categoryRepository = Graph.categoryRepository

    val budgetsWithProgress: StateFlow<List<BudgetWithProgress>> = budgetRepository.getBudgetsWithProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.saveBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }
}
