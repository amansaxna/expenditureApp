package com.example.myexpenditureapp.data.dao

import androidx.room.*
import com.example.myexpenditureapp.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<Category>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<Category>

    @Query("SELECT * FROM categories WHERE parentId IS NULL")
    fun getRootCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId")
    fun getSubcategories(parentId: Long): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}
