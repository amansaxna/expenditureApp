package com.example.myexpenditureapp.domain.usecase.account

import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.domain.repository.AccountRepository

class SaveAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(account: Account) = repository.saveAccount(account)
}
