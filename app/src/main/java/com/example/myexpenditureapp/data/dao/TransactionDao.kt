package com.example.myexpenditureapp.data.dao

import androidx.room.*
import com.example.myexpenditureapp.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:type IS NULL OR type = :type)
        AND (:startDate IS NULL OR timestamp >= :startDate)
        AND (:endDate IS NULL OR timestamp <= :endDate)
        AND (:query IS NULL OR merchant LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun getFilteredTransactions(
        accountId: Long? = null,
        categoryId: Long? = null,
        type: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        query: String? = null
    ): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND timestamp >= :startDate AND timestamp <= :endDate")
    fun getTransactionsByCategoryAndDate(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsId = :smsId)")
    suspend fun existsBySmsId(smsId: String): Boolean
}
