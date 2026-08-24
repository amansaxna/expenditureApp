package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Account
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {
    val accounts: StateFlow<List<Account>> = Graph.getAccountsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveAccount(account: Account) {
        viewModelScope.launch {
            Graph.saveAccountUseCase(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            Graph.deleteAccountUseCase(account)
        }
    }
}
