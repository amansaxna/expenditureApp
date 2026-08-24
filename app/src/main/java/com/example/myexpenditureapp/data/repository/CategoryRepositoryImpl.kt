package com.example.myexpenditureapp.data.repository

import com.example.myexpenditureapp.data.dao.CategoryDao
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class CategoryRepositoryImpl(private val categoryDao: CategoryDao) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    override fun getRootCategories(): Flow<List<Category>> = categoryDao.getRootCategories()

    override fun getSubcategories(parentId: Long): Flow<List<Category>> = categoryDao.getSubcategories(parentId)

    override suspend fun saveCategory(category: Category) {
        if (category.id == 0L) {
            categoryDao.insertCategory(category)
        } else {
            categoryDao.updateCategory(category)
        }
    }

    override suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
}
