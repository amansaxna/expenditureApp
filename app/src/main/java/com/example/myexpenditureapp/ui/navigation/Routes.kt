package com.example.myexpenditureapp.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object AccountList : Route

    @Serializable
    data class AccountEdit(val accountId: Long? = null) : Route

    @Serializable
    data object CategoryList : Route

    @Serializable
    data class CategoryEdit(val categoryId: Long? = null, val parentId: Long? = null) : Route

    @Serializable
    data object TransactionList : Route

    @Serializable
    data class TransactionEdit(val transactionId: Long? = null) : Route

    @Serializable
    data class TransactionReview(val transactionId: Long) : Route

    @Serializable
    data object BudgetList : Route

    @Serializable
    data class BudgetEdit(val budgetId: Long? = null) : Route

    @Serializable
    data object Analytics : Route
}
