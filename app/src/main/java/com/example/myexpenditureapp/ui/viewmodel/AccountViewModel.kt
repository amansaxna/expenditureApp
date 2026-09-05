package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Account
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class AccountViewModel : ViewModel() {
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val accounts: StateFlow<List<Account>> = Graph.getAccountsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveAccount(account: Account) {
        if (account.name.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(UiEvent.ShowSnackbar("Account name cannot be empty")) }
            return
        }
        viewModelScope.launch {
            try {
                Graph.saveAccountUseCase(account)
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar("Account saved"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error saving account: ${e.message}"))
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            try {
                Graph.deleteAccountUseCase(account)
                _eventFlow.emit(UiEvent.Success)
                _eventFlow.emit(UiEvent.ShowSnackbar("Account deleted"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Error deleting account: ${e.message}"))
            }
        }
    }
}
