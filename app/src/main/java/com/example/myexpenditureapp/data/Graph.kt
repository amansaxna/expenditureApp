package com.example.myexpenditureapp.data

import android.content.Context
import com.example.myexpenditureapp.data.repository.AccountRepositoryImpl
import com.example.myexpenditureapp.data.repository.BudgetRepositoryImpl
import com.example.myexpenditureapp.data.repository.CategoryRepositoryImpl
import com.example.myexpenditureapp.data.repository.TransactionRepositoryImpl
import com.example.myexpenditureapp.domain.repository.AccountRepository
import com.example.myexpenditureapp.domain.repository.BudgetRepository
import com.example.myexpenditureapp.domain.repository.CategoryRepository
import com.example.myexpenditureapp.domain.repository.TransactionRepository
import com.example.myexpenditureapp.domain.usecase.account.DeleteAccountUseCase
import com.example.myexpenditureapp.domain.usecase.account.GetAccountsUseCase
import com.example.myexpenditureapp.domain.usecase.account.SaveAccountUseCase
import com.example.myexpenditureapp.domain.usecase.category.DeleteCategoryUseCase
import com.example.myexpenditureapp.domain.usecase.category.GetCategoriesUseCase
import com.example.myexpenditureapp.domain.usecase.category.SaveCategoryUseCase
import com.example.myexpenditureapp.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.myexpenditureapp.domain.usecase.transaction.GetTransactionsUseCase
import com.example.myexpenditureapp.domain.usecase.transaction.SaveTransactionUseCase

object Graph {
    lateinit var database: AppDatabase
    lateinit var appContext: Context

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database.accountDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database.budgetDao(), database.categoryDao(), database.transactionDao())
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database, database.transactionDao(), database.accountDao())
    }

    val autoCategoryRuleRepository: com.example.myexpenditureapp.domain.repository.AutoCategoryRuleRepository by lazy {
        com.example.myexpenditureapp.data.repository.AutoCategoryRuleRepositoryImpl(database.autoCategoryRuleDao())
    }

    val savingGoalRepository: com.example.myexpenditureapp.domain.repository.SavingGoalRepository by lazy {
        com.example.myexpenditureapp.data.repository.SavingGoalRepositoryImpl(database.savingGoalDao())
    }

    val subscriptionRepository: com.example.myexpenditureapp.domain.repository.SubscriptionRepository by lazy {
        com.example.myexpenditureapp.data.repository.SubscriptionRepositoryImpl(database.subscriptionDao())
    }

    val getAccountsUseCase by lazy { GetAccountsUseCase(accountRepository) }
    val saveAccountUseCase by lazy { SaveAccountUseCase(accountRepository) }
    val deleteAccountUseCase by lazy { DeleteAccountUseCase(accountRepository) }

    val getCategoriesUseCase by lazy { GetCategoriesUseCase(categoryRepository) }
    val saveCategoryUseCase by lazy { SaveCategoryUseCase(categoryRepository) }
    val deleteCategoryUseCase by lazy { DeleteCategoryUseCase(categoryRepository) }

    val getTransactionsUseCase by lazy { GetTransactionsUseCase(transactionRepository) }
    val saveTransactionUseCase by lazy { SaveTransactionUseCase(transactionRepository) }
    val deleteTransactionUseCase by lazy { DeleteTransactionUseCase(transactionRepository) }

    fun provide(context: Context) {
        appContext = context.applicationContext
        database = AppDatabase.getDatabase(context)
    }
}
