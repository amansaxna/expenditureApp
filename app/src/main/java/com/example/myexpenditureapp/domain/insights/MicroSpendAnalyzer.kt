package com.example.myexpenditureapp.domain.insights

import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import java.math.BigDecimal
import java.math.RoundingMode

data class CategoryMicroBreakdown(
    val categoryId: Long?,
    val categoryName: String,
    val categoryIcon: String,
    val microTotal: BigDecimal,
    val txCount: Int,
    val percentOfMicroTotal: Int
)

data class MicroSpendSummary(
    val effectiveThreshold: BigDecimal,
    val totalMicroSpend: BigDecimal,
    val totalMacroSpend: BigDecimal,
    val microTxCount: Int,
    val macroTxCount: Int,
    val microSpendPercentage: Int, // 0..100
    val topMicroCategories: List<CategoryMicroBreakdown>,
    val allMacroCategories: List<CategoryMicroBreakdown>,
    val topMicroCategoryName: String,
    val topMicroCategoryIcon: String,
    val microTransactions: List<Transaction>,
    val macroTransactions: List<Transaction>
)

object MicroSpendAnalyzer {

    fun analyze(
        transactions: List<Transaction>,
        categories: List<Category>,
        safeDailyAllowance: BigDecimal = BigDecimal.ZERO,
        customThresholdOverride: BigDecimal? = null
    ): MicroSpendSummary {
        val expenses = transactions.filter { it.type == "Expense" }
        
        // Effective threshold: User Custom Override > (Safe Daily Allowance / 2) > Default ₹250
        val effectiveThreshold = customThresholdOverride ?: if (safeDailyAllowance > BigDecimal.ZERO) {
            safeDailyAllowance.divide(BigDecimal(2), 0, RoundingMode.HALF_UP).coerceAtLeast(BigDecimal("100"))
        } else {
            BigDecimal("250")
        }

        if (expenses.isEmpty()) {
            return MicroSpendSummary(
                effectiveThreshold = effectiveThreshold,
                totalMicroSpend = BigDecimal.ZERO,
                totalMacroSpend = BigDecimal.ZERO,
                microTxCount = 0,
                macroTxCount = 0,
                microSpendPercentage = 0,
                topMicroCategories = emptyList(),
                allMacroCategories = emptyList(),
                topMicroCategoryName = "None",
                topMicroCategoryIcon = "💳",
                microTransactions = emptyList(),
                macroTransactions = emptyList()
            )
        }

        val microList = mutableListOf<Transaction>()
        val macroList = mutableListOf<Transaction>()

        expenses.forEach { tx ->
            val isMicro = when (tx.isMicroOverride) {
                true -> true
                false -> false
                null -> tx.amount <= effectiveThreshold
            }
            if (isMicro) microList.add(tx) else macroList.add(tx)
        }

        val totalMicro = microList.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val totalMacro = macroList.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val grandTotal = totalMicro.add(totalMacro)

        val microPercent = if (grandTotal > BigDecimal.ZERO) {
            totalMicro.divide(grandTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
        } else 0

        val categoryMap = categories.associateBy { it.id }
        val categoryGrouped = microList.groupBy { it.categoryId }
        
        val topCategories = categoryGrouped.map { (catId, txs) ->
            val catSum = txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            val catObj = categoryMap[catId]
            val pct = if (totalMicro > BigDecimal.ZERO) {
                catSum.divide(totalMicro, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
            } else 0

            CategoryMicroBreakdown(
                categoryId = catId,
                categoryName = catObj?.name ?: "General",
                categoryIcon = catObj?.icon ?: "💳",
                microTotal = catSum,
                txCount = txs.size,
                percentOfMicroTotal = pct
            )
        }.sortedByDescending { it.microTotal }

        val macroCategoryGrouped = macroList.groupBy { it.categoryId }
        val allMacroCats = macroCategoryGrouped.map { (catId, txs) ->
            val catSum = txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            val catObj = categoryMap[catId]
            val pct = if (totalMacro > BigDecimal.ZERO) {
                catSum.divide(totalMacro, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
            } else 0

            CategoryMicroBreakdown(
                categoryId = catId,
                categoryName = catObj?.name ?: "General",
                categoryIcon = catObj?.icon ?: "💳",
                microTotal = catSum,
                txCount = txs.size,
                percentOfMicroTotal = pct
            )
        }.sortedByDescending { it.microTotal }

        val topCategoryName = topCategories.firstOrNull()?.categoryName ?: "General"
        val topCategoryIcon = topCategories.firstOrNull()?.categoryIcon ?: "☕"

        return MicroSpendSummary(
            effectiveThreshold = effectiveThreshold,
            totalMicroSpend = totalMicro,
            totalMacroSpend = totalMacro,
            microTxCount = microList.size,
            macroTxCount = macroList.size,
            microSpendPercentage = microPercent,
            topMicroCategories = topCategories,
            allMacroCategories = allMacroCats,
            topMicroCategoryName = topCategoryName,
            topMicroCategoryIcon = topCategoryIcon,
            microTransactions = microList.sortedByDescending { it.amount },
            macroTransactions = macroList.sortedByDescending { it.amount }
        )
    }
}
