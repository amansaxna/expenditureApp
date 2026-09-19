package com.example.myexpenditureapp.domain.repository

import com.example.myexpenditureapp.data.entity.AutoCategoryRule
import kotlinx.coroutines.flow.Flow

interface AutoCategoryRuleRepository {
    fun getAllRules(): Flow<List<AutoCategoryRule>>
    suspend fun getAllRulesSync(): List<AutoCategoryRule>
    suspend fun getRuleById(id: Long): AutoCategoryRule?
    suspend fun getMatchingCategoryId(merchant: String): Long?
    suspend fun saveRule(rule: AutoCategoryRule): Long
    suspend fun deleteRule(rule: AutoCategoryRule)
    suspend fun deleteRuleById(id: Long)
}
