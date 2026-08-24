package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }
    var quickEditTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search transactions...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (uiState.filterAccountId != null || uiState.filterCategoryId != null)
                                MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
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
                        "No transactions found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(uiState.transactions, key = { it.id }) { transaction ->
                    Box(modifier = Modifier.animateItem()) {
                        TransactionItem(
                            transaction = transaction,
                            accounts = uiState.accounts,
                            categories = uiState.categories,
                            onClick = { onEditTransaction(transaction) },
                            onLongClick = { quickEditTransaction = transaction }
                        )
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
    onUpdateAmount: (Double) -> Unit,
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
                .padding(bottom = 48.dp)
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
            
            var amountText by remember { mutableStateOf(transaction.amount.toString()) }
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹ ") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        amountText.toDoubleOrNull()?.let { onUpdateAmount(it) }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Update Amount")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Change Category",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = transaction.categoryId == null,
                    onClick = { onUpdateCategory(null) },
                    label = { Text("None") }
                )
                categories.take(8).forEach { category ->
                    FilterChip(
                        selected = transaction.categoryId == category.id,
                        onClick = { onUpdateCategory(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }
        }
    }
}

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
            Column {
                Text("Account")
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                    FilterChip(
                        selected = selectedAccountId == null,
                        onClick = { onAccountSelected(null) },
                        label = { Text("All") }
                    )
                    accounts.forEach { account ->
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = selectedAccountId == account.id,
                            onClick = { onAccountSelected(account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Category")
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All") }
                    )
                    categories.forEach { category ->
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { onCategorySelected(category.id) },
                            label = { Text(category.name) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    accounts: List<Account>,
    categories: List<Category>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val account = accounts.find { it.id == transaction.accountId }
    val toAccount = accounts.find { it.id == transaction.toAccountId }
    val category = categories.find { it.id == transaction.categoryId }
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon or Type Icon
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = when (transaction.type) {
                    "Income" -> IncomeGreen.copy(alpha = 0.1f)
                    "Expense" -> ExpenseRed.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (transaction.type) {
                            "Income" -> Icons.Default.TrendingUp
                            "Expense" -> Icons.Default.TrendingDown
                            else -> Icons.Default.SwapHoriz
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when (transaction.type) {
                            "Income" -> IncomeGreen
                            "Expense" -> ExpenseRed
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = if (transaction.type == "Transfer") {
                        "${account?.name} -> ${toAccount?.name}"
                    } else {
                        "${category?.name ?: "Uncategorized"} • ${account?.name}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type == "Expense") "-" else "+"}₹${transaction.amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        "Income" -> IncomeGreen
                        "Expense" -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = sdf.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    transaction: Transaction? = null,
    accounts: List<Account>,
    categories: List<Category>,
    onSave: (accountId: Long, toAccountId: Long?, categoryId: Long?, amount: Double, merchant: String, type: String, timestamp: Long, id: Long) -> Unit,
    onDelete: (Transaction) -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(transaction?.merchant ?: "") }
    var type by remember { mutableStateOf(transaction?.type ?: "Expense") }
    var accountId by remember { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by remember { mutableStateOf(transaction?.toAccountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var categoryId by remember { mutableStateOf(transaction?.categoryId) }
    var timestamp by remember { mutableStateOf(transaction?.timestamp ?: System.currentTimeMillis()) }

    var expandedAccount by remember { mutableStateOf(false) }
    var expandedToAccount by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
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
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                singleLine = true
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
                                "Income" -> Icons.Default.TrendingUp
                                "Expense" -> Icons.Default.TrendingDown
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

            // Category Dropdown
            val selectedCategory = categories.find { it.id == categoryId }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "No Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            categoryId = null
                            expandedCategory = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                categoryId = category.id
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
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
