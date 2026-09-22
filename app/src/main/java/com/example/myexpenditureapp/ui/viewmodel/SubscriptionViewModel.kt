package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Subscription
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.radar.RadarBillItem
import com.example.myexpenditureapp.domain.radar.SubscriptionRadarEngine
import com.example.myexpenditureapp.domain.radar.SubscriptionRadarSummary
import com.example.myexpenditureapp.domain.radar.SubscriptionSuggestion
import com.example.myexpenditureapp.notifications.NotificationHelper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SubscriptionViewModel : ViewModel() {
    private val subscriptionRepo = Graph.subscriptionRepository
    private val transactionRepo = Graph.transactionRepository
    private val accountRepo = Graph.accountRepository

    val subscriptions: StateFlow<List<Subscription>> = subscriptionRepo.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts = accountRepo.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = Graph.categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dismissedSuggestions = MutableStateFlow<Set<String>>(emptySet())

    val radarSummary: StateFlow<SubscriptionRadarSummary> = combine(
        subscriptions,
        transactionRepo.getAllTransactions(),
        _dismissedSuggestions
    ) { subs, txs, dismissed ->
        val summary = SubscriptionRadarEngine.computeRadar(subs, txs)
        summary.copy(
            detectedSuggestions = summary.detectedSuggestions.filter { !dismissed.contains(it.merchantName.lowercase()) }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SubscriptionRadarSummary(
            totalMonthlyCommitment = BigDecimal.ZERO,
            upcomingNext7Days = BigDecimal.ZERO,
            paidThisMonth = BigDecimal.ZERO,
            overdueAmount = BigDecimal.ZERO,
            billItems = emptyList(),
            detectedSuggestions = emptyList()
        )
    )

    private val _eventChannel = Channel<UiEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    fun saveSubscription(
        name: String,
        amount: BigDecimal,
        billingCycle: String = "Monthly",
        dueDayOfMonth: Int = 1,
        categoryId: Long? = null,
        accountId: Long? = null,
        notes: String? = null,
        id: Long = 0
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _eventChannel.send(UiEvent.ShowSnackbar("Subscription name cannot be empty"))
                return@launch
            }
            if (amount <= BigDecimal.ZERO) {
                _eventChannel.send(UiEvent.ShowSnackbar("Amount must be greater than 0"))
                return@launch
            }

            try {
                val subscription = Subscription(
                    id = id,
                    name = name.trim(),
                    amount = amount,
                    billingCycle = billingCycle,
                    dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31),
                    categoryId = categoryId,
                    accountId = accountId,
                    notes = notes?.trim(),
                    isActive = true
                )
                if (id == 0L) {
                    subscriptionRepo.insertSubscription(subscription)
                    _eventChannel.send(UiEvent.ShowSnackbar("Subscription added"))
                } else {
                    subscriptionRepo.updateSubscription(subscription)
                    _eventChannel.send(UiEvent.ShowSnackbar("Subscription updated"))
                }
                NotificationHelper.triggerSubscriptionCheck(Graph.appContext)
                _eventChannel.send(UiEvent.Success)
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error saving: ${e.localizedMessage}"))
            }
        }
    }

    fun logPayment(item: RadarBillItem) {
        viewModelScope.launch {
            try {
                val sub = item.subscription
                val defaultAccount = accounts.value.firstOrNull()
                val targetAccountId = sub.accountId ?: defaultAccount?.id

                if (targetAccountId == null) {
                    _eventChannel.send(UiEvent.ShowSnackbar("Please create an account first to log payments"))
                    return@launch
                }

                val now = System.currentTimeMillis()
                val tx = Transaction(
                    accountId = targetAccountId,
                    categoryId = sub.categoryId,
                    amount = sub.amount,
                    merchant = sub.name,
                    timestamp = now,
                    type = "Expense",
                    isReviewed = true,
                    tags = listOf("Subscription", "AutoBill")
                )

                transactionRepo.saveTransaction(tx)
                subscriptionRepo.updateSubscription(sub.copy(lastPaidDate = now))
                NotificationHelper.triggerSubscriptionCheck(Graph.appContext)

                _eventChannel.send(UiEvent.ShowSnackbar("Payment logged for ${sub.name} (₹${sub.amount})"))
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error logging payment: ${e.localizedMessage}"))
            }
        }
    }

    fun acceptSuggestion(suggestion: SubscriptionSuggestion) {
        viewModelScope.launch {
            val sub = Subscription(
                name = suggestion.merchantName,
                amount = suggestion.estimatedAmount,
                billingCycle = "Monthly",
                dueDayOfMonth = suggestion.detectedDueDay,
                categoryId = suggestion.suggestedCategoryId,
                accountId = suggestion.suggestedAccountId,
                isActive = true
            )
            subscriptionRepo.insertSubscription(sub)
            _dismissedSuggestions.update { it + suggestion.merchantName.lowercase() }
            _eventChannel.send(UiEvent.ShowSnackbar("Added ${suggestion.merchantName} to subscriptions"))
        }
    }

    fun dismissSuggestion(suggestion: SubscriptionSuggestion) {
        _dismissedSuggestions.update { it + suggestion.merchantName.lowercase() }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionRepo.deleteSubscription(subscription)
            _eventChannel.send(UiEvent.ShowSnackbar("Subscription deleted"))
        }
    }
}
