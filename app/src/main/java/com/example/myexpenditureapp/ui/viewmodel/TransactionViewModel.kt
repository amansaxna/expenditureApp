package com.example.myexpenditureapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.notifications.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object Success : UiEvent()
}

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val filterAccountId: Long? = null,
    val filterCategoryId: Long? = null,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isLoading: Boolean = false,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionRepository = Graph.transactionRepository
    private val getAccountsUseCase = Graph.getAccountsUseCase
    private val getCategoriesUseCase = Graph.getCategoriesUseCase
    private val deleteTransactionUseCase = Graph.deleteTransactionUseCase

    private val _searchQuery = MutableStateFlow("")
    private val _filterAccountId = MutableStateFlow<Long?>(null)
    private val _filterCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val unreviewedTransactions: StateFlow<List<Transaction>> = transactionRepository.getUnreviewedTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionUiState> = combine(
        combine(_searchQuery, _filterAccountId, _filterCategoryId) { q, a, c -> Triple(q, a, c) },
        combine(_selectedMonth, _selectedYear) { m, y -> m to y },
        getAccountsUseCase(),
        getCategoriesUseCase.getAll()
    ) { params, dateParams, accounts, categories ->
        val (query, accId, catId) = params
        val (month, year) = dateParams
        
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startDate = cal.timeInMillis
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endDate = cal.timeInMillis

        transactionRepository.getFilteredTransactions(
            accountId = accId,
            categoryId = catId,
            query = if (query.isEmpty()) null else query,
            startDate = startDate,
            endDate = endDate
        ).map { transactions ->
            val income = transactions.filter { it.type == "Income" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
            val expense = transactions.filter { it.type == "Expense" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
            TransactionUiState(
                transactions = transactions,
                accounts = accounts,
                categories = categories,
                searchQuery = query,
                filterAccountId = accId,
                filterCategoryId = catId,
                selectedMonth = month,
                selectedYear = year,
                totalIncome = income,
                totalExpense = expense,
                netBalance = income.subtract(expense)
            )
        }
    }.flatMapLatest { it }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterAccountChange(accountId: Long?) {
        _filterAccountId.value = accountId
    }

    fun onFilterCategoryChange(categoryId: Long?) {
        _filterCategoryId.value = categoryId
    }

    fun onMonthYearChange(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionRepository.getTransactionById(id)
    }

    fun saveTransaction(
        accountId: Long,
        toAccountId: Long? = null,
        categoryId: Long? = null,
        amount: BigDecimal,
        merchant: String,
        type: String,
        timestamp: Long = System.currentTimeMillis(),
        id: Long = 0L,
        tags: List<String> = emptyList(),
        saveAsRule: Boolean = false
    ) {
        if (amount <= BigDecimal.ZERO) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Amount must be greater than zero")) }
            return
        }
        if (merchant.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Merchant/Description cannot be empty")) }
            return
        }
        if (accountId == 0L) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Please select an account")) }
            return
        }
        if (type == "Transfer" && toAccountId == null) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Please select a destination account")) }
            return
        }
        if (type == "Transfer" && toAccountId == accountId) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Source and destination accounts must be different")) }
            return
        }

        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    id = id,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    amount = amount,
                    merchant = merchant,
                    type = type,
                    timestamp = timestamp,
                    isReviewed = true,
                    tags = tags
                )
                transactionRepository.saveTransaction(transaction)

                if (saveAsRule && categoryId != null && merchant.isNotBlank()) {
                    try {
                        Graph.autoCategoryRuleRepository.saveRule(
                            com.example.myexpenditureapp.data.entity.AutoCategoryRule(
                                keyword = merchant.trim().uppercase(),
                                categoryId = categoryId,
                                matchType = "CONTAINS"
                            )
                        )
                    } catch (e: Exception) {
                        // ignore duplicate rule errors
                    }
                }

                NotificationHelper.triggerBudgetCheck(getApplication())
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar(if (id == 0L) "Transaction saved" else "Transaction updated"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error saving transaction: ${e.message}"))
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                deleteTransactionUseCase(transaction)
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar("Transaction deleted"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error deleting transaction: ${e.message}"))
            }
        }
    }

    fun updateTransactionCategory(transaction: Transaction, categoryId: Long?) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(transaction.copy(categoryId = categoryId))
            NotificationHelper.triggerBudgetCheck(getApplication())
        }
    }

    fun updateTransactionAmount(transaction: Transaction, amount: BigDecimal) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(transaction.copy(amount = amount))
            NotificationHelper.triggerBudgetCheck(getApplication())
        }
    }

    fun markAsReviewed(id: Long) {
        viewModelScope.launch {
            transactionRepository.markAsReviewed(id)
        }
    }

    fun reviewTransaction(transactionId: Long, categoryId: Long? = null, saveAsRule: Boolean = false) {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(transactionId) ?: return@launch
            val updated = tx.copy(categoryId = categoryId ?: tx.categoryId, isReviewed = true)
            transactionRepository.saveTransaction(updated)
            if (saveAsRule && categoryId != null && tx.merchant.isNotBlank()) {
                try {
                    Graph.autoCategoryRuleRepository.saveRule(
                        com.example.myexpenditureapp.data.entity.AutoCategoryRule(
                            keyword = tx.merchant.trim().uppercase(),
                            categoryId = categoryId,
                            matchType = "CONTAINS"
                        )
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }
            NotificationHelper.triggerBudgetCheck(getApplication())
            _eventFlow.emit(UiEvent.ShowSnackbar("Transaction reviewed!"))
            _eventFlow.emit(UiEvent.Success)
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            try {
                transactionRepository.deleteAllTransactions()
                _eventFlow.emit(UiEvent.ShowSnackbar("All transactions cleared"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error clearing transactions: ${e.message}"))
            }
        }
    }
}
