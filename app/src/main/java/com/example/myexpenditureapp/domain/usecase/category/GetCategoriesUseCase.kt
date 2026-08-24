package com.example.myexpenditureapp.domain.usecase.category

import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(private val repository: CategoryRepository) {
    fun getAll(): Flow<List<Category>> = repository.getAllCategories()
    fun getRoot(): Flow<List<Category>> = repository.getRootCategories()
    fun getSub(parentId: Long): Flow<List<Category>> = repository.getSubcategories(parentId)
}
