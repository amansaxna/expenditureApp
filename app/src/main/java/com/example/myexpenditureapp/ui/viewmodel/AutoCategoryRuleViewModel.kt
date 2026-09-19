package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.AutoCategoryRule
import com.example.myexpenditureapp.data.entity.Category
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AutoCategoryRuleItem(
    val rule: AutoCategoryRule,
    val categoryName: String,
    val categoryIcon: String?
)

class AutoCategoryRuleViewModel : ViewModel() {
    private val ruleRepo = Graph.autoCategoryRuleRepository
    private val categoryRepo = Graph.categoryRepository

    val rulesWithCategory: StateFlow<List<AutoCategoryRuleItem>> = combine(
        ruleRepo.getAllRules(),
        categoryRepo.getAllCategories()
    ) { rules, categories ->
        val categoryMap = categories.associateBy { it.id }
        rules.map { rule ->
            val cat = categoryMap[rule.categoryId]
            AutoCategoryRuleItem(
                rule = rule,
                categoryName = cat?.name ?: "Unknown",
                categoryIcon = cat?.icon ?: "📁"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepo.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventChannel = Channel<UiEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    fun saveRule(keyword: String, categoryId: Long, matchType: String = "CONTAINS", id: Long = 0) {
        viewModelScope.launch {
            if (keyword.isBlank()) {
                _eventChannel.send(UiEvent.ShowSnackbar("Keyword cannot be empty"))
                return@launch
            }
            try {
                val rule = AutoCategoryRule(
                    id = id,
                    keyword = keyword.trim().uppercase(),
                    categoryId = categoryId,
                    matchType = matchType
                )
                ruleRepo.saveRule(rule)
                _eventChannel.send(UiEvent.ShowSnackbar("Rule saved successfully!"))
                _eventChannel.send(UiEvent.Success)
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error saving rule: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteRule(rule: AutoCategoryRule) {
        viewModelScope.launch {
            ruleRepo.deleteRule(rule)
            _eventChannel.send(UiEvent.ShowSnackbar("Rule deleted"))
        }
    }
}
