package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.ui.theme.ExpenseRed
import com.example.myexpenditureapp.ui.theme.IncomeGreen
import com.example.myexpenditureapp.ui.viewmodel.TransactionViewModel
import com.example.myexpenditureapp.ui.component.CalculatorTextField
import com.example.myexpenditureapp.ui.component.CategoryTabSelector
import com.example.myexpenditureapp.ui.component.evaluateExpression
import com.example.myexpenditureapp.ui.theme.MonospaceFont
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var quickEditTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val haptic = LocalHapticFeedback.current

    val currentWeek = remember { Calendar.getInstance().get(Calendar.YEAR) * 100 + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) }
    var expandedWeeks by remember { mutableStateOf(setOf(currentWeek)) }

    val weekGroups = remember(uiState.transactions) {
        getWeekGroups(uiState.transactions)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (uiState.filterAccountId != null || uiState.filterCategoryId != null)
                                        MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear All",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { }

                HorizontalMonthPicker(
                    selectedMonth = uiState.selectedMonth,
                    selectedYear = uiState.selectedYear,
                    onMonthSelected = { month, year -> viewModel.onMonthYearChange(month, year) }
                )

                MonthlySummaryBar(
                    income = uiState.totalIncome,
                    expense = uiState.totalExpense,
                    net = uiState.netBalance
                )
                
                FilterChipsRow(
                    accounts = uiState.accounts,
                    categories = uiState.categories,
                    selectedAccountId = uiState.filterAccountId,
                    selectedCategoryId = uiState.filterCategoryId,
                    onAccountSelected = { viewModel.onFilterAccountChange(it) },
                    onCategorySelected = { viewModel.onFilterCategoryChange(it) }
                )
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddTransaction()
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(30.dp))
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No transactions found for this month",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                weekGroups.forEach { group ->
                    stickyHeader {
                        WeekHeader(
                            group = group,
                            isExpanded = expandedWeeks.contains(group.weekOfYear),
                            onToggle = {
                                expandedWeeks = if (expandedWeeks.contains(group.weekOfYear)) {
                                    expandedWeeks - group.weekOfYear
                                } else {
                                    expandedWeeks + group.weekOfYear
                                }
                            }
                        )
                    }

                    if (expandedWeeks.contains(group.weekOfYear)) {
                        items(group.transactions, key = { it.id }) { transaction ->
                            Box(modifier = Modifier.animateItem()) {
                                TransactionRow(
                                    transaction = transaction,
                                    categories = uiState.categories,
                                    accounts = uiState.accounts,
                                    onClick = { onEditTransaction(transaction) },
                                    onLongClick = { quickEditTransaction = transaction }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            accounts = uiState.accounts,
            categories = uiState.categories,
            selectedAccountId = uiState.filterAccountId,
            selectedCategoryId = uiState.filterCategoryId,
            onAccountSelected = { viewModel.onFilterAccountChange(it) },
            onCategorySelected = { viewModel.onFilterCategoryChange(it) },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Clear All Transactions") },
            text = { Text("Are you sure you want to delete ALL transactions and reset account balances? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllTransactions()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (quickEditTransaction != null) {
        QuickEditBottomSheet(
            transaction = quickEditTransaction!!,
            categories = uiState.categories,
            onUpdateCategory = { catId ->
                viewModel.updateTransactionCategory(quickEditTransaction!!, catId)
                quickEditTransaction = null
            },
            onUpdateAmount = { amount ->
                viewModel.updateTransactionAmount(quickEditTransaction!!, amount)
                quickEditTransaction = null
            },
            onDismiss = { quickEditTransaction = null },
            sheetState = sheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickEditBottomSheet(
    transaction: Transaction,
    categories: List<Category>,
    onUpdateCategory: (Long?) -> Unit,
    onUpdateAmount: (BigDecimal) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Quick Edit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                transaction.merchant,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Update Amount",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            var amountText by remember { mutableStateOf(transaction.amount.stripTrailingZeros().toPlainString()) }
            CalculatorTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount"
            )

            Button(
                onClick = {
                    val amt = evaluateExpression(amountText)?.let { BigDecimal(it) } ?: transaction.amount
                    onUpdateAmount(amt)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text("Update Amount")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Change Category",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            CategoryTabSelector(
                categories = categories,
                selectedCategoryId = transaction.categoryId,
                onCategorySelected = onUpdateCategory
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    accounts: List<Account>,
    categories: List<Category>,
    selectedAccountId: Long?,
    selectedCategoryId: Long?,
    onAccountSelected: (Long?) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filters") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { onAccountSelected(null) },
                        label = { Text("All") }
                    )
                    accounts.forEach { account ->
                        FilterChip(
                            selected = selectedAccountId == account.id,
                            onClick = { onAccountSelected(account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All Categories") },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    val rootCategories = categories.filter { it.parentId == null }
                    rootCategories.forEach { parent ->
                        CategoryGroup(
                            parent = parent,
                            children = categories.filter { it.parentId == parent.id },
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = onCategorySelected
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryGroup(
    parent: Category,
    children: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCategorySelected(parent.id) }
                .padding(vertical = 4.dp),
            color = if (selectedCategoryId == parent.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        ) {
            Text(
                text = "${parent.icon ?: ""} ${parent.name}".trim(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp),
                color = if (selectedCategoryId == parent.id) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
        }
        
        FlowRow(
            modifier = Modifier.padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            children.forEach { child ->
                FilterChip(
                    selected = selectedCategoryId == child.id,
                    onClick = { onCategorySelected(child.id) },
                    label = { Text("${child.icon ?: ""} ${child.name}".trim()) }
                )
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    accounts: List<Account>,
    categories: List<Category>,
    selectedAccountId: Long?,
    selectedCategoryId: Long?,
    onAccountSelected: (Long?) -> Unit,
    onCategorySelected: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedAccountId != null || selectedCategoryId != null,
                onClick = { 
                    onAccountSelected(null)
                    onCategorySelected(null)
                },
                label = { Text("All") },
                leadingIcon = if (selectedAccountId == null && selectedCategoryId == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }

        if (selectedAccountId != null) {
            val account = accounts.find { it.id == selectedAccountId }
            item {
                FilterChip(
                    selected = true,
                    onClick = { onAccountSelected(null) },
                    label = { Text(account?.name ?: "Account") },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (selectedCategoryId != null) {
            val category = categories.find { it.id == selectedCategoryId }
            item {
                FilterChip(
                    selected = true,
                    onClick = { onCategorySelected(null) },
                    label = { Text("${category?.icon ?: ""} ${category?.name}".trim()) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
fun HorizontalMonthPicker(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthSelected: (Int, Int) -> Unit
) {
    val months = remember {
        val list = mutableListOf<Pair<Int, Int>>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -3)
        repeat(7) {
            list.add(cal.get(Calendar.MONTH) to cal.get(Calendar.YEAR))
            cal.add(Calendar.MONTH, 1)
        }
        list
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months) { (month, year) ->
            val isSelected = month == selectedMonth && year == selectedYear
            val cal = Calendar.getInstance().apply {
                set(Calendar.MONTH, month)
                set(Calendar.YEAR, year)
            }
            val monthName = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
            
            FilterChip(
                selected = isSelected,
                onClick = { onMonthSelected(month, year) },
                label = { Text(monthName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun MonthlySummaryBar(
    income: BigDecimal,
    expense: BigDecimal,
    net: BigDecimal
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompactSummaryItem("Income", income, IncomeGreen)
            CompactSummaryItem("Expense", expense, ExpenseRed)
            CompactSummaryItem("Net", net, if (net >= BigDecimal.ZERO) IncomeGreen else ExpenseRed)
        }
    }
}

@Composable
private fun CompactSummaryItem(label: String, amount: BigDecimal, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(
            "₹${amount.setScale(0, java.math.RoundingMode.HALF_UP)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonospaceFont),
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

data class WeekGroup(
    val weekOfYear: Int,
    val startDate: Date,
    val endDate: Date,
    val transactions: List<Transaction>,
    val totalSpending: BigDecimal
)

fun getWeekGroups(transactions: List<Transaction>): List<WeekGroup> {
    val cal = Calendar.getInstance()
    val grouped = transactions.groupBy {
        cal.timeInMillis = it.timestamp
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        year * 100 + week
    }
    
    return grouped.map { (yearWeek, txs) ->
        val year = yearWeek / 100
        val week = yearWeek % 100
        val weekCal = Calendar.getInstance()
        weekCal.set(Calendar.YEAR, year)
        weekCal.set(Calendar.WEEK_OF_YEAR, week)
        weekCal.set(Calendar.DAY_OF_WEEK, weekCal.firstDayOfWeek)
        val start = weekCal.time
        weekCal.add(Calendar.DAY_OF_WEEK, 6)
        val end = weekCal.time
        
        val total = txs.filter { it.type == "Expense" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        
        WeekGroup(yearWeek, start, end, txs, total)
    }.sortedByDescending { it.weekOfYear }
}

@Composable
fun WeekHeader(
    group: WeekGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Week ${group.weekOfYear % 100}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${sdf.format(group.startDate)} - ${sdf.format(group.endDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Text(
                text = "₹${group.totalSpending.setScale(0, java.math.RoundingMode.HALF_UP)}",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val category = categories.find { it.id == transaction.categoryId }
    val account = accounts.find { it.id == transaction.accountId }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day
                Text(
                    text = SimpleDateFormat("dd", Locale.getDefault()).format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(28.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Merchant & Account
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "${category?.name ?: "Misc"} • ${account?.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                // Amount
                Text(
                    text = "₹${transaction.amount.stripTrailingZeros().toPlainString()}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonospaceFont),
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        "Income" -> IncomeGreen
                        "Expense" -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    transaction: Transaction? = null,
    accounts: List<Account>,
    categories: List<Category>,
    onSave: (accountId: Long, toAccountId: Long?, categoryId: Long?, amount: BigDecimal, merchant: String, type: String, timestamp: Long, id: Long) -> Unit,
    onDelete: (Transaction) -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var amount by remember { mutableStateOf(transaction?.amount?.stripTrailingZeros()?.toPlainString() ?: "") }
    var merchant by remember { mutableStateOf(transaction?.merchant ?: "") }
    var type by remember { mutableStateOf(transaction?.type ?: "Expense") }
    var accountId by remember { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by remember { mutableStateOf(transaction?.toAccountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var categoryId by remember { mutableStateOf(transaction?.categoryId) }
    var timestamp by remember { mutableStateOf(transaction?.timestamp ?: System.currentTimeMillis()) }

    var expandedAccount by remember { mutableStateOf(false) }
    var expandedToAccount by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
    
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    val isAmountValid = amount.toDoubleOrNull()?.let { it > 0 } ?: false
    val isMerchantValid = merchant.isNotBlank()
    val isSaveEnabled = isAmountValid && isMerchantValid && accountId != 0L && (type != "Transfer" || toAccountId != 0L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transaction == null) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transaction != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Transaction",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CalculatorTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Amount"
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Description") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                singleLine = true
            )

            // Date Selection
            OutlinedTextField(
                value = sdf.format(Date(timestamp)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false, // To make it clickable but not editable
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Change Date")
                    }
                }
            )

            // Type Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    leadingIcon = {
                        Icon(
                            imageVector = when (type) {
                                "Income" -> Icons.AutoMirrored.Filled.TrendingUp
                                "Expense" -> Icons.AutoMirrored.Filled.TrendingDown
                                else -> Icons.Default.SwapHoriz
                            },
                            contentDescription = null
                        )
                    }
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    listOf("Expense", "Income", "Transfer").forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                type = t
                                expandedType = false
                            }
                        )
                    }
                }
            }

            // Account Dropdown
            val selectedAccount = accounts.find { it.id == accountId }
            ExposedDropdownMenuBox(
                expanded = expandedAccount,
                onExpandedChange = { expandedAccount = !expandedAccount }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (type == "Transfer") "From Account" else "Account") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccount) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = expandedAccount,
                    onDismissRequest = { expandedAccount = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                accountId = account.id
                                expandedAccount = false
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = type == "Transfer") {
                // To Account Dropdown
                val selectedToAccount = accounts.find { it.id == toAccountId }
                ExposedDropdownMenuBox(
                    expanded = expandedToAccount,
                    onExpandedChange = { expandedToAccount = !expandedToAccount }
                ) {
                    OutlinedTextField(
                        value = selectedToAccount?.name ?: "Select To Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToAccount) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedToAccount,
                        onDismissRequest = { expandedToAccount = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    toAccountId = account.id
                                    expandedToAccount = false
                                }
                            )
                        }
                    }
                }
            }

            // Category Selection (Bottom Sheet)
            val selectedCategory = categories.find { it.id == categoryId }
            var showCategorySheet by remember { mutableStateOf(false) }
            val categorySheetState = rememberModalBottomSheetState()

            OutlinedTextField(
                value = selectedCategory?.let { "${it.name} ${it.icon ?: ""}".trim() } ?: "None",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategorySheet = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
            )

            if (showCategorySheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCategorySheet = false },
                    sheetState = categorySheetState
                ) {
                    Column(modifier = Modifier.padding(bottom = 32.dp)) {
                        Text(
                            "Select Category",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        CategoryTabSelector(
                            categories = categories,
                            selectedCategoryId = categoryId,
                            onCategorySelected = {
                                categoryId = it
                                showCategorySheet = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val amt = evaluateExpression(amount)?.let { BigDecimal(it) } ?: BigDecimal.ZERO
                    onSave(
                        accountId,
                        if (type == "Transfer") toAccountId else null,
                        categoryId,
                        amt,
                        merchant,
                        type,
                        timestamp,
                        transaction?.id ?: 0L
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isSaveEnabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (transaction == null) "Save Transaction" else "Update Transaction", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        timestamp = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog && transaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(transaction)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReviewScreen(
    transaction: Transaction,
    onMarkAsReviewed: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = transaction.merchant,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "₹${transaction.amount.stripTrailingZeros().toPlainString()}",
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = MonospaceFont),
                color = if (transaction.type == "Income") IncomeGreen else ExpenseRed
            )
            
            Text(
                text = transaction.type,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onMarkAsReviewed,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Mark as Reviewed", style = MaterialTheme.typography.titleMedium)
            }
            
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Review Later", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
