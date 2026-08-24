package com.example.myexpenditureapp.domain.usecase.transaction

import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.repository.TransactionRepository

class SaveTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.saveTransaction(transaction)
    }
}
