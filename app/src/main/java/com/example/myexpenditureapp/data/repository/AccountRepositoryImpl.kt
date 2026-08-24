package com.example.myexpenditureapp.data.repository

import com.example.myexpenditureapp.data.dao.AccountDao
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class AccountRepositoryImpl(private val accountDao: AccountDao) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    override suspend fun getAccountById(id: Long): Account? = accountDao.getAccountById(id)

    override suspend fun saveAccount(account: Account) {
        accountDao.insertAccount(account)
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    override suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)
}
