package com.example.myexpenditureapp.ui.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Calendar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
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
import com.example.myexpenditureapp.ui.viewmodel.AccountViewModel
import com.example.myexpenditureapp.ui.viewmodel.BudgetViewModel
import com.example.myexpenditureapp.ui.viewmodel.TransactionViewModel
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode
import com.example.myexpenditureapp.ui.theme.MonospaceFont
import com.example.myexpenditureapp.ui.theme.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.myexpenditureapp.utils.formatIndian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    accountViewModel: AccountViewModel,
    transactionViewModel: TransactionViewModel,
    budgetViewModel: BudgetViewModel,
    onAddAccount: () -> Unit,
    onEditAccount: (Account) -> Unit,
    onReviewTransaction: (Transaction) -> Unit,
    onOpenSmartInbox: () -> Unit = {},
    onOpenGoals: () -> Unit = {},
    settlementViewModel: com.example.myexpenditureapp.ui.viewmodel.MonthlySettlementViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val unreviewedTransactions by transactionViewModel.unreviewedTransactions.collectAsStateWithLifecycle()
    val budgetsWithProgress by budgetViewModel.budgetsWithProgress.collectAsStateWithLifecycle()
    val txUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val categories by budgetViewModel.allCategories.collectAsStateWithLifecycle()
    val goals by com.example.myexpenditureapp.data.Graph.savingGoalRepository.getAllGoals().collectAsStateWithLifecycle(initialValue = emptyList())
    
    val unsettledSummary by settlementViewModel.unsettledMonthSummary.collectAsStateWithLifecycle()
    val isSettlementBannerDismissed by settlementViewModel.isBannerDismissed.collectAsStateWithLifecycle()
    val isSettlementSheetVisible by settlementViewModel.isBottomSheetVisible.collectAsStateWithLifecycle()
    val activeGoals by settlementViewModel.activeGoals.collectAsStateWithLifecycle()
    
    val totalBudget = budgetsWithProgress.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.budget.limitAmount) }
    val totalSpent = budgetsWithProgress.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.currentSpending) }
    val budgetLeft = totalBudget.subtract(totalSpent)
    
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var isNotificationAccessGranted by remember {
        mutableStateOf(com.example.myexpenditureapp.notifications.NotificationHelper.isNotificationListenerAccessGranted(context))
    }
    var isNotificationBannerDismissed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = com.example.myexpenditureapp.notifications.NotificationHelper.isNotificationListenerAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val smartInsights = remember(txUiState.transactions, categories, budgetsWithProgress, accounts) {
        com.example.myexpenditureapp.domain.insights.InsightsEngine.generateInsights(
            transactions = txUiState.transactions,
            categories = categories,
            budgets = budgetsWithProgress.map { it.budget },
            accounts = accounts
        )
    }

    val totalBalance = accounts.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.balance) }

    val cal = remember { java.util.Calendar.getInstance() }
    val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val daysRemaining = (daysInMonth - currentDay).coerceAtLeast(0)

    val monthStart = remember {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.DAY_OF_MONTH, 1)
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    val todayStart = remember {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    val thisMonthExpenses = txUiState.transactions.filter { it.type == "Expense" && it.timestamp >= monthStart }
    val monthlySpent = thisMonthExpenses.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
    val todaySpent = txUiState.transactions.filter { it.type == "Expense" && it.timestamp >= todayStart }
        .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
    val dailyAverage = if (currentDay > 0) {
        monthlySpent.divide(BigDecimal(currentDay), 0, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            ) 
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notification Access Banner (if not enabled and not dismissed)
            if (!isNotificationAccessGranted && !isNotificationBannerDismissed) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enable Auto-Expense Capture",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Tap to allow reading bank & UPI notifications automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { isNotificationBannerDismissed = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Month-End Settlement Banner (Model A: Interactive Wrap & Settle)
            if (unsettledSummary != null && !isSettlementBannerDismissed) {
                item {
                    com.example.myexpenditureapp.ui.component.MonthEndSettlementBanner(
                        summary = unsettledSummary!!,
                        onReviewClick = { settlementViewModel.showSettlementSheet() },
                        onDismissClick = { settlementViewModel.dismissBanner() }
                    )
                }
            }

            item {
                DashboardPacedHeroCard(
                    totalNetBalance = totalBalance,
                    monthlySpent = monthlySpent,
                    todayBurn = todaySpent,
                    dailyAverage = dailyAverage,
                    daysRemainingInMonth = daysRemaining,
                    totalBudget = totalBudget
                )
            }

            // SMART FINANCIAL DIGEST
            if (smartInsights.isNotEmpty()) {
                item {
                    com.example.myexpenditureapp.ui.component.FinancialDigestSection(insights = smartInsights)
                }
            }

            // SAVINGS GOALS & POCKETS PREVIEW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVINGS TARGETS & POCKETS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    TextButton(onClick = onOpenGoals) {
                        Text("Manage", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (goals.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onOpenGoals),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Create a saving pocket (Vacation, Gadget, Emergency)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(goals, key = { it.id }) { g ->
                            val pct = if (g.targetAmount > BigDecimal.ZERO) {
                                (g.currentAmount.divide(g.targetAmount, 2, RoundingMode.HALF_UP).multiply(BigDecimal(100))).toInt()
                            } else 0
                            Card(
                                modifier = Modifier.width(180.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onOpenGoals),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(g.icon, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(g.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "$pct% (${g.currentAmount.formatIndian(includeSymbol = true)} / ${g.targetAmount.formatIndian(includeSymbol = true)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = MonospaceFont,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (unreviewedTransactions.isNotEmpty()) {
                item {
                    ToReviewSection(
                        transactions = unreviewedTransactions,
                        onMarkAsReviewed = { transactionViewModel.markAsReviewed(it.id) },
                        onDeleteUnreviewed = { transactionViewModel.deleteTransaction(it) },
                        onClearAllUnreviewed = { transactionViewModel.deleteAllUnreviewedTransactions() },
                        onReviewDetail = onReviewTransaction,
                        onOpenSmartInbox = onOpenSmartInbox
                    )
                }
            }

            item {
                CircularBudgetsRow(budgetsWithProgress)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddAccount()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No accounts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(onClick = onAddAccount) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Account")
                        }
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

        // Month-End Settlement Bottom Sheet Modal
        if (isSettlementSheetVisible && unsettledSummary != null) {
            com.example.myexpenditureapp.ui.component.MonthEndSettlementBottomSheet(
                summary = unsettledSummary!!,
                activeGoals = activeGoals,
                onDismissRequest = { settlementViewModel.hideSettlementSheet() },
                onSweepToGoal = { goalId, amt -> settlementViewModel.sweepToGoal(goalId, amt) },
                onRolloverToBudget = { amt -> settlementViewModel.rolloverToBudget(amt) },
                onCleanSlate = { amt -> settlementViewModel.settleCleanSlate(amt) }
            )
        }
    }
}

@Composable
fun DashboardPacedHeroCard(
    totalNetBalance: BigDecimal,
    monthlySpent: BigDecimal,
    todayBurn: BigDecimal,
    dailyAverage: BigDecimal,
    daysRemainingInMonth: Int,
    totalBudget: BigDecimal,
    modifier: Modifier = Modifier
) {
    val isUnderPaced = if (totalBudget > BigDecimal.ZERO) {
        val expectedDailyBudget = totalBudget.divide(BigDecimal(daysRemainingInMonth + 1).coerceAtLeast(BigDecimal.ONE), 2, RoundingMode.HALF_UP)
        dailyAverage <= expectedDailyBudget
    } else true

    val pacingColor = if (isUnderPaced) IncomeGreen else AmberWarning

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Net Balance Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL LIQUID BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pacingColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, pacingColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (isUnderPaced) "On Track" else "Pacing High",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = pacingColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Figure (Monospaced Tabular)
            Text(
                text = totalNetBalance.formatIndian(includeSymbol = true, includeDecimals = false),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = MonospaceFont,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // 3-Metric Precision Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Spent This Month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        monthlySpent.formatIndian(includeSymbol = true, includeDecimals = false),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = MonospaceFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Column {
                    Text(
                        "Today's Burn",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        todayBurn.formatIndian(includeSymbol = true, includeDecimals = false),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = MonospaceFont,
                            fontWeight = FontWeight.Bold,
                            color = if (todayBurn > BigDecimal.ZERO) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Days Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${daysRemainingInMonth}d",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = MonospaceFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ToReviewSection(
    transactions: List<Transaction>,
    onMarkAsReviewed: (Transaction) -> Unit,
    onDeleteUnreviewed: (Transaction) -> Unit,
    onClearAllUnreviewed: () -> Unit,
    onReviewDetail: (Transaction) -> Unit,
    onOpenSmartInbox: () -> Unit = {}
) {
    var showDismissAllDialog by remember { mutableStateOf(false) }

    if (showDismissAllDialog) {
        AlertDialog(
            onDismissRequest = { showDismissAllDialog = false },
            title = { Text("Dismiss All Pending?") },
            text = { Text("Are you sure you want to discard all ${transactions.size} pending unreviewed transactions? This will revert their temporary impact on your account balance.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDismissAllDialog = false
                        onClearAllUnreviewed()
                    }
                ) {
                    Text("Dismiss All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDismissAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenSmartInbox)
            ) {
                Text(
                    text = "SMART INBOX",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldAccent.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${transactions.size} pending",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onOpenSmartInbox,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        "View All",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (transactions.size > 1) {
                    TextButton(
                        onClick = { showDismissAllDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Dismiss All",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        transactions.take(3).forEach { transaction ->
            SwipeablePendingTransactionItem(
                transaction = transaction,
                onMarkAsReviewed = { onMarkAsReviewed(transaction) },
                onDeleteUnreviewed = { onDeleteUnreviewed(transaction) },
                onReviewDetail = { onReviewDetail(transaction) }
            )
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
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = budgetProgress.category?.name ?: "Other",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                val left = budgetProgress.budget.limitAmount.subtract(budgetProgress.currentSpending)
                Text(
                    text = if (left >= BigDecimal.ZERO) "${left.formatIndian(includeSymbol = true)} left" 
                           else "${left.negate().formatIndian(includeSymbol = true)} over",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonospaceFont, fontSize = 10.sp),
                    color = if (left >= BigDecimal.ZERO) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AccountItem(account: Account, onEdit: () -> Unit, onDelete: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "accountPressScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onEdit
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (account.type.lowercase()) {
                            "bank" -> Icons.Default.AccountBalance
                            "credit card", "credit" -> Icons.Default.CreditCard
                            else -> Icons.Default.AccountBalanceWallet
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = account.type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = account.balance.formatIndian(includeSymbol = true, includeDecimals = false),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonospaceFont, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.padding(16.dp)) {
            CircularBudgetsRow(sampleBudgets)
        }
    }
}
