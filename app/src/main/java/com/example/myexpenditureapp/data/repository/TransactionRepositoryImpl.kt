package com.example.myexpenditureapp.data.repository

import androidx.room.withTransaction
import com.example.myexpenditureapp.data.AppDatabase
import com.example.myexpenditureapp.data.dao.AccountDao
import com.example.myexpenditureapp.data.dao.TransactionDao
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class TransactionRepositoryImpl(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    override fun getFilteredTransactions(
        accountId: Long?,
        categoryId: Long?,
        type: String?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): Flow<List<Transaction>> = transactionDao.getFilteredTransactions(accountId, categoryId, type, startDate, endDate, query)

    override fun getUnreviewedTransactions(): Flow<List<Transaction>> = transactionDao.getUnreviewedTransactions()

    override suspend fun markAsReviewed(id: Long) {
        transactionDao.markAsReviewed(id)
    }

    override suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

    override suspend fun saveTransaction(transaction: Transaction) {
        database.withTransaction {
            if (transaction.id != 0L) {
                val oldTransaction = transactionDao.getTransactionById(transaction.id)
                if (oldTransaction != null) {
                    // Revert old transaction balances
                    adjustBalances(oldTransaction, -1)
                }
            }
            
            // Apply new transaction balances
            adjustBalances(transaction, 1)
            
            transactionDao.insertTransaction(transaction)
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        database.withTransaction {
            // Revert balances
            adjustBalances(transaction, -1)
            transactionDao.deleteTransaction(transaction)
        }
    }

    private suspend fun adjustBalances(transaction: Transaction, multiplier: Int) {
        val multiplierBigDecimal = BigDecimal(multiplier)
        // Adjust primary account balance
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            val balanceChange = when (transaction.type) {
                "Expense" -> transaction.amount.negate()
                "Income" -> transaction.amount
                "Transfer" -> transaction.amount.negate()
                else -> BigDecimal.ZERO
            }
            val newBalance = account.balance.add(balanceChange.multiply(multiplierBigDecimal))
            accountDao.updateAccount(account.copy(balance = newBalance))
        }

        // Adjust destination account balance for Transfers
        if (transaction.type == "Transfer" && transaction.toAccountId != null) {
            val toAccount = accountDao.getAccountById(transaction.toAccountId)
            if (toAccount != null) {
                val newToBalance = toAccount.balance.add(transaction.amount.multiply(multiplierBigDecimal))
                accountDao.updateAccount(toAccount.copy(balance = newToBalance))
            }
        }
    }

    override suspend fun existsBySmsId(smsId: String): Boolean = transactionDao.existsBySmsId(smsId)

    override suspend fun deleteAllTransactions() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            accountDao.resetAllBalances()
        }
    }
}
