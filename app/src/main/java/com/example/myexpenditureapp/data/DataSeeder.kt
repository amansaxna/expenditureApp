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
        }
    }
}
