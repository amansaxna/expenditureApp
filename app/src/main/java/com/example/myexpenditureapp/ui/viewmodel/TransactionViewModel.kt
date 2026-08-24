package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val filterAccountId: Long? = null,
    val filterCategoryId: Long? = null,
    val isLoading: Boolean = false
)

class TransactionViewModel : ViewModel() {
    private val transactionRepository = Graph.transactionRepository
    private val getAccountsUseCase = Graph.getAccountsUseCase
    private val getCategoriesUseCase = Graph.getCategoriesUseCase
    private val deleteTransactionUseCase = Graph.deleteTransactionUseCase

    private val _searchQuery = MutableStateFlow("")
    private val _filterAccountId = MutableStateFlow<Long?>(null)
    private val _filterCategoryId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionUiState> = combine(
        _searchQuery,
        _filterAccountId,
        _filterCategoryId,
        getAccountsUseCase(),
        getCategoriesUseCase.getAll()
    ) { query, accId, catId, accounts, categories ->
        Triple(query, accId, catId) to (accounts to categories)
    }.flatMapLatest { (params, data) ->
        val (query, accId, catId) = params
        val (accounts, categories) = data
        transactionRepository.getFilteredTransactions(
            accountId = accId,
            categoryId = catId,
            query = if (query.isEmpty()) null else query
        ).map { transactions ->
            TransactionUiState(
                transactions = transactions,
                accounts = accounts,
                categories = categories,
                searchQuery = query,
                filterAccountId = accId,
                filterCategoryId = catId
            )
        }
    }.stateIn(
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

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionRepository.getTransactionById(id)
    }

    fun saveTransaction(
        accountId: Long,
        toAccountId: Long? = null,
        categoryId: Long? = null,
        amount: Double,
        merchant: String,
        type: String,
        timestamp: Long = System.currentTimeMillis(),
        id: Long = 0L
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                id = id,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                amount = amount,
                merchant = merchant,
                type = type,
                timestamp = timestamp
            )
            transactionRepository.saveTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransactionUseCase(transaction)
        }
    }

    fun updateTransactionCategory(transaction: Transaction, categoryId: Long?) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(transaction.copy(categoryId = categoryId))
        }
    }

    fun updateTransactionAmount(transaction: Transaction, amount: Double) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(transaction.copy(amount = amount))
        }
    }
}
