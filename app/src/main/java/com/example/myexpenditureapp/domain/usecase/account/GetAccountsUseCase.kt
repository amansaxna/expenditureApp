package com.example.myexpenditureapp.domain.usecase.account

import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(private val repository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> = repository.getAllAccounts()
}
