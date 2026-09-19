package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.data.entity.AutoCategoryRule
import org.junit.Assert.*
import org.junit.Test

class AutoCategoryRuleTest {

    private fun matchMerchant(rules: List<AutoCategoryRule>, merchant: String): Long? {
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

    @Test
    fun testContainsMatching() {
        val rules = listOf(
            AutoCategoryRule(id = 1L, keyword = "SWIGGY", categoryId = 10L, matchType = "CONTAINS"),
            AutoCategoryRule(id = 2L, keyword = "UBER", categoryId = 20L, matchType = "CONTAINS")
        )

        assertEquals(10L, matchMerchant(rules, "SWIGGY INSTAMART BANGALORE"))
        assertEquals(20L, matchMerchant(rules, "UBER INDIA RIDES"))
        assertNull(matchMerchant(rules, "STARBUCKS COFFEE"))
    }

    @Test
    fun testExactMatching() {
        val rules = listOf(
            AutoCategoryRule(id = 1L, keyword = "NETFLIX", categoryId = 30L, matchType = "EXACT")
        )

        assertEquals(30L, matchMerchant(rules, "NETFLIX"))
        assertEquals(30L, matchMerchant(rules, "netflix"))
        assertNull(matchMerchant(rules, "NETFLIX ENTERTAINMENT"))
    }

    @Test
    fun testRegexMatching() {
        val rules = listOf(
            AutoCategoryRule(id = 1L, keyword = "AMAZON.*PAY", categoryId = 40L, matchType = "REGEX"),
            AutoCategoryRule(id = 2L, keyword = "^(ZOMATO|SWIGGY)$", categoryId = 50L, matchType = "REGEX")
        )

        assertEquals(40L, matchMerchant(rules, "AMAZON RETAIL PAY MUMBAI"))
        assertEquals(50L, matchMerchant(rules, "ZOMATO"))
        assertNull(matchMerchant(rules, "ZOMATO RESTAURANT"))
    }
}
