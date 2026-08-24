package com.example.myexpenditureapp.data

import com.example.myexpenditureapp.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.*

object DataSeeder {
    suspend fun seedData(database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()
            val transactionDao = database.transactionDao()
            val budgetDao = database.budgetDao()

            // 1. Seed Accounts if empty
            val existingAccounts = accountDao.getAllAccounts().first()
            if (existingAccounts.isEmpty()) {
                accountDao.insertAccount(Account(name = "Main Bank", type = "Bank", balance = 150000.0))
                accountDao.insertAccount(Account(name = "Cash", type = "Wallet", balance = 5000.0))
                accountDao.insertAccount(Account(name = "Credit Card", type = "Credit Card", balance = -12000.0))
            }

            // 2. Seed Categories if empty
            val existingCategories = categoryDao.getAllCategories().first()
            if (existingCategories.isEmpty()) {
                categoryDao.insertCategory(Category(name = "Income", icon = "payments"))
                categoryDao.insertCategory(Category(name = "Food", icon = "restaurant"))
                categoryDao.insertCategory(Category(name = "Entertainment", icon = "movie"))
                categoryDao.insertCategory(Category(name = "Utilities", icon = "home"))
                categoryDao.insertCategory(Category(name = "Transportation", icon = "directions_car"))

                // Fetch categories to get IDs for subcategories and budgets
                val categories = categoryDao.getAllCategories().first()
                val incomeId = categories.find { it.name == "Income" }?.id
                val foodId = categories.find { it.name == "Food" }?.id
                val entertainmentId = categories.find { it.name == "Entertainment" }?.id
                val utilitiesId = categories.find { it.name == "Utilities" }?.id
                val transportationId = categories.find { it.name == "Transportation" }?.id

                // Subcategories
                incomeId?.let {
                    categoryDao.insertCategory(Category(name = "Salary", parentId = it))
                    categoryDao.insertCategory(Category(name = "Bonus", parentId = it))
                }
                foodId?.let {
                    categoryDao.insertCategory(Category(name = "Groceries", parentId = it))
                    categoryDao.insertCategory(Category(name = "Dining Out", parentId = it))
                }
                entertainmentId?.let {
                    categoryDao.insertCategory(Category(name = "Movies", parentId = it))
                    categoryDao.insertCategory(Category(name = "Games", parentId = it))
                }
                utilitiesId?.let {
                    categoryDao.insertCategory(Category(name = "Rent", parentId = it))
                    categoryDao.insertCategory(Category(name = "Electricity", parentId = it))
                }
                transportationId?.let {
                    categoryDao.insertCategory(Category(name = "Fuel", parentId = it))
                }

                // Seed Budgets
                if (foodId != null) {
                    budgetDao.insertBudget(Budget(categoryId = foodId, limitAmount = 15000.0, period = "Monthly"))
                }
                if (entertainmentId != null) {
                    budgetDao.insertBudget(Budget(categoryId = entertainmentId, limitAmount = 5000.0, period = "Monthly"))
                }
            }

            // 3. Seed Transactions if empty
            val existingTransactions = transactionDao.getAllTransactions().first()
            if (existingTransactions.isEmpty()) {
                val accounts = accountDao.getAllAccounts().first()
                val mainBankId = accounts.find { it.name == "Main Bank" }?.id ?: accounts.firstOrNull()?.id
                val cashId = accounts.find { it.name == "Cash" }?.id ?: accounts.lastOrNull()?.id
                val creditCardId = accounts.find { it.name == "Credit Card" }?.id ?: mainBankId

                val allCategories = categoryDao.getAllCategories().first()
                val salaryId = allCategories.find { it.name == "Salary" }?.id
                val groceriesId = allCategories.find { it.name == "Groceries" }?.id
                val diningId = allCategories.find { it.name == "Dining Out" }?.id
                val moviesId = allCategories.find { it.name == "Movies" }?.id
                val rentId = allCategories.find { it.name == "Rent" }?.id
                val fuelId = allCategories.find { it.name == "Fuel" }?.id

                if (mainBankId != null) {
                    // Seed Transactions (Spread over last 3 months)
                    for (monthOffset in -2..0) {
                        val monthCalendar = Calendar.getInstance()
                        monthCalendar.add(Calendar.MONTH, monthOffset)
                        
                        // Income: Salary on 1st
                        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
                        transactionDao.insertTransaction(Transaction(
                            accountId = mainBankId,
                            categoryId = salaryId,
                            amount = 85000.0,
                            merchant = "Company Inc",
                            timestamp = monthCalendar.timeInMillis,
                            type = "Income"
                        ))
                        
                        // Expense: Rent on 2nd
                        monthCalendar.set(Calendar.DAY_OF_MONTH, 2)
                        transactionDao.insertTransaction(Transaction(
                            accountId = mainBankId,
                            categoryId = rentId,
                            amount = 25000.0,
                            merchant = "Landlord",
                            timestamp = monthCalendar.timeInMillis,
                            type = "Expense"
                        ))

                        // Expense: Groceries on 5th
                        monthCalendar.set(Calendar.DAY_OF_MONTH, 5)
                        transactionDao.insertTransaction(Transaction(
                            accountId = mainBankId,
                            categoryId = groceriesId,
                            amount = 4500.0,
                            merchant = "Reliance Fresh",
                            timestamp = monthCalendar.timeInMillis,
                            type = "Expense"
                        ))

                        // Expense: Dining Out on 10th
                        if (cashId != null) {
                            monthCalendar.set(Calendar.DAY_OF_MONTH, 10)
                            transactionDao.insertTransaction(Transaction(
                                accountId = cashId,
                                categoryId = diningId,
                                amount = 1200.0,
                                merchant = "Local Restaurant",
                                timestamp = monthCalendar.timeInMillis,
                                type = "Expense"
                            ))

                            // Transfer: Bank to Cash on 12th
                            monthCalendar.set(Calendar.DAY_OF_MONTH, 12)
                            transactionDao.insertTransaction(Transaction(
                                accountId = mainBankId,
                                toAccountId = cashId,
                                categoryId = null,
                                amount = 5000.0,
                                merchant = "ATM Withdrawal",
                                timestamp = monthCalendar.timeInMillis,
                                type = "Transfer"
                            ))
                        }

                        // Expense: Fuel on 15th
                        if (creditCardId != null) {
                            monthCalendar.set(Calendar.DAY_OF_MONTH, 15)
                            transactionDao.insertTransaction(Transaction(
                                accountId = creditCardId,
                                categoryId = fuelId,
                                amount = 3000.0,
                                merchant = "Indian Oil",
                                timestamp = monthCalendar.timeInMillis,
                                type = "Expense"
                            ))
                        }

                        // Expense: Movies on 20th
                        monthCalendar.set(Calendar.DAY_OF_MONTH, 20)
                        transactionDao.insertTransaction(Transaction(
                            accountId = mainBankId,
                            categoryId = moviesId,
                            amount = 800.0,
                            merchant = "PVR Cinemas",
                            timestamp = monthCalendar.timeInMillis,
                            type = "Expense"
                        ))
                    }
                }
            }
        }
    }
}
