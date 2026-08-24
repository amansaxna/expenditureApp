package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel : ViewModel() {
    val allCategories: StateFlow<List<Category>> = Graph.getCategoriesUseCase.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val rootCategories: StateFlow<List<Category>> = Graph.getCategoriesUseCase.getRoot()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            Graph.saveCategoryUseCase(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            Graph.deleteCategoryUseCase(category)
        }
    }

    fun getSubcategories(parentId: Long) = Graph.getCategoriesUseCase.getSub(parentId)
}
