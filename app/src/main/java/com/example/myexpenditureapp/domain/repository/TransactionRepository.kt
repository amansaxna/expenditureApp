package com.example.myexpenditureapp.domain.repository

import com.example.myexpenditureapp.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getFilteredTransactions(
        accountId: Long? = null,
        categoryId: Long? = null,
        type: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        query: String? = null
    ): Flow<List<Transaction>>
    suspend fun saveTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun existsBySmsId(smsId: String): Boolean
}
