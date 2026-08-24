package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import kotlinx.coroutines.flow.*
import java.util.*

data class AnalyticsUiState(
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val dateRange: LongRange? = null
)

class AnalyticsViewModel : ViewModel() {
    private val transactionRepository = Graph.transactionRepository
    private val categoryRepository = Graph.categoryRepository
    private val getAccountsUseCase = Graph.getAccountsUseCase

    private val _filterState = MutableStateFlow(AnalyticsUiState())
    val filterState = _filterState.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = getAccountsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions = combine(transactions, _filterState) { txs, filters ->
        txs.filter { tx ->
            val accountMatch = filters.selectedAccountId == null || tx.accountId == filters.selectedAccountId
            val categoryMatch = filters.selectedCategoryId == null || tx.categoryId == filters.selectedCategoryId
            val dateMatch = filters.dateRange == null || tx.timestamp in filters.dateRange
            accountMatch && categoryMatch && dateMatch
        }
    }

    val categorySpending: Flow<Map<Category?, Double>> = combine(filteredTransactions, categories) { txs, cats ->
        txs.filter { it.type == "Expense" }
            .groupBy { tx -> cats.find { it.id == tx.categoryId } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val monthlyComparison: Flow<Map<String, Double>> = transactions.map { txs ->
        val calendar = Calendar.getInstance()
        txs.filter { it.type == "Expense" }
            .groupBy { tx ->
                calendar.timeInMillis = tx.timestamp
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                val year = calendar.get(Calendar.YEAR)
                "$month $year"
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toSortedMap(compareByDescending { it })
    }

    val spendingTrend: Flow<Map<Long, Double>> = filteredTransactions.map { txs ->
        txs.filter { it.type == "Expense" }
            .groupBy { it.timestamp / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000) } // Group by day
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toSortedMap()
    }

    val kpis: Flow<Map<String, String>> = combine(filteredTransactions, categories) { txs, cats ->
        val expenses = txs.filter { it.type == "Expense" }
        val incomes = txs.filter { it.type == "Income" }
        
        val totalSpending = expenses.sumOf { it.amount }
        val totalIncome = incomes.sumOf { it.amount }
        
        val highestCategory = expenses.groupBy { it.categoryId }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .maxByOrNull { it.value }
            ?.let { cats.find { c -> c.id == it.key }?.name ?: "Other" } ?: "N/A"
            
        val days = txs.map { it.timestamp / (24 * 60 * 60 * 1000) }.distinct().size.coerceAtLeast(1)
        val avgDaily = totalSpending / days
        
        val ratio = if (totalSpending == 0.0) totalIncome else totalIncome / totalSpending

        mapOf(
            "Monthly Spending" to "₹${String.format("%.0f", totalSpending)}",
            "Avg. Daily" to "₹${String.format("%.0f", avgDaily)}",
            "Top Category" to highestCategory,
            "Income/Expense" to String.format("%.2f", ratio)
        )
    }

    fun onAccountFilterChange(accountId: Long?) {
        _filterState.value = _filterState.value.copy(selectedAccountId = accountId)
    }

    fun onCategoryFilterChange(categoryId: Long?) {
        _filterState.value = _filterState.value.copy(selectedCategoryId = categoryId)
    }
    
    fun onDateRangeChange(start: Long?, end: Long?) {
        val range = if (start != null && end != null) start..end else null
        _filterState.value = _filterState.value.copy(dateRange = range)
    }
}
