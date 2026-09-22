package com.example.myexpenditureapp.data

import com.example.myexpenditureapp.data.entity.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DataSeeder handles initial category setup for the application on a fresh install.
 * It ONLY seeds default categories if no categories exist.
 * It NEVER injects fake accounts or dummy transactions.
 */
object DataSeeder {
    suspend fun seedData(database: AppDatabase) {
        withContext(Dispatchers.IO) {
            val categoryDao = database.categoryDao()

            // Seed Default Taxonomy only if categories table is completely empty
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
                categoryDao.insertCategory(Category(name = "Miscellaneous", icon = "📦"))

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
            } else {
                val hasMisc = existingCategories.any { it.name.equals("Miscellaneous", ignoreCase = true) || it.name.equals("Misc", ignoreCase = true) }
                if (!hasMisc) {
                    categoryDao.insertCategory(Category(name = "Miscellaneous", icon = "📦"))
                }
            }
        }
    }

    suspend fun seedDemoData(database: AppDatabase) {
        withContext(Dispatchers.IO) {
            // First ensure categories exist
            seedData(database)

            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()
            val transactionDao = database.transactionDao()
            val budgetDao = database.budgetDao()
            val savingGoalDao = database.savingGoalDao()
            val subscriptionDao = database.subscriptionDao()

            val categories = categoryDao.getAllCategoriesList()
            val foodCat = categories.find { it.name == "Restaurants" || it.name == "Food & Dining" }
            val transportCat = categories.find { it.name == "Transportation" || it.name == "Fuel" }
            val groceryCat = categories.find { it.name == "Groceries" }
            val salaryCat = categories.find { it.name == "Salary" || it.name == "Finance" }
            val entertainmentCat = categories.find { it.name == "Entertainment" }

            // 1. Demo Accounts
            var existingAccounts = accountDao.getAllAccountsSync()
            if (existingAccounts.isEmpty()) {
                accountDao.insertAccount(com.example.myexpenditureapp.data.entity.Account(name = "Salary Account (HDFC)", type = "Bank", balance = java.math.BigDecimal("78500.00")))
                accountDao.insertAccount(com.example.myexpenditureapp.data.entity.Account(name = "Daily UPI Wallet", type = "Wallet", balance = java.math.BigDecimal("4200.00")))
                accountDao.insertAccount(com.example.myexpenditureapp.data.entity.Account(name = "Credit Card", type = "Credit Card", balance = java.math.BigDecimal("-8400.00")))
                existingAccounts = accountDao.getAllAccountsSync()
            }

            val primaryAccId = existingAccounts.firstOrNull()?.id ?: 1L
            val walletAccId = existingAccounts.getOrNull(1)?.id ?: primaryAccId

            // 2. Demo Saving Goals
            savingGoalDao.insertGoal(
                com.example.myexpenditureapp.data.entity.SavingGoal(
                    name = "Emergency Reserve",
                    targetAmount = java.math.BigDecimal("100000.00"),
                    currentAmount = java.math.BigDecimal("65000.00"),
                    icon = "🛡️"
                )
            )
            savingGoalDao.insertGoal(
                com.example.myexpenditureapp.data.entity.SavingGoal(
                    name = "Japan Trip 2027",
                    targetAmount = java.math.BigDecimal("150000.00"),
                    currentAmount = java.math.BigDecimal("45000.00"),
                    icon = "✈️"
                )
            )

            // 3. Demo Budgets
            val cal = java.util.Calendar.getInstance()
            val currentMonth = cal.get(java.util.Calendar.MONTH)
            val currentYear = cal.get(java.util.Calendar.YEAR)

            foodCat?.id?.let {
                budgetDao.insertBudget(
                    com.example.myexpenditureapp.data.entity.Budget(
                        categoryId = it,
                        limitAmount = java.math.BigDecimal("12000.00"),
                        period = "MONTHLY",
                        month = currentMonth,
                        year = currentYear
                    )
                )
            }
            transportCat?.id?.let {
                budgetDao.insertBudget(
                    com.example.myexpenditureapp.data.entity.Budget(
                        categoryId = it,
                        limitAmount = java.math.BigDecimal("5000.00"),
                        period = "MONTHLY",
                        month = currentMonth,
                        year = currentYear
                    )
                )
            }

            // 4. Demo Subscriptions
            subscriptionDao.insert(
                com.example.myexpenditureapp.data.entity.Subscription(
                    name = "Netflix Standard",
                    amount = java.math.BigDecimal("649.00"),
                    billingCycle = "Monthly",
                    dueDayOfMonth = 15,
                    categoryId = entertainmentCat?.id,
                    accountId = primaryAccId,
                    autoDetectEnabled = true,
                    isActive = true
                )
            )
            subscriptionDao.insert(
                com.example.myexpenditureapp.data.entity.Subscription(
                    name = "Spotify Premium",
                    amount = java.math.BigDecimal("119.00"),
                    billingCycle = "Monthly",
                    dueDayOfMonth = 22,
                    categoryId = entertainmentCat?.id,
                    accountId = walletAccId,
                    autoDetectEnabled = true,
                    isActive = true
                )
            )

            // 5. Demo Past Reviewed Transactions
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            val reviewedTransactions = listOf(
                com.example.myexpenditureapp.data.entity.Transaction(
                    accountId = primaryAccId,
                    categoryId = salaryCat?.id,
                    amount = java.math.BigDecimal("95000.00"),
                    merchant = "TECH CORP SALARY",
                    type = "Income",
                    timestamp = now - 10 * dayMillis,
                    isReviewed = true,
                    smsId = "demo_salary"
                ),
                com.example.myexpenditureapp.data.entity.Transaction(
                    accountId = primaryAccId,
                    categoryId = groceryCat?.id,
                    amount = java.math.BigDecimal("3450.00"),
                    merchant = "NATURE'S BASKET",
                    type = "Expense",
                    timestamp = now - 5 * dayMillis,
                    isReviewed = true,
                    smsId = "demo_grocery"
                ),
                com.example.myexpenditureapp.data.entity.Transaction(
                    accountId = walletAccId,
                    categoryId = foodCat?.id,
                    amount = java.math.BigDecimal("820.00"),
                    merchant = "PIZZA BAKERY",
                    type = "Expense",
                    timestamp = now - 2 * dayMillis,
                    isReviewed = true,
                    smsId = "demo_pizza"
                )
            )
            transactionDao.insertAll(reviewedTransactions)

            // 6. Demo Pending / Unreviewed Transactions (for Smart Inbox & Review Layout)
            val demoPendingTransactions = listOf(
                com.example.myexpenditureapp.data.entity.Transaction(
                    accountId = walletAccId,
                    categoryId = null,
                    amount = java.math.BigDecimal("450.00"),
                    merchant = "STARBUCKS COFFEE",
                    type = "Expense",
                    timestamp = now - 3600000L,
                    isReviewed = false,
                    smsId = "demo_pending_starbucks",
                    rawMessage = "Spent Rs. 450.00 on your HDFC Bank Credit Card ending 4321 at STARBUCKS COFFEE on 20-Sep-2026. Avl Bal: Rs. 78,050.00"
                ),
                com.example.myexpenditureapp.data.entity.Transaction(
                    accountId = primaryAccId,
                    categoryId = null,
                    amount = java.math.BigDecimal("320.00"),
                    merchant = "SWIGGY",
                    type = "Expense",
                    timestamp = now - 7200000L,
                    isReviewed = false,
                    smsId = "demo_pending_swiggy",
                    rawMessage = "Paid Rs. 320.00 to SWIGGY via UPI Ref 9876543210 from a/c linked to VPA swiggy@icici."
                ),
                com.example.myexpenditureapp.data.entity.Transaction(
                    accountId = walletAccId,
                    categoryId = null,
                    amount = java.math.BigDecimal("180.00"),
                    merchant = "UBER RIDES",
                    type = "Expense",
                    timestamp = now - 14400000L,
                    isReviewed = false,
                    smsId = "demo_pending_uber",
                    rawMessage = "Payment to UBER RIDES of Rs. 180.00 successful from your Bank Account ending 1234."
                )
            )
            transactionDao.insertAll(demoPendingTransactions)
        }
    }
}
