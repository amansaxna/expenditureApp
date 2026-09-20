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
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.ui.viewmodel.TransactionViewModel
import com.example.myexpenditureapp.ui.component.CategorySelectionBottomSheet
import com.example.myexpenditureapp.ui.component.StandardTransactionRow
import com.example.myexpenditureapp.ui.component.CalculatorTextField
import com.example.myexpenditureapp.ui.component.CategoryTabSelector
import com.example.myexpenditureapp.ui.component.evaluateExpression
import com.example.myexpenditureapp.ui.theme.MonospaceFont
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.math.BigDecimal
import java.math.RoundingMode
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

    val dayGroups = remember(uiState.transactions) {
        getDayGroups(uiState.transactions)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                var showMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        onSearch = { },
                        active = false,
                        onActiveChange = { },
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { showFilterDialog = true }) {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.filterAccountId != null || uiState.filterCategoryId != null) {
                                                Badge { Text("1") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = "Filter",
                                            tint = if (uiState.filterAccountId != null || uiState.filterCategoryId != null)
                                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Clear All Transactions", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { 
                                                 Icon(
                                                     Icons.Default.DeleteSweep, 
                                                     contentDescription = null, 
                                                     tint = MaterialTheme.colorScheme.error 
                                                 ) 
                                            },
                                            onClick = {
                                                showMenu = false
                                                showDeleteAllDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { }
                }

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
            SmallFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddTransaction()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayGroups.forEach { group ->
                    stickyHeader(key = group.dayKey) {
                        DayHeader(group = group)
                    }

                    items(group.transactions, key = { it.id }) { transaction ->
                        val category = uiState.categories.find { it.id == transaction.categoryId }
                        val account = uiState.accounts.find { it.id == transaction.accountId }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        ) {
                            StandardTransactionRow(
                                transaction = transaction,
                                category = category,
                                accountName = account?.name ?: "Account",
                                onClick = { onEditTransaction(transaction) }
                            )
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
            
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMonthSelected(month, year) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp, 
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun MonthlySummaryBar(
    income: BigDecimal,
    expense: BigDecimal,
    net: BigDecimal
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompactSummaryItem(
                label = "Income", 
                amount = income, 
                color = IncomeGreen,
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )
            
            VerticalDivider(
                modifier = Modifier.height(28.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            CompactSummaryItem(
                label = "Expense", 
                amount = expense, 
                color = ExpenseRed,
                icon = Icons.AutoMirrored.Filled.TrendingDown
            )
            
            VerticalDivider(
                modifier = Modifier.height(28.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            CompactSummaryItem(
                label = "Net", 
                amount = net, 
                color = if (net >= BigDecimal.ZERO) IncomeGreen else ExpenseRed,
                icon = Icons.Default.AccountBalanceWallet
            )
        }
    }
}

@Composable
private fun CompactSummaryItem(
    label: String, 
    amount: BigDecimal, 
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.85f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label.uppercase(), 
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "₹${amount.setScale(0, java.math.RoundingMode.HALF_UP)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonospaceFont),
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

data class DayGroup(
    val dayKey: Long,
    val date: Date,
    val title: String,
    val transactions: List<Transaction>,
    val totalExpense: BigDecimal,
    val totalIncome: BigDecimal
)

fun getDayGroups(transactions: List<Transaction>): List<DayGroup> {
    val cal = Calendar.getInstance()
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val todayYear = todayCal.get(Calendar.YEAR)
    val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)

    val yesterdayYear = yesterdayCal.get(Calendar.YEAR)
    val yesterdayDay = yesterdayCal.get(Calendar.DAY_OF_YEAR)

    val grouped = transactions.groupBy {
        cal.timeInMillis = it.timestamp
        cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
    }

    val dayFormat = SimpleDateFormat("dd MMM, EEE", Locale.getDefault())
    val yearFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    return grouped.map { (key, txs) ->
        val firstTx = txs.first()
        cal.timeInMillis = firstTx.timestamp
        val txYear = cal.get(Calendar.YEAR)
        val txDay = cal.get(Calendar.DAY_OF_YEAR)

        val title = when {
            txYear == todayYear && txDay == todayDay -> "Today"
            txYear == yesterdayYear && txDay == yesterdayDay -> "Yesterday"
            txYear == todayYear -> dayFormat.format(cal.time)
            else -> yearFormat.format(cal.time)
        }

        val totalExp = txs.filter { it.type == "Expense" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val totalInc = txs.filter { it.type == "Income" }.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }

        DayGroup(
            dayKey = key,
            date = cal.time,
            title = title,
            transactions = txs.sortedByDescending { it.timestamp },
            totalExpense = totalExp,
            totalIncome = totalInc
        )
    }.sortedByDescending { it.dayKey }
}

@Composable
fun DayHeader(
    group: DayGroup
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = "${group.transactions.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (group.totalExpense > BigDecimal.ZERO) {
                Text(
                    text = "-₹${group.totalExpense.setScale(0, RoundingMode.HALF_UP)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonospaceFont, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditScreen(
    transaction: Transaction? = null,
    accounts: List<Account>,
    categories: List<Category>,
    onSave: (accountId: Long, toAccountId: Long?, categoryId: Long?, amount: BigDecimal, merchant: String, type: String, timestamp: Long, id: Long, tags: List<String>, saveAsRule: Boolean) -> Unit,
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
    var tags by remember { mutableStateOf(transaction?.tags ?: emptyList()) }
    var customTags by remember { mutableStateOf(emptyList<String>()) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var saveAsRule by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }

    val commonTags = listOf("split", "vacation", "work", "personal", "reimbursable", "food", "gift")

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
            // Amount with Split Bill Action
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CalculatorTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = "Amount"
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { showSplitDialog = true },
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = "Split Bill", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Quick Increment Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    val increments = listOf(100, 500, 1000, 2000)
                    items(increments) { inc ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    val current = evaluateExpression(amount)?.toDoubleOrNull() ?: 0.0
                                    amount = (current + inc).toLong().toString()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "+₹$inc",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Segmented Transaction Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Expense" to ExpenseRed, "Income" to IncomeGreen, "Transfer" to BlueAccent).forEach { (t, activeColor) ->
                    val isSelected = type == t
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent
                            )
                            .clickable {
                                type = t
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Description") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
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
                enabled = false,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategorySheet = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
            )

            if (showCategorySheet) {
                CategorySelectionBottomSheet(
                    categories = categories,
                    selectedCategoryId = categoryId,
                    onCategorySelected = {
                        categoryId = it
                        showCategorySheet = false
                    },
                    onAddNewCategory = {
                        showCategorySheet = false
                    },
                    onDismiss = { showCategorySheet = false }
                )
            }

            // TAGS SECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tags & Labels",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            newTagInput = ""
                            showAddTagDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom Tag", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allAvailableTags = (commonTags + customTags + tags).distinct()
                    allAvailableTags.forEach { tag ->
                        val isSelected = tags.contains(tag)
                        val isCustom = !commonTags.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tags = if (isSelected) tags - tag else tags + tag
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            label = { Text("#$tag") },
                            trailingIcon = if (isCustom) {
                                {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete custom tag",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                tags = tags - tag
                                                customTags = customTags - tag
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    AssistChip(
                        onClick = {
                            newTagInput = ""
                            showAddTagDialog = true
                        },
                        label = { Text("+ Add Tag") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // AUTO-CATEGORIZATION RULE TOGGLE
            if (categoryId != null && merchant.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { saveAsRule = !saveAsRule },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveAsRule,
                            onCheckedChange = { saveAsRule = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Always categorize '$merchant' as ${selectedCategory?.name ?: "this category"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        transaction?.id ?: 0L,
                        tags,
                        saveAsRule
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

    if (showSplitDialog) {
        val initialAmt = evaluateExpression(amount)?.let { BigDecimal(it) } ?: transaction?.amount ?: BigDecimal.ZERO
        com.example.myexpenditureapp.ui.component.SplitBillDialog(
            initialAmount = initialAmt,
            merchant = merchant.ifBlank { "Expense" },
            onDismiss = { showSplitDialog = false },
            onApplySplit = { perPersonShare, splitCount, tag ->
                amount = perPersonShare.stripTrailingZeros().toPlainString()
                if (!tags.contains(tag)) {
                    tags = tags + tag
                }
                showSplitDialog = false
            }
        )
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

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Custom Tag", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g. coffee, amazon, travel") },
                    singleLine = true,
                    prefix = { Text("#") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = newTagInput.replace("#", "").trim().lowercase()
                        if (cleaned.isNotBlank()) {
                            customTags = (customTags + cleaned).distinct()
                            tags = (tags + cleaned).distinct()
                            showAddTagDialog = false
                        }
                    },
                    enabled = newTagInput.trim().isNotBlank()
                ) {
                    Text("Add & Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
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
    categories: List<Category> = emptyList(),
    onMarkAsReviewed: (categoryId: Long?, saveAsRule: Boolean) -> Unit,
    onDelete: ((Transaction) -> Unit)? = null,
    onBack: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var saveAsRule by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Misread Transaction?") },
            text = { Text("This transaction was parsed from an SMS/notification. Deleting it will permanently remove it from your records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(transaction)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
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
            
            if (!transaction.rawMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ORIGINAL MESSAGE / SMS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = transaction.rawMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Assign Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            CategoryTabSelector(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it }
            )

            if (selectedCategoryId != null) {
                val cat = categories.find { it.id == selectedCategoryId }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { saveAsRule = !saveAsRule },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveAsRule,
                            onCheckedChange = { saveAsRule = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Always categorize '${transaction.merchant}' as ${cat?.name ?: "this category"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onMarkAsReviewed(selectedCategoryId, saveAsRule) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm & Mark as Reviewed", style = MaterialTheme.typography.titleMedium)
            }

            if (onDelete != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Discard Misread SMS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
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
