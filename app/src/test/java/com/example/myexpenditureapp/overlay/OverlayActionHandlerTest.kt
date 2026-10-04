package com.example.myexpenditureapp.overlay

import com.example.myexpenditureapp.data.entity.AutoCategoryRule
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.repository.AutoCategoryRuleRepository
import com.example.myexpenditureapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FakeTransactionRepository : TransactionRepository {
    val savedTransactions = mutableListOf<Transaction>()
    val deletedTransactions = mutableListOf<Transaction>()

    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(savedTransactions)
    override fun getFilteredTransactions(
        accountId: Long?,
        categoryId: Long?,
        type: String?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): Flow<List<Transaction>> = flowOf(savedTransactions)
    override fun getUnreviewedTransactions(): Flow<List<Transaction>> = flowOf(savedTransactions.filter { !it.isReviewed })
    override suspend fun markAsReviewed(id: Long) {
        val index = savedTransactions.indexOfFirst { it.id == id }
        if (index != -1) {
            savedTransactions[index] = savedTransactions[index].copy(isReviewed = true)
        }
    }
    override suspend fun markAllAsReviewed() {}
    override suspend fun saveTransaction(transaction: Transaction) {
        val index = savedTransactions.indexOfFirst { it.id == transaction.id && it.id != 0L }
        if (index != -1) {
            savedTransactions[index] = transaction
        } else {
            savedTransactions.add(transaction)
        }
    }
    override suspend fun deleteTransaction(transaction: Transaction) {
        savedTransactions.remove(transaction)
        deletedTransactions.add(transaction)
    }
    override suspend fun getTransactionById(id: Long): Transaction? = savedTransactions.find { it.id == id }
    override suspend fun existsBySmsId(smsId: String): Boolean = savedTransactions.any { it.smsId == smsId }
    override suspend fun deleteAllUnreviewedTransactions() {}
    override suspend fun deleteAllTransactions() {}
}

class FakeAutoCategoryRuleRepository : AutoCategoryRuleRepository {
    val savedRules = mutableListOf<AutoCategoryRule>()

    override fun getAllRules(): Flow<List<AutoCategoryRule>> = flowOf(savedRules)
    override suspend fun getAllRulesSync(): List<AutoCategoryRule> = savedRules
    override suspend fun getRuleById(id: Long): AutoCategoryRule? = savedRules.find { it.id == id }
    override suspend fun getMatchingCategoryId(merchant: String): Long? =
        savedRules.find { merchant.contains(it.keyword, ignoreCase = true) }?.categoryId
    override suspend fun saveRule(rule: AutoCategoryRule): Long {
        savedRules.add(rule)
        return savedRules.size.toLong()
    }
    override suspend fun deleteRule(rule: AutoCategoryRule) { savedRules.remove(rule) }
    override suspend fun deleteRuleById(id: Long) { savedRules.removeIf { it.id == id } }
}

class OverlayActionHandlerTest {

    private lateinit var transactionRepo: FakeTransactionRepository
    private lateinit var ruleRepo: FakeAutoCategoryRuleRepository
    private lateinit var actionHandler: OverlayActionHandler

    @Before
    fun setUp() {
        transactionRepo = FakeTransactionRepository()
        ruleRepo = FakeAutoCategoryRuleRepository()
        actionHandler = OverlayActionHandler(transactionRepo, ruleRepo)
    }

    @Test
    fun testConfirmTransactionUpdatesDetailsAndMarksReviewed() = runTest {
        val initialTx = Transaction(
            id = 100L,
            accountId = 1L,
            categoryId = null,
            amount = BigDecimal("500"),
            merchant = "Swiggy",
            timestamp = 1000L,
            type = "Expense",
            isReviewed = false
        )
        transactionRepo.saveTransaction(initialTx)

        actionHandler.confirmTransaction(
            transaction = initialTx,
            updatedMerchant = "Zomato Gourmet",
            updatedAmount = BigDecimal("650"),
            updatedType = "Expense",
            categoryId = 5L,
            saveAsRule = true
        )

        val saved = transactionRepo.savedTransactions.first { it.id == 100L }
        assertEquals("Zomato Gourmet", saved.merchant)
        assertEquals(BigDecimal("650"), saved.amount)
        assertEquals(5L, saved.categoryId)
        assertTrue(saved.isReviewed)

        // Verify rule was saved
        assertEquals(1, ruleRepo.savedRules.size)
        assertEquals("Zomato Gourmet", ruleRepo.savedRules.first().keyword)
        assertEquals(5L, ruleRepo.savedRules.first().categoryId)
    }

    @Test
    fun testDiscardTransactionDeletesIt() = runTest {
        val tx = Transaction(
            id = 200L,
            accountId = 1L,
            categoryId = null,
            amount = BigDecimal("1200"),
            merchant = "Bank SMS Misread",
            timestamp = 2000L,
            type = "Expense",
            isReviewed = false
        )
        transactionRepo.saveTransaction(tx)

        actionHandler.discardTransaction(tx)

        assertTrue(transactionRepo.savedTransactions.isEmpty())
        assertEquals(1, transactionRepo.deletedTransactions.size)
        assertEquals(200L, transactionRepo.deletedTransactions.first().id)
    }

    @Test
    fun testShouldShowOverlayOnlyWhenPermissionAndSettingAreTrue() {
        assertTrue(actionHandler.shouldShowOverlay(isPermissionGranted = true, isOverlayEnabledInSettings = true))
        assertFalse(actionHandler.shouldShowOverlay(isPermissionGranted = false, isOverlayEnabledInSettings = true))
        assertFalse(actionHandler.shouldShowOverlay(isPermissionGranted = true, isOverlayEnabledInSettings = false))
        assertFalse(actionHandler.shouldShowOverlay(isPermissionGranted = false, isOverlayEnabledInSettings = false))
    }
}
