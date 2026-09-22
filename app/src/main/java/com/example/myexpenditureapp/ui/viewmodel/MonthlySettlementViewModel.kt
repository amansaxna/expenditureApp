package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.SavingGoal
import com.example.myexpenditureapp.domain.settlement.MonthClosureSummary
import com.example.myexpenditureapp.domain.settlement.MonthlySettlementManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MonthlySettlementViewModel : ViewModel() {
    private val transactionRepo = Graph.transactionRepository
    private val categoryRepo = Graph.categoryRepository
    private val budgetRepo = Graph.budgetRepository
    private val goalRepo = Graph.savingGoalRepository

    private val _eventChannel = Channel<UiEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible = _isBottomSheetVisible.asStateFlow()

    private val _isBannerDismissed = MutableStateFlow(false)
    val isBannerDismissed = _isBannerDismissed.asStateFlow()

    val activeGoals: StateFlow<List<SavingGoal>> = goalRepo.getAllGoals()
        .map { goals -> goals.filter { !it.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsettledMonthSummary: StateFlow<MonthClosureSummary?> = combine(
        transactionRepo.getAllTransactions(),
        categoryRepo.getAllCategories(),
        budgetRepo.getAllBudgets()
    ) { txs, cats, buds ->
        val (prevMonth, prevYear) = MonthlySettlementManager.getPreviousMonthAndYear()
        val isSettled = MonthlySettlementManager.isMonthSettled(prevMonth, prevYear)
        val isDismissed = MonthlySettlementManager.isMonthDismissed(prevMonth, prevYear)

        if (isSettled) {
            null
        } else {
            val summary = MonthlySettlementManager.calculateMonthSummary(
                month = prevMonth,
                year = prevYear,
                transactions = txs,
                categories = cats,
                budgets = buds
            )
            // If the user had activity (transactions or budgets) in the previous month
            if (summary.transactionCount > 0 || summary.budgetTotal > BigDecimal.ZERO) {
                summary
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun showSettlementSheet() {
        _isBottomSheetVisible.value = true
    }

    fun hideSettlementSheet() {
        _isBottomSheetVisible.value = false
    }

    fun dismissBanner() {
        val summary = unsettledMonthSummary.value
        if (summary != null) {
            MonthlySettlementManager.markMonthDismissed(summary.month, summary.year)
        }
        _isBannerDismissed.value = true
    }

    fun sweepToGoal(goalId: Long, amount: BigDecimal) {
        val summary = unsettledMonthSummary.value ?: return
        viewModelScope.launch {
            try {
                MonthlySettlementManager.sweepToSavingGoal(
                    goalId = goalId,
                    amount = amount,
                    month = summary.month,
                    year = summary.year
                )
                _isBottomSheetVisible.value = false
                _eventChannel.send(UiEvent.ShowSnackbar("🎉 Swept ₹${amount.toPlainString()} into savings goal! ${summary.monthName} is settled."))
                _eventChannel.send(UiEvent.Success)
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error sweeping to goal: ${e.localizedMessage}"))
            }
        }
    }

    fun rolloverToBudget(amount: BigDecimal) {
        val summary = unsettledMonthSummary.value ?: return
        viewModelScope.launch {
            try {
                MonthlySettlementManager.rolloverToCurrentMonthBudget(
                    amount = amount,
                    month = summary.month,
                    year = summary.year
                )
                _isBottomSheetVisible.value = false
                _eventChannel.send(UiEvent.ShowSnackbar("🔄 Rolled over ₹${amount.toPlainString()} to this month's budget! ${summary.monthName} is settled."))
                _eventChannel.send(UiEvent.Success)
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error rolling over budget: ${e.localizedMessage}"))
            }
        }
    }

    fun settleCleanSlate(amount: BigDecimal) {
        val summary = unsettledMonthSummary.value ?: return
        viewModelScope.launch {
            try {
                MonthlySettlementManager.settleCleanSlate(
                    amount = amount,
                    month = summary.month,
                    year = summary.year
                )
                _isBottomSheetVisible.value = false
                _eventChannel.send(UiEvent.ShowSnackbar("🍃 ${summary.monthName} closed with a clean slate!"))
                _eventChannel.send(UiEvent.Success)
            } catch (e: Exception) {
                _eventChannel.send(UiEvent.ShowSnackbar("Error settling month: ${e.localizedMessage}"))
            }
        }
    }
}
