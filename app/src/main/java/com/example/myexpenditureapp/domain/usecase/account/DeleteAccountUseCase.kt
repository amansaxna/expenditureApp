package com.example.myexpenditureapp.domain.usecase.account

import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.domain.repository.AccountRepository

class DeleteAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(account: Account) = repository.deleteAccount(account)
}
