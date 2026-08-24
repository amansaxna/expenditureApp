package com.example.myexpenditureapp.domain.usecase.transaction

import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.repository.TransactionRepository

class DeleteTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.deleteTransaction(transaction)
    }
}
