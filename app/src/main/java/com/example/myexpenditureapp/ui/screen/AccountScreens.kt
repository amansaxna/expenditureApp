package com.example.myexpenditureapp.ui.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import com.example.myexpenditureapp.ui.theme.AccentSuccess
import com.example.myexpenditureapp.ui.theme.NavyDeep
import com.example.myexpenditureapp.ui.theme.NavyLight
import com.example.myexpenditureapp.ui.viewmodel.AccountViewModel
import com.example.myexpenditureapp.ui.viewmodel.BudgetViewModel
import com.example.myexpenditureapp.ui.viewmodel.TransactionViewModel
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode
import com.example.myexpenditureapp.ui.theme.MonospaceFont
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    budgetViewModel: BudgetViewModel,
    onAddAccount: () -> Unit,
    onEditAccount: (Account) -> Unit,
    onReviewTransaction: (Transaction) -> Unit
) {
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val unreviewedTransactions by transactionViewModel.unreviewedTransactions.collectAsStateWithLifecycle()
    val budgetsWithProgress by budgetViewModel.budgetsWithProgress.collectAsStateWithLifecycle()
    
    val totalBudget = budgetsWithProgress.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.budget.limitAmount) }
    val totalSpent = budgetsWithProgress.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.currentSpending) }
    val budgetLeft = totalBudget.subtract(totalSpent)
    
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            ) 
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddAccount()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        },
        containerColor = NavyDeep
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BudgetLeftCard(
                    amountLeft = budgetLeft,
                    totalBudget = totalBudget,
                    spendingPoints = listOf(0.2f, 0.4f, 0.3f, 0.6f, 0.5f, 0.8f, 0.7f)
                )
            }

            if (unreviewedTransactions.isNotEmpty()) {
                item {
                    ToReviewSection(
                        transactions = unreviewedTransactions,
                        onMarkAsReviewed = { transactionViewModel.markAsReviewed(it.id) },
                        onReviewDetail = onReviewTransaction
                    )
                }
            }

            item {
                CircularBudgetsRow(budgetsWithProgress)
            }

            item {
                Text(
                    text = "My Accounts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (accounts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No accounts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    Box(modifier = Modifier.animateItem()) {
                        AccountItem(
                            account = account,
                            onEdit = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onEditAccount(account)
                            },
                            onDelete = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                accountViewModel.deleteAccount(account)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetLeftCard(amountLeft: BigDecimal, totalBudget: BigDecimal, spendingPoints: List<Float>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = NavyLight)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Graph
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 80.dp)) {
                val width = size.width
                val height = size.height
                val path = Path()
                
                if (spendingPoints.isNotEmpty()) {
                    path.moveTo(0f, height * (1 - spendingPoints[0]))
                    for (i in 1 until spendingPoints.size) {
                        val x = i * (width / (spendingPoints.size - 1))
                        val y = height * (1 - spendingPoints[i])
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.2f),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Target budget line
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = androidx.compose.ui.geometry.Offset(0f, height * 0.5f),
                    end = androidx.compose.ui.geometry.Offset(width, height * 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "₹${amountLeft.setScale(0, RoundingMode.HALF_UP)} left",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        fontFamily = MonospaceFont
                    ),
                    color = Color.White
                )
                Text(
                    text = "out of ₹${totalBudget.setScale(0, RoundingMode.HALF_UP)} budgeted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ToReviewSection(
    transactions: List<Transaction>,
    onMarkAsReviewed: (Transaction) -> Unit,
    onReviewDetail: (Transaction) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.animateContentSize()
    ) {
        Text(
            text = "TO REVIEW",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
        
        transactions.take(3).forEach { transaction ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onReviewDetail(transaction) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyLight)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            transaction.merchant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            "₹${transaction.amount.stripTrailingZeros().toPlainString()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonospaceFont),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    TextButton(
                        onClick = { onMarkAsReviewed(transaction) },
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentSuccess),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("DONE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun CircularBudgetsRow(budgets: List<BudgetWithProgress>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(budgets) { budgetProgress ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(
                        progress = { budgetProgress.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = if (budgetProgress.progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeWidth = 6.dp
                    )
                    if (budgetProgress.category?.icon != null) {
                        Text(
                            text = budgetProgress.category.icon,
                            fontSize = 24.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = budgetProgress.category?.name ?: "Other",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                val left = budgetProgress.budget.limitAmount.subtract(budgetProgress.currentSpending)
                Text(
                    text = if (left >= BigDecimal.ZERO) "₹${left.setScale(0, RoundingMode.HALF_UP)} left" 
                           else "₹${left.negate().setScale(0, RoundingMode.HALF_UP)} over",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonospaceFont, fontSize = 10.sp),
                    color = if (left >= BigDecimal.ZERO) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AccountItem(account: Account, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyLight)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(
                    text = "₹${account.balance.stripTrailingZeros().toPlainString()}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont),
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    account: Account?,
    onSave: (Account) -> Unit,
    onDelete: (Account) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var balance by remember { mutableStateOf(account?.balance?.toString() ?: "0.0") }
    var type by remember { mutableStateOf(account?.type ?: "Bank") }
    var expandedType by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (account == null) "New Account" else "Edit Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (account != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    listOf("Bank", "Wallet", "Credit Card", "Cash").forEach { t ->
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

            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text("Current Balance") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text("₹") }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(
                        account?.copy(name = name, balance = BigDecimal(balance), type = type)
                            ?: Account(name = name, balance = BigDecimal(balance), type = type)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank() && balance.toBigDecimalOrNull() != null
            ) {
                Text("Save Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteDialog && account != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure? All associated transactions will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(account)
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CircularBudgetsRowPreview() {
    val sampleBudgets = listOf(
        BudgetWithProgress(
            budget = com.example.myexpenditureapp.data.entity.Budget(limitAmount = BigDecimal("5000"), categoryId = 1, period = "Monthly", month = 8, year = 2026),
            category = com.example.myexpenditureapp.data.entity.Category(name = "Food", icon = "🍔"),
            currentSpending = BigDecimal("1200"),
            progress = 0.24f
        ),
        BudgetWithProgress(
            budget = com.example.myexpenditureapp.data.entity.Budget(limitAmount = BigDecimal("2000"), categoryId = 2, period = "Monthly", month = 8, year = 2026),
            category = com.example.myexpenditureapp.data.entity.Category(name = "Transport", icon = "🚗"),
            currentSpending = BigDecimal("2500"),
            progress = 1.25f
        ),
        BudgetWithProgress(
            budget = com.example.myexpenditureapp.data.entity.Budget(limitAmount = BigDecimal("1000"), categoryId = 3, period = "Monthly", month = 8, year = 2026),
            category = com.example.myexpenditureapp.data.entity.Category(name = "Other", icon = null),
            currentSpending = BigDecimal("500"),
            progress = 0.5f
        )
    )
    Surface(color = NavyDeep) {
        Box(modifier = Modifier.padding(16.dp)) {
            CircularBudgetsRow(sampleBudgets)
        }
    }
}
