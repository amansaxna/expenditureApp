package com.example.myexpenditureapp.domain.usecase.transaction

import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAllTransactions()
}
