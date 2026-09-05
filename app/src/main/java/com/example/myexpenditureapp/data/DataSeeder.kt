package com.example.myexpenditureapp.data

import com.example.myexpenditureapp.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.*

/**
 * DataSeeder handles initial data setup for the application.
 * On a fresh install, it seeds the default Category hierarchy with emojis.
 */
object DataSeeder {
    suspend fun seedData(database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val categoryDao = database.categoryDao()
            val accountDao = database.accountDao()
            val transactionDao = database.transactionDao()

            // 1. Seed Categories if empty
            val existingCategories = categoryDao.getAllCategoriesList()
            if (existingCategories.isEmpty()) {
                // Root Categories
                categoryDao.insertCategory(Category(name = "Housing", icon = "🏠"))
                categoryDao.insertCategory(Category(name = "Food & Dining", icon = "🍽️"))
                categoryDao.insertCategory(Category(name = "Transportation", icon = "🚗"))
                categoryDao.insertCategory(Category(name = "Personal Care", icon = "🧘"))
                categoryDao.insertCategory(Category(name = "Entertainment", icon = "🎬"))
                categoryDao.insertCategory(Category(name = "Shopping", icon = "🛒"))
                categoryDao.insertCategory(Category(name = "Finance", icon = "💰"))
                categoryDao.insertCategory(Category(name = "Education", icon = "📚"))

                // Fetch categories to get IDs for subcategories
                val categories = categoryDao.getAllCategoriesList()
                val housingId = categories.find { it.name == "Housing" }?.id
                val foodId = categories.find { it.name == "Food & Dining" }?.id
                val transportId = categories.find { it.name == "Transportation" }?.id
                val personalId = categories.find { it.name == "Personal Care" }?.id
                val financeId = categories.find { it.name == "Finance" }?.id

                // Subcategories
                housingId?.let {
                    categoryDao.insertCategory(Category(name = "Rent", parentId = it, icon = "🏠"))
                    categoryDao.insertCategory(Category(name = "Utilities", parentId = it, icon = "⚡"))
                }
                foodId?.let {
                    categoryDao.insertCategory(Category(name = "Groceries", parentId = it, icon = "🛒"))
                    categoryDao.insertCategory(Category(name = "Restaurants", parentId = it, icon = "🍔"))
                    categoryDao.insertCategory(Category(name = "Cafes", parentId = it, icon = "☕"))
                }
                transportId?.let {
                    categoryDao.insertCategory(Category(name = "Fuel", parentId = it, icon = "⛽"))
                    categoryDao.insertCategory(Category(name = "Public Transit", parentId = it, icon = "🚌"))
                }
                financeId?.let {
                    categoryDao.insertCategory(Category(name = "Salary", parentId = it, icon = "💰"))
                    categoryDao.insertCategory(Category(name = "Dividends", parentId = it, icon = "📈"))
                }
                personalId?.let { categoryDao.insertCategory(Category(name = "Gym", parentId = it, icon = "🏋️‍♂️")) }
            }

            // 2. Seed Accounts if empty
            val accounts = accountDao.getAllAccounts().first()
            if (accounts.isEmpty()) {
                accountDao.insertAccount(Account(name = "SBI Bank", type = "Bank", balance = BigDecimal("50000")))
                accountDao.insertAccount(Account(name = "Paytm Wallet", type = "Wallet", balance = BigDecimal("1500")))
                accountDao.insertAccount(Account(name = "Cash", type = "Cash", balance = BigDecimal("2000")))
            }

            // 3. Seed Transactions for last 3 months if empty
            val existingTransactions = transactionDao.getAllTransactions().first()
            if (existingTransactions.isEmpty()) {
                val allAccounts = accountDao.getAllAccounts().first()
                val allCategories = categoryDao.getAllCategoriesList()
                
                if (allAccounts.isNotEmpty() && allCategories.isNotEmpty()) {
                    val sbi = allAccounts.find { it.name == "SBI Bank" }?.id ?: allAccounts.first().id
                    val cash = allAccounts.find { it.name == "Cash" }?.id ?: allAccounts.first().id
                    
                    val groceries = allCategories.find { it.name == "Groceries" }?.id
                    val rent = allCategories.find { it.name == "Rent" }?.id
                    val fuel = allCategories.find { it.name == "Fuel" }?.id
                    val salary = allCategories.find { it.name == "Salary" }?.id
                    val gym = allCategories.find { it.name == "Gym" }?.id
                    val dining = allCategories.find { it.name == "Restaurants" }?.id

                    // Helper to add transaction
                    suspend fun addTx(amount: String, merchant: String, type: String, catId: Long?, accId: Long, monthsAgo: Int, day: Int) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.MONTH, -monthsAgo)
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
                        transactionDao.insertTransaction(Transaction(
                            accountId = accId,
                            categoryId = catId,
                            amount = BigDecimal(amount),
                            merchant = merchant,
                            type = type,
                            timestamp = cal.timeInMillis,
                            isReviewed = true
                        ))
                    }

                    // Seed data for last 3 months
                    for (m in 0..2) {
                        addTx("45000", "Monthly Salary", "Income", salary, sbi, m, 1)
                        addTx("15000", "House Rent", "Expense", rent, sbi, m, 5)
                        addTx("1200", "Supermarket", "Expense", groceries, cash, m, 10)
                        addTx("800", "Petrol Pump", "Expense", fuel, sbi, m, 12)
                        addTx("2500", "Cult Gym Membership", "Expense", gym, sbi, m, 2)
                        addTx("1500", "Dinner at Social", "Expense", dining, sbi, m, 20)
                        addTx("500", "Blue Tokai Coffee", "Expense", dining, cash, m, 15)
                        addTx("3000", "Electricity Bill", "Expense", null, sbi, m, 18)
                        
                        // Weekly expenses
                        for (w in 1..4) {
                            addTx("450", "Weekly Groceries", "Expense", groceries, cash, m, w * 7)
                            addTx("600", "Uber Ride", "Expense", fuel, sbi, m, w * 7 - 3)
                            addTx("120", "Milk & Snacks", "Expense", groceries, cash, m, w * 7 - 5)
                        }
                    }
                }
            }
        }
    }
}

