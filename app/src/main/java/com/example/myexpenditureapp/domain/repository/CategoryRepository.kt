package com.example.myexpenditureapp.domain.repository

import com.example.myexpenditureapp.data.entity.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getRootCategories(): Flow<List<Category>>
    fun getSubcategories(parentId: Long): Flow<List<Category>>
    suspend fun saveCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}
