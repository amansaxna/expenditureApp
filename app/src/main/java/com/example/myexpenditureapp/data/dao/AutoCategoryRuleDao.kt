package com.example.myexpenditureapp.data.dao

import androidx.room.*
import com.example.myexpenditureapp.data.entity.AutoCategoryRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoCategoryRuleDao {
    @Query("SELECT * FROM auto_category_rules ORDER BY keyword ASC")
    fun getAllRules(): Flow<List<AutoCategoryRule>>

    @Query("SELECT * FROM auto_category_rules")
    suspend fun getAllRulesSync(): List<AutoCategoryRule>

    @Query("SELECT * FROM auto_category_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): AutoCategoryRule?

    @Query("SELECT * FROM auto_category_rules WHERE keyword = :keyword LIMIT 1")
    suspend fun getRuleByKeyword(keyword: String): AutoCategoryRule?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRule(rule: AutoCategoryRule): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rules: List<AutoCategoryRule>)

    @Update
    suspend fun updateRule(rule: AutoCategoryRule)

    @Delete
    suspend fun deleteRule(rule: AutoCategoryRule)

    @Query("DELETE FROM auto_category_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM auto_category_rules")
    suspend fun deleteAllRules()
}
