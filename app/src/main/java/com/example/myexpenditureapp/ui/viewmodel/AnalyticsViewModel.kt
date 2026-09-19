package com.example.myexpenditureapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.insights.InsightsEngine
import com.example.myexpenditureapp.domain.insights.SmartInsight
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.math.BigDecimal
import java.math.RoundingMode

enum class DrillLevel {
    MACRO,
    CATEGORY,
    DAY,
    WEEK,
    INSIGHT
}

data class DrillNode(
    val level: DrillLevel = DrillLevel.MACRO,
    val title: String = "All Categories & Activity",
    val subtitle: String = "Macro Overview",
    val category: Category? = null,
    val dayOfMonth: Int? = null,
    val weekNumber: Int? = null,
    val insight: SmartInsight? = null,
    val merchant: String? = null
)

data class AnalyticsUiState(
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val selectedTransactionType: String? = null, // "Income", "Expense", "Transfer"
    val selectedSource: String? = null, // "Manual", "SMS"
    val dateRange: LongRange? = null,
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR)
)

class AnalyticsViewModel : ViewModel() {
    private val transactionRepository = Graph.transactionRepository
    private val categoryRepository = Graph.categoryRepository
    private val getAccountsUseCase = Graph.getAccountsUseCase
    private val budgetRepository = Graph.budgetRepository

    private val _filterState = MutableStateFlow(AnalyticsUiState())
    val filterState = _filterState.asStateFlow()

    private val _drillState = MutableStateFlow(DrillNode())
    val drillState = _drillState.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = getAccountsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<com.example.myexpenditureapp.data.entity.Budget>> = budgetRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insights: Flow<List<SmartInsight>> = combine(transactions, categories, budgets) { txs, cats, buds ->
        InsightsEngine.generateInsights(txs, cats, buds)
    }

    val filteredTransactions = combine(transactions, _filterState) { txs, filters ->
        val calendar = Calendar.getInstance()
        calendar.set(filters.year, filters.month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        calendar.set(filters.year, filters.month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val monthEnd = calendar.timeInMillis

        txs.filter { tx ->
            val accountMatch = filters.selectedAccountId == null || tx.accountId == filters.selectedAccountId
            val categoryMatch = filters.selectedCategoryId == null || tx.categoryId == filters.selectedCategoryId
            val typeMatch = filters.selectedTransactionType == null || tx.type == filters.selectedTransactionType
            val sourceMatch = when (filters.selectedSource) {
                "SMS" -> tx.smsId != null
                "Manual" -> tx.smsId == null
                else -> true
            }
            val dateMatch = if (filters.dateRange != null) {
                tx.timestamp in filters.dateRange
            } else {
                tx.timestamp in monthStart..monthEnd
            }
            accountMatch && categoryMatch && typeMatch && sourceMatch && dateMatch
        }
    }

    // Transactions filtered specifically by the active DrillNode
    val drillDownTransactions: Flow<List<Transaction>> = combine(filteredTransactions, _drillState) { txs, drill ->
        val cal = Calendar.getInstance()
        when (drill.level) {
            DrillLevel.MACRO -> emptyList()
            DrillLevel.CATEGORY -> {
                if (drill.category != null) {
                    txs.filter { it.categoryId == drill.category.id }
                } else txs
            }
            DrillLevel.DAY -> {
                if (drill.dayOfMonth != null) {
                    txs.filter {
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.DAY_OF_MONTH) == drill.dayOfMonth
                    }
                } else txs
            }
            DrillLevel.WEEK -> {
                if (drill.weekNumber != null) {
                    txs.filter {
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.WEEK_OF_MONTH) == drill.weekNumber
                    }
                } else txs
            }
            DrillLevel.INSIGHT -> {
                val ins = drill.insight
                if (ins != null) {
                    txs.filter { tx ->
                        val catMatch = ins.drillDownCategoryId == null || tx.categoryId == ins.drillDownCategoryId
                        val merchMatch = ins.drillDownMerchant == null || tx.merchant.equals(ins.drillDownMerchant, ignoreCase = true)
                        val typeMatch = ins.drillDownType == null || tx.type.equals(ins.drillDownType, ignoreCase = true)
                        catMatch && merchMatch && typeMatch
                    }
                } else txs
            }
        }
    }

    private val _isParentRollupEnabled = MutableStateFlow(true)
    val isParentRollupEnabled = _isParentRollupEnabled.asStateFlow()

    fun toggleParentRollup() {
        _isParentRollupEnabled.update { !it }
    }

    // Micro merchant/subcategory breakdown when drilled into a Category
    val categorySubBreakdown: Flow<Map<String, BigDecimal>> = combine(drillDownTransactions, _drillState) { txs, drill ->
        if (drill.level != DrillLevel.CATEGORY || drill.category == null) {
            emptyMap()
        } else {
            txs.groupBy { if (it.merchant.isNotBlank()) it.merchant.trim() else "Other" }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
                .toList()
                .sortedByDescending { it.second }
                .toMap()
        }
    }

    val categorySpending: Flow<Map<Category?, BigDecimal>> = combine(
        filteredTransactions,
        categories,
        _isParentRollupEnabled
    ) { txs, cats, rollup ->
        val catMap = cats.associateBy { it.id }
        txs.filter { it.type == "Expense" }
            .groupBy { tx ->
                val directCat = tx.categoryId?.let { catMap[it] }
                if (rollup && directCat?.parentId != null) {
                    var curr: Category? = directCat
                    while (curr?.parentId != null) {
                        val parent = catMap[curr.parentId]
                        if (parent != null) {
                            curr = parent
                        } else {
                            break
                        }
                    }
                    curr
                } else {
                    directCat
                }
            }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
    }

    val weeklySpending: Flow<Map<String, BigDecimal>> = filteredTransactions.map { txs ->
        val calendar = Calendar.getInstance()
        val result = mutableMapOf<Int, BigDecimal>()
        
        txs.filter { it.type == "Expense" }
            .forEach { tx ->
                calendar.timeInMillis = tx.timestamp
                val week = calendar.get(Calendar.WEEK_OF_MONTH)
                result[week] = (result[week] ?: BigDecimal.ZERO).add(tx.amount)
            }
            
        result.toSortedMap().mapKeys { "Week ${it.key}" }
    }

    val monthlyComparison: Flow<Map<String, BigDecimal>> = transactions.map { txs ->
        val calendar = Calendar.getInstance()
        val result = mutableMapOf<Long, BigDecimal>()
        
        for (i in 0 until 6) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.MONTH, -i)
            tempCal.set(Calendar.DAY_OF_MONTH, 1)
            tempCal.set(Calendar.HOUR_OF_DAY, 0)
            tempCal.set(Calendar.MINUTE, 0)
            tempCal.set(Calendar.SECOND, 0)
            tempCal.set(Calendar.MILLISECOND, 0)
            result[tempCal.timeInMillis] = BigDecimal.ZERO
        }

        val sixMonthsAgo = result.keys.minOrNull() ?: 0L
        
        txs.filter { it.type == "Expense" && it.timestamp >= sixMonthsAgo }
            .forEach { tx ->
                calendar.timeInMillis = tx.timestamp
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                if (result.containsKey(monthStart)) {
                    result[monthStart] = (result[monthStart] ?: BigDecimal.ZERO).add(tx.amount)
                }
            }
            
        result.toSortedMap().mapKeys { 
            calendar.timeInMillis = it.key
            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
            val year = calendar.get(Calendar.YEAR)
            "$month $year"
        }
    }

    val spendingTrend: Flow<Map<Long, BigDecimal>> = filteredTransactions.map { txs ->
        txs.filter { it.type == "Expense" }
            .groupBy { it.timestamp / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000) }
            .mapValues { it.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
            .toSortedMap()
    }

    val projectedSpend: Flow<BigDecimal> = spendingTrend.map { trend ->
        if (trend.isEmpty()) return@map BigDecimal.ZERO
        val calendar = Calendar.getInstance()
        val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val monthStart = calendar.apply { 
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val monthExpenses = trend.filterKeys { it >= monthStart }.values.fold(BigDecimal.ZERO) { acc, d -> acc.add(d) }
        
        monthExpenses.divide(BigDecimal(currentDayOfMonth), 2, RoundingMode.HALF_UP).multiply(BigDecimal(daysInMonth))
    }

    val projectedTrend: Flow<Map<Long, BigDecimal>> = combine(spendingTrend, projectedSpend) { trend, projection ->
        if (trend.isEmpty()) return@combine emptyMap()
        
        val lastDay = trend.keys.maxOrNull() ?: return@combine emptyMap()
        val calendar = Calendar.getInstance()
        val lastDayOfMonth = calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        mapOf(lastDay to trend[lastDay]!!, lastDayOfMonth to projection)
    }

    val monthlyPerformance: Flow<MonthlyPerformance> = combine(transactions, budgets, _filterState) { txs, buds, filters ->
        val month = filters.month
        val year = filters.year
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endOfMonth = calendar.timeInMillis
        
        val currentMonthTxs = txs.filter { it.timestamp in startOfMonth..endOfMonth }
        val currentSpent = currentMonthTxs.filter { it.type == "Expense" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val currentIncome = currentMonthTxs.filter { it.type == "Income" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val currentSavings = currentIncome.subtract(currentSpent)
        
        val currentBudgets = buds.filter { it.month == month && it.year == year }
        val totalBudgeted = currentBudgets.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.limitAmount) }
        
        // Previous month savings
        calendar.set(year, month - 1, 1)
        calendar.add(Calendar.MONTH, -1)
        val prevMonth = calendar.get(Calendar.MONTH) + 1
        val prevYear = calendar.get(Calendar.YEAR)
        calendar.set(prevYear, prevMonth - 1, 1, 0, 0, 0)
        val startOfPrevMonth = calendar.timeInMillis
        calendar.set(prevYear, prevMonth - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endOfPrevMonth = calendar.timeInMillis
        
        val prevMonthTxs = txs.filter { it.timestamp in startOfPrevMonth..endOfPrevMonth }
        val prevSpent = prevMonthTxs.filter { it.type == "Expense" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val prevIncome = prevMonthTxs.filter { it.type == "Income" }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val prevSavings = prevIncome.subtract(prevSpent)
        
        val savingsChange = if (prevSavings != BigDecimal.ZERO) {
            currentSavings.subtract(prevSavings).divide(prevSavings.abs().coerceAtLeast(BigDecimal.ONE), 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else BigDecimal.ZERO
        
        val grade = when {
            totalBudgeted == BigDecimal.ZERO -> "No Budgets"
            currentSpent > totalBudgeted -> "Overspent"
            currentSpent > totalBudgeted.multiply(BigDecimal(0.9)) -> "Near Limit"
            else -> "On Track"
        }
        
        MonthlyPerformance(
            totalBudgeted = totalBudgeted,
            totalSpent = currentSpent,
            savingsChange = savingsChange,
            grade = grade,
            netSavings = currentSavings
        )
    }

    data class MonthlyPerformance(
        val totalBudgeted: BigDecimal,
        val totalSpent: BigDecimal,
        val savingsChange: BigDecimal,
        val grade: String,
        val netSavings: BigDecimal
    )

    fun onMonthYearChange(month: Int, year: Int) {
        _filterState.value = _filterState.value.copy(month = month, year = year)
        resetDrill()
    }

    // Bidirectional Drill Operations
    fun drillDownCategory(category: Category) {
        _drillState.value = DrillNode(
            level = DrillLevel.CATEGORY,
            title = "${category.icon ?: "📁"} ${category.name}",
            subtitle = "Category Breakdown & Merchants",
            category = category
        )
    }

    fun drillDownDay(dayOfMonth: Int) {
        val monthName = Calendar.getInstance().apply { set(Calendar.MONTH, _filterState.value.month - 1) }
            .getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
        _drillState.value = DrillNode(
            level = DrillLevel.DAY,
            title = "Day $dayOfMonth $monthName",
            subtitle = "Daily Spending Timeline",
            dayOfMonth = dayOfMonth
        )
    }

    fun drillDownWeek(weekNumber: Int) {
        _drillState.value = DrillNode(
            level = DrillLevel.WEEK,
            title = "Week $weekNumber Activity",
            subtitle = "Weekly Cashflow Details",
            weekNumber = weekNumber
        )
    }

    fun drillDownInsight(insight: SmartInsight) {
        _drillState.value = DrillNode(
            level = DrillLevel.INSIGHT,
            title = "${insight.icon} ${insight.title}",
            subtitle = insight.description,
            insight = insight
        )
    }

    fun drillUp() {
        _drillState.value = DrillNode(
            level = DrillLevel.MACRO,
            title = "All Categories & Activity",
            subtitle = "Macro Overview"
        )
    }

    fun resetDrill() {
        drillUp()
    }

    val kpis: Flow<List<KPI>> = combine(filteredTransactions, categories, budgets, projectedSpend, _filterState) { txs, cats, buds, projection, filters ->
        val month = filters.month
        val year = filters.year
        
        val expenses = txs.filter { it.type == "Expense" }
        val income = txs.filter { it.type == "Income" }
        
        val totalExpenses = expenses.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val totalIncome = income.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val netCashFlow = totalIncome.subtract(totalExpenses)
        
        val highestCategory = expenses.groupBy { it.categoryId }
            .mapValues { it.value.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) } }
            .maxByOrNull { it.value }
            ?.let { cats.find { c -> c.id == it.key }?.name ?: "Other" } ?: "N/A"
            
        val currentBudgets = buds.filter { it.month == month && it.year == year }
        val budgetLimit = currentBudgets.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.limitAmount) }.coerceAtLeast(BigDecimal.ONE)
        val budgetUsed = totalExpenses.divide(budgetLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))

        listOf(
            KPI("Net Flow", "₹${netCashFlow.setScale(0, RoundingMode.HALF_UP)}", "Income - Exp"),
            KPI("Top Category", highestCategory, "Most spent"),
            KPI("Monthly Spend", "₹${totalExpenses.setScale(0, RoundingMode.HALF_UP)}", "Budget: ${budgetUsed.setScale(0, RoundingMode.HALF_UP)}%"),
            KPI("Projected", "₹${projection.setScale(0, RoundingMode.HALF_UP)}", "End of month")
        )
    }

    data class KPI(val label: String, val value: String, val secondary: String)

    fun onAccountFilterChange(accountId: Long?) {
        _filterState.value = _filterState.value.copy(selectedAccountId = accountId)
    }

    fun onCategoryFilterChange(categoryId: Long?) {
        _filterState.value = _filterState.value.copy(selectedCategoryId = categoryId)
    }

    fun onTypeFilterChange(type: String?) {
        _filterState.value = _filterState.value.copy(selectedTransactionType = type)
    }

    fun onSourceFilterChange(source: String?) {
        _filterState.value = _filterState.value.copy(selectedSource = source)
    }
    
    fun onDateRangeChange(start: Long?, end: Long?) {
        val range = if (start != null && end != null) start..end else null
        _filterState.value = _filterState.value.copy(dateRange = range)
    }

    fun exportTransactionsToCsv(context: android.content.Context) {
        viewModelScope.launch {
            val txs = filteredTransactions.first()
            val cats = categories.value
            val accs = accounts.value
            
            val csvBuilder = StringBuilder()
            csvBuilder.append("Date,Merchant,Amount,Type,Category,Account,To Account\n")
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            
            txs.forEach { tx ->
                val date = sdf.format(java.util.Date(tx.timestamp))
                val category = cats.find { it.id == tx.categoryId }?.name ?: "N/A"
                val account = accs.find { it.id == tx.accountId }?.name ?: "N/A"
                val toAccount = if (tx.type == "Transfer") accs.find { it.id == tx.toAccountId }?.name ?: "N/A" else ""
                
                csvBuilder.append("\"$date\",\"${tx.merchant}\",${tx.amount},${tx.type},\"$category\",\"$account\",\"$toAccount\"\n")
            }
            
            try {
                val fileName = "expenditure_export_${System.currentTimeMillis()}.csv"
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(csvBuilder.toString())
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = android.content.Intent.createChooser(intent, "Export Transactions")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
