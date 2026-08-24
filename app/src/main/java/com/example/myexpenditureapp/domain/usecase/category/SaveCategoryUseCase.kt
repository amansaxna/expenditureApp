package com.example.myexpenditureapp.domain.usecase.category

import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.domain.repository.CategoryRepository

class SaveCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(category: Category) = repository.saveCategory(category)
}
