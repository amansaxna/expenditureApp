package com.example.myexpenditureapp.overlay

import com.example.myexpenditureapp.data.entity.AutoCategoryRule
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.repository.AutoCategoryRuleRepository
import com.example.myexpenditureapp.domain.repository.TransactionRepository
import java.math.BigDecimal

class OverlayActionHandler(
    private val transactionRepository: TransactionRepository,
    private val ruleRepository: AutoCategoryRuleRepository
) {
    suspend fun confirmTransaction(
        transaction: Transaction,
        updatedMerchant: String,
        updatedAmount: BigDecimal,
        updatedType: String,
        categoryId: Long?,
        updatedMessage: String? = transaction.rawMessage,
        saveAsRule: Boolean
    ) {
        val updated = transaction.copy(
            merchant = updatedMerchant.trim().ifBlank { transaction.merchant },
            amount = updatedAmount,
            type = updatedType,
            categoryId = categoryId,
            rawMessage = updatedMessage,
            isReviewed = true
        )
        transactionRepository.saveTransaction(updated)

        if (saveAsRule && categoryId != null) {
            val ruleMerchant = updatedMerchant.trim().ifBlank { transaction.merchant }
            ruleRepository.saveRule(
                AutoCategoryRule(
                    keyword = ruleMerchant,
                    categoryId = categoryId
                )
            )
        }
    }

    suspend fun discardTransaction(transaction: Transaction) {
        transactionRepository.deleteTransaction(transaction)
    }

    fun shouldShowOverlay(isPermissionGranted: Boolean, isOverlayEnabledInSettings: Boolean): Boolean {
        return isPermissionGranted && isOverlayEnabledInSettings
    }
}
