package com.example.myexpenditureapp.data.repository

import com.example.myexpenditureapp.data.dao.AutoCategoryRuleDao
import com.example.myexpenditureapp.data.entity.AutoCategoryRule
import com.example.myexpenditureapp.domain.repository.AutoCategoryRuleRepository
import kotlinx.coroutines.flow.Flow

class AutoCategoryRuleRepositoryImpl(
    private val ruleDao: AutoCategoryRuleDao
) : AutoCategoryRuleRepository {

    override fun getAllRules(): Flow<List<AutoCategoryRule>> = ruleDao.getAllRules()

    override suspend fun getAllRulesSync(): List<AutoCategoryRule> = ruleDao.getAllRulesSync()

    override suspend fun getRuleById(id: Long): AutoCategoryRule? = ruleDao.getRuleById(id)

    override suspend fun getMatchingCategoryId(merchant: String): Long? {
        val rules = ruleDao.getAllRulesSync()
        val normalizedMerchant = merchant.trim().uppercase()
        
        for (rule in rules) {
            val normalizedKeyword = rule.keyword.trim().uppercase()
            when (rule.matchType) {
                "EXACT" -> {
                    if (normalizedMerchant.equals(normalizedKeyword, ignoreCase = true)) {
                        return rule.categoryId
                    }
                }
                "REGEX" -> {
                    try {
                        val regex = Regex(rule.keyword, RegexOption.IGNORE_CASE)
                        if (regex.containsMatchIn(merchant)) {
                            return rule.categoryId
                        }
                    } catch (e: Exception) {
                        // ignore invalid regex
                    }
                }
                else -> { // "CONTAINS"
                    if (normalizedMerchant.contains(normalizedKeyword, ignoreCase = true)) {
                        return rule.categoryId
                    }
                }
            }
        }
        return null
    }

    override suspend fun saveRule(rule: AutoCategoryRule): Long {
        return if (rule.id != 0L) {
            ruleDao.updateRule(rule)
            rule.id
        } else {
            ruleDao.insertRule(rule)
        }
    }

    override suspend fun deleteRule(rule: AutoCategoryRule) {
        ruleDao.deleteRule(rule)
    }

    override suspend fun deleteRuleById(id: Long) {
        ruleDao.deleteRuleById(id)
    }
}
