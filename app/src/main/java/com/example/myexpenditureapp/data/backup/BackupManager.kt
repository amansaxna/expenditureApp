package com.example.myexpenditureapp.data.backup

import android.content.Context
import android.net.Uri
import com.example.myexpenditureapp.data.AppDatabase
import com.example.myexpenditureapp.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.math.BigDecimal

object BackupManager {

    suspend fun exportBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val accounts = database.accountDao().getAllAccountsSync()
                val categories = database.categoryDao().getAllCategoriesSync()
                val transactions = database.transactionDao().getAllTransactionsSync()
                val budgets = database.budgetDao().getAllBudgetsSync()
                val rules = database.autoCategoryRuleDao().getAllRulesSync()
                val goals = database.savingGoalDao().getAllGoalsSync()

                val rootJson = JSONObject().apply {
                    put("version", 1)
                    put("exportTimestamp", System.currentTimeMillis())

                    // Accounts
                    val accountsArray = JSONArray()
                    accounts.forEach { acc ->
                        accountsArray.put(JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("type", acc.type)
                            put("balance", acc.balance.toPlainString())
                        })
                    }
                    put("accounts", accountsArray)

                    // Categories
                    val categoriesArray = JSONArray()
                    categories.forEach { cat ->
                        categoriesArray.put(JSONObject().apply {
                            put("id", cat.id)
                            put("name", cat.name)
                            put("icon", cat.icon ?: "")
                            put("parentId", cat.parentId ?: JSONObject.NULL)
                        })
                    }
                    put("categories", categoriesArray)

                    // Transactions
                    val txArray = JSONArray()
                    transactions.forEach { tx ->
                        txArray.put(JSONObject().apply {
                            put("id", tx.id)
                            put("accountId", tx.accountId)
                            put("toAccountId", tx.toAccountId ?: JSONObject.NULL)
                            put("categoryId", tx.categoryId ?: JSONObject.NULL)
                            put("amount", tx.amount.toPlainString())
                            put("merchant", tx.merchant)
                            put("timestamp", tx.timestamp)
                            put("type", tx.type)
                            put("smsId", tx.smsId ?: JSONObject.NULL)
                            put("isReviewed", tx.isReviewed)
                            val tagsArray = JSONArray()
                            tx.tags.forEach { tagsArray.put(it) }
                            put("tags", tagsArray)
                        })
                    }
                    put("transactions", txArray)

                    // Budgets
                    val budgetArray = JSONArray()
                    budgets.forEach { b ->
                        budgetArray.put(JSONObject().apply {
                            put("id", b.id)
                            put("categoryId", b.categoryId)
                            put("limitAmount", b.limitAmount.toPlainString())
                            put("period", b.period)
                            put("month", b.month)
                            put("year", b.year)
                        })
                    }
                    put("budgets", budgetArray)

                    // Rules
                    val rulesArray = JSONArray()
                    rules.forEach { r ->
                        rulesArray.put(JSONObject().apply {
                            put("id", r.id)
                            put("keyword", r.keyword)
                            put("categoryId", r.categoryId)
                            put("matchType", r.matchType)
                        })
                    }
                    put("rules", rulesArray)

                    // Goals
                    val goalsArray = JSONArray()
                    goals.forEach { g ->
                        goalsArray.put(JSONObject().apply {
                            put("id", g.id)
                            put("name", g.name)
                            put("targetAmount", g.targetAmount.toPlainString())
                            put("currentAmount", g.currentAmount.toPlainString())
                            put("targetDateEpochMs", g.targetDateEpochMs ?: JSONObject.NULL)
                            put("icon", g.icon)
                            put("colorHex", g.colorHex)
                            put("isCompleted", g.isCompleted)
                        })
                    }
                    put("goals", goalsArray)
                }

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(rootJson.toString(2))
                    }
                } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun importBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: return@withContext Result.failure(Exception("Failed to open input stream"))

                val rootJson = JSONObject(jsonString)

                // Parse Accounts
                val accounts = mutableListOf<Account>()
                val accountsArray = rootJson.optJSONArray("accounts") ?: JSONArray()
                for (i in 0 until accountsArray.length()) {
                    val obj = accountsArray.getJSONObject(i)
                    accounts.add(
                        Account(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            type = obj.getString("type"),
                            balance = BigDecimal(obj.getString("balance"))
                        )
                    )
                }

                // Parse Categories
                val categories = mutableListOf<Category>()
                val categoriesArray = rootJson.optJSONArray("categories") ?: JSONArray()
                for (i in 0 until categoriesArray.length()) {
                    val obj = categoriesArray.getJSONObject(i)
                    categories.add(
                        Category(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            parentId = if (obj.isNull("parentId")) null else obj.getLong("parentId"),
                            icon = if (obj.isNull("icon") || obj.optString("icon").isEmpty()) null else obj.getString("icon")
                        )
                    )
                }

                // Parse Transactions
                val transactions = mutableListOf<Transaction>()
                val txArray = rootJson.optJSONArray("transactions") ?: JSONArray()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val tagsList = mutableListOf<String>()
                    val tagsArray = obj.optJSONArray("tags")
                    if (tagsArray != null) {
                        for (j in 0 until tagsArray.length()) {
                            tagsList.add(tagsArray.getString(j))
                        }
                    }
                    transactions.add(
                        Transaction(
                            id = obj.getLong("id"),
                            accountId = obj.getLong("accountId"),
                            toAccountId = if (obj.isNull("toAccountId")) null else obj.getLong("toAccountId"),
                            categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                            amount = BigDecimal(obj.getString("amount")),
                            merchant = obj.getString("merchant"),
                            timestamp = obj.getLong("timestamp"),
                            type = obj.getString("type"),
                            smsId = if (obj.isNull("smsId")) null else obj.getString("smsId"),
                            isReviewed = obj.optBoolean("isReviewed", true),
                            tags = tagsList
                        )
                    )
                }

                // Parse Budgets
                val budgets = mutableListOf<Budget>()
                val budgetArray = rootJson.optJSONArray("budgets") ?: JSONArray()
                for (i in 0 until budgetArray.length()) {
                    val obj = budgetArray.getJSONObject(i)
                    budgets.add(
                        Budget(
                            id = obj.getLong("id"),
                            categoryId = obj.getLong("categoryId"),
                            limitAmount = BigDecimal(obj.getString("limitAmount")),
                            period = obj.optString("period", "Monthly"),
                            month = obj.getInt("month"),
                            year = obj.getInt("year")
                        )
                    )
                }

                // Parse Rules
                val rules = mutableListOf<AutoCategoryRule>()
                val rulesArray = rootJson.optJSONArray("rules") ?: JSONArray()
                for (i in 0 until rulesArray.length()) {
                    val obj = rulesArray.getJSONObject(i)
                    rules.add(
                        AutoCategoryRule(
                            id = obj.getLong("id"),
                            keyword = obj.getString("keyword"),
                            categoryId = obj.getLong("categoryId"),
                            matchType = obj.optString("matchType", "CONTAINS")
                        )
                    )
                }

                // Parse Goals
                val goals = mutableListOf<SavingGoal>()
                val goalsArray = rootJson.optJSONArray("goals") ?: JSONArray()
                for (i in 0 until goalsArray.length()) {
                    val obj = goalsArray.getJSONObject(i)
                    goals.add(
                        SavingGoal(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            targetAmount = BigDecimal(obj.getString("targetAmount")),
                            currentAmount = BigDecimal(obj.getString("currentAmount")),
                            targetDateEpochMs = if (obj.isNull("targetDateEpochMs")) null else obj.getLong("targetDateEpochMs"),
                            icon = obj.optString("icon", "🎯"),
                            colorHex = obj.optString("colorHex", "#3B82F6"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }

                // Restore to Database in transaction
                database.accountDao().insertAll(accounts)
                database.categoryDao().insertAll(categories)
                database.transactionDao().insertAll(transactions)
                database.budgetDao().insertAll(budgets)
                database.autoCategoryRuleDao().insertAll(rules)
                database.savingGoalDao().insertAll(goals)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
