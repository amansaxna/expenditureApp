package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel : ViewModel() {
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

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
        if (category.name.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Category name cannot be empty")) }
            return
        }
        viewModelScope.launch {
            try {
                Graph.saveCategoryUseCase(category)
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar("Category saved"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error saving category: ${e.message}"))
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                Graph.deleteCategoryUseCase(category)
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar("Category deleted"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error deleting category: ${e.message}"))
            }
        }
    }

    fun getSubcategories(parentId: Long) = Graph.getCategoriesUseCase.getSub(parentId)
}
