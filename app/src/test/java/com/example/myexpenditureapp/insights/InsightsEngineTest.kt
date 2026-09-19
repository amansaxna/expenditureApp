package com.example.myexpenditureapp.insights

import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.insights.InsightType
import com.example.myexpenditureapp.domain.insights.InsightsEngine
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar

class InsightsEngineTest {

    private fun createTx(
        id: Long,
        amount: Double,
        type: String,
        merchant: String,
        categoryId: Long?
    ): Transaction {
        return Transaction(
            id = id,
            accountId = 1L,
            categoryId = categoryId,
            amount = BigDecimal(amount),
            merchant = merchant,
            type = type,
            timestamp = System.currentTimeMillis()
        )
    }

    @Test
    fun testSavingsRateHealthyInsight() {
        val txs = listOf(
            createTx(1L, 100000.0, "Income", "Salary", null),
            createTx(2L, 40000.0, "Expense", "Rent", 1L)
        )
        val categories = listOf(Category(id = 1L, name = "Housing"))

        val insights = InsightsEngine.generateInsights(txs, categories, emptyList())
        val savingsInsight = insights.find { it.id == "savings_rate_healthy" }

        assertNotNull("Should generate savings_rate_healthy insight", savingsInsight)
        assertEquals(InsightType.POSITIVE, savingsInsight!!.type)
        assertTrue(savingsInsight.title.contains("60%"))
    }

    @Test
    fun testSavingsRateNegativeDeficitInsight() {
        val txs = listOf(
            createTx(1L, 20000.0, "Income", "Freelance", null),
            createTx(2L, 30000.0, "Expense", "Shopping", 2L)
        )
        val categories = listOf(Category(id = 2L, name = "Shopping"))

        val insights = InsightsEngine.generateInsights(txs, categories, emptyList())
        val deficitInsight = insights.find { it.id == "savings_rate_negative" }

        assertNotNull("Should generate savings_rate_negative insight", deficitInsight)
        assertEquals(InsightType.WARNING, deficitInsight!!.type)
    }

    @Test
    fun testCategoryConcentrationRiskInsight() {
        val txs = listOf(
            createTx(1L, 6000.0, "Expense", "Swiggy", 1L),
            createTx(2L, 2000.0, "Expense", "Fuel", 2L),
            createTx(3L, 2000.0, "Expense", "Misc", 3L)
        )
        val categories = listOf(
            Category(id = 1L, name = "Food & Dining"),
            Category(id = 2L, name = "Transportation"),
            Category(id = 3L, name = "Other")
        )

        val insights = InsightsEngine.generateInsights(txs, categories, emptyList())
        val catInsight = insights.find { it.id == "top_category" }

        assertNotNull("Should generate top_category insight", catInsight)
        assertEquals(InsightType.WARNING, catInsight!!.type)
        assertEquals(1L, catInsight.drillDownCategoryId)
        assertTrue(catInsight.title.contains("Food & Dining"))
        assertTrue(catInsight.description.contains("60%"))
    }

    @Test
    fun testTopMerchantConcentrationInsight() {
        val txs = listOf(
            createTx(1L, 700.0, "Expense", "Swiggy", 1L),
            createTx(2L, 700.0, "Expense", "Swiggy", 1L),
            createTx(3L, 700.0, "Expense", "Swiggy", 1L),
            createTx(4L, 700.0, "Expense", "Swiggy", 1L),
            createTx(5L, 700.0, "Expense", "Swiggy", 1L),
            createTx(6L, 1000.0, "Expense", "Amazon", 2L)
        )
        val categories = listOf(
            Category(id = 1L, name = "Food"),
            Category(id = 2L, name = "Shopping")
        )

        val insights = InsightsEngine.generateInsights(txs, categories, emptyList())
        val merchantInsight = insights.find { it.id == "top_merchant" }

        assertNotNull("Should generate top_merchant insight", merchantInsight)
        assertEquals("SWIGGY", merchantInsight!!.drillDownMerchant)
        assertTrue(merchantInsight.description.contains("5 transactions"))
    }

    @Test
    fun testEmptyTransactionsReturnsEmptyList() {
        val insights = InsightsEngine.generateInsights(emptyList(), emptyList(), emptyList())
        assertTrue("No insights for empty transactions", insights.isEmpty())
    }
}
