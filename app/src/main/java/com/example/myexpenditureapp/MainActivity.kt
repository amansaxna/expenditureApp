package com.example.myexpenditureapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ripple
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.ui.navigation.Route
import com.example.myexpenditureapp.ui.screen.*
import com.example.myexpenditureapp.ui.theme.MyExpenditureAppTheme
import com.example.myexpenditureapp.ui.viewmodel.*
import com.example.myexpenditureapp.notifications.NotificationHelper
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

data class NavigationItem(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: NavKey
)

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsReceived = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val smsRead = permissions[Manifest.permission.READ_SMS] ?: false
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }
        
        if (smsReceived && smsRead) {
            // Permissions granted
        }
        if (notificationsGranted) {
            NotificationHelper.createNotificationChannels(this)
            NotificationHelper.scheduleWorkers(this)
        }
    }

    private val pendingShortcutAction = mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.provide(this)
        enableEdgeToEdge()
        
        val action = intent?.getStringExtra("shortcut_action") 
            ?: intent?.getStringExtra("ACTION") 
            ?: if (intent?.action == "com.example.myexpenditureapp.QUICK_ADD") "quick_add" else null
        pendingShortcutAction.value = action

        NotificationHelper.createNotificationChannels(this)
        NotificationHelper.scheduleWorkers(this)

        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        requestPermissionLauncher.launch(permissions.toTypedArray())

        setContent {
            MainScreen(shortcutAction = pendingShortcutAction.value)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.getStringExtra("shortcut_action") 
            ?: intent.getStringExtra("ACTION") 
            ?: if (intent.action == "com.example.myexpenditureapp.QUICK_ADD") "quick_add" else null
        pendingShortcutAction.value = action
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(shortcutAction: String? = null) {
    val themeViewModel: ThemeViewModel = viewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(Route.AccountList)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    )
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(shortcutAction) {
        when (shortcutAction) {
            "quick_add" -> {
                if (backStack.lastOrNull() !is Route.TransactionEdit) {
                    backStack.add(Route.TransactionEdit())
                }
            }
            "analytics" -> {
                backStack.clear()
                backStack.add(Route.Analytics)
            }
            "goals" -> {
                if (backStack.lastOrNull() !is Route.GoalList) {
                    backStack.add(Route.GoalList)
                }
            }
            "transactions" -> {
                backStack.clear()
                backStack.add(Route.TransactionList)
            }
        }
    }

    MyExpenditureAppTheme(themeMode = themeMode) {
        val haptic = LocalHapticFeedback.current
        val currentRoute = backStack.lastOrNull()
        val isTopLevelRoute = currentRoute is Route.AccountList ||
                currentRoute is Route.TransactionList ||
                currentRoute is Route.Analytics ||
                currentRoute is Route.Settings

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (isTopLevelRoute) {
                    AppBottomNavBar(
                        currentRoute = currentRoute,
                        onItemSelected = { route ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            backStack.clear()
                            backStack.add(route)
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategy = listDetailStrategy,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                entryProvider = entryProvider {
                    entry<Route.AccountList>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select an account to edit")
                                }
                            }
                        )
                    ) {
                        val accountViewModel: AccountViewModel = viewModel()
                        val transactionViewModel: TransactionViewModel = viewModel()
                        val budgetViewModel: BudgetViewModel = viewModel()
                        AccountListScreen(
                            accountViewModel = accountViewModel,
                            transactionViewModel = transactionViewModel,
                            budgetViewModel = budgetViewModel,
                            onAddAccount = { backStack.add(Route.AccountEdit()) },
                            onEditAccount = { backStack.add(Route.AccountEdit(it.id)) },
                            onReviewTransaction = { backStack.add(Route.TransactionReview(it.id)) },
                            onOpenSmartInbox = { backStack.add(Route.SmartInbox) },
                            onOpenGoals = { backStack.add(Route.GoalList) }
                        )
                    }
                    entry<Route.AccountEdit>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { route ->
                        val viewModel: AccountViewModel = viewModel()
                        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
                        val account = accounts.find { it.id == route.accountId }
                        
                        LaunchedEffect(viewModel.eventFlow) {
                            viewModel.eventFlow.collect { event ->
                                when (event) {
                                    is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                                    is UiEvent.Success -> backStack.removeLastOrNull()
                                }
                            }
                        }

                        AccountEditScreen(
                            account = account,
                            onSave = {
                                viewModel.saveAccount(it)
                            },
                            onDelete = {
                                viewModel.deleteAccount(it)
                            },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<Route.TransactionReview>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { route ->
                        val viewModel: TransactionViewModel = viewModel()
                        val categoryViewModel: CategoryViewModel = viewModel()
                        val categories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
                        var transaction by remember(route.transactionId) { mutableStateOf<Transaction?>(null) }
                        LaunchedEffect(route.transactionId) {
                            transaction = viewModel.getTransactionById(route.transactionId)
                        }
                        
                        transaction?.let {
                            TransactionReviewScreen(
                                transaction = it,
                                categories = categories,
                                onMarkAsReviewed = { catId, saveAsRule ->
                                    viewModel.reviewTransaction(it.id, catId, saveAsRule)
                                    backStack.removeLastOrNull()
                                },
                                onDelete = { tx ->
                                    viewModel.deleteTransaction(tx)
                                    backStack.removeLastOrNull()
                                },
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                    entry<Route.CategoryList>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select a category to edit")
                                }
                            }
                        )
                    ) {
                        val viewModel: CategoryViewModel = viewModel()
                        CategoryListScreen(
                            viewModel = viewModel,
                            onAddCategory = { backStack.add(Route.CategoryEdit(parentId = it)) },
                            onEditCategory = { backStack.add(Route.CategoryEdit(categoryId = it.id)) }
                        )
                    }
                    entry<Route.CategoryEdit>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { route ->
                        val viewModel: CategoryViewModel = viewModel()
                        val categories by viewModel.allCategories.collectAsStateWithLifecycle()
                        val category = categories.find { it.id == route.categoryId }

                        LaunchedEffect(viewModel.eventFlow) {
                            viewModel.eventFlow.collect { event ->
                                when (event) {
                                    is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                                    is UiEvent.Success -> backStack.removeLastOrNull()
                                }
                            }
                        }

                        CategoryEditScreen(
                            category = category,
                            parentId = route.parentId,
                            onSave = {
                                viewModel.saveCategory(it)
                            },
                            onDelete = {
                                viewModel.deleteCategory(it)
                            },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<Route.TransactionList>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select a transaction to edit")
                                }
                            }
                        )
                    ) {
                        val viewModel: TransactionViewModel = viewModel()
                        TransactionListScreen(
                            viewModel = viewModel,
                            onAddTransaction = { backStack.add(Route.TransactionEdit()) },
                            onEditTransaction = { backStack.add(Route.TransactionEdit(it.id)) }
                        )
                    }
                    entry<Route.TransactionEdit>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { route ->
                        val viewModel: TransactionViewModel = viewModel()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        
                        var transactionToEdit by remember(route.transactionId) { mutableStateOf<Transaction?>(null) }
                        LaunchedEffect(route.transactionId) {
                            if (route.transactionId != null) {
                                transactionToEdit = viewModel.getTransactionById(route.transactionId)
                            }
                        }

                        LaunchedEffect(viewModel.eventFlow) {
                            viewModel.eventFlow.collect { event ->
                                when (event) {
                                    is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                                    is UiEvent.Success -> backStack.removeLastOrNull()
                                }
                            }
                        }

                        if (route.transactionId == null || transactionToEdit != null) {
                            TransactionEditScreen(
                                transaction = transactionToEdit,
                                accounts = uiState.accounts,
                                categories = uiState.categories,
                                onSave = { accId, toAccId, catId, amt, merch, type, time, id, tags, saveAsRule ->
                                    viewModel.saveTransaction(accId, toAccId, catId, amt, merch, type, time, id, tags, saveAsRule)
                                },
                                onDelete = {
                                    viewModel.deleteTransaction(it)
                                },
                                onBack = { backStack.removeLastOrNull() }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    entry<Route.BudgetList>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Select a budget to edit")
                                }
                            }
                        )
                    ) {
                        val viewModel: BudgetViewModel = viewModel()
                        BudgetListScreen(
                            viewModel = viewModel,
                            onAddBudget = { backStack.add(Route.BudgetEdit()) },
                            onEditBudget = { backStack.add(Route.BudgetEdit(it.id)) }
                        )
                    }
                    entry<Route.BudgetEdit>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { route ->
                        val viewModel: BudgetViewModel = viewModel()
                        val budgets by viewModel.budgetsWithProgress.collectAsStateWithLifecycle()
                        val categories by viewModel.allCategories.collectAsStateWithLifecycle()
                        val budget = budgets.find { it.budget.id == route.budgetId }?.budget

                        LaunchedEffect(viewModel.eventFlow) {
                            viewModel.eventFlow.collect { event ->
                                when (event) {
                                    is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                                    is UiEvent.Success -> backStack.removeLastOrNull()
                                }
                            }
                        }

                        BudgetEditScreen(
                            budget = budget,
                            categories = categories,
                            onSave = {
                                viewModel.saveBudget(it)
                            },
                            onDelete = {
                                viewModel.deleteBudget(it)
                            },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<Route.Analytics>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        val viewModel: AnalyticsViewModel = viewModel()
                        AnalyticsScreen(viewModel = viewModel)
                    }
                    entry<Route.Settings>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        val themeViewModel: ThemeViewModel = viewModel()
                        SettingsScreen(
                            viewModel = themeViewModel,
                            onNavigateToRules = { backStack.add(Route.AutoCategoryRuleList) },
                            onNavigateToGoals = { backStack.add(Route.GoalList) },
                            onNavigateToCategories = { backStack.add(Route.CategoryList) },
                            onNavigateToSubscriptions = { backStack.add(Route.SubscriptionList) }
                        )
                    }
                    entry<Route.GoalList>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        val goalViewModel: GoalViewModel = viewModel()
                        GoalListScreen(
                            viewModel = goalViewModel,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<Route.AutoCategoryRuleList>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        val ruleViewModel: AutoCategoryRuleViewModel = viewModel()
                        AutoCategoryRuleListScreen(
                            viewModel = ruleViewModel,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<Route.SubscriptionList>(
                        metadata = ListDetailSceneStrategy.listPane()
                    ) {
                        SubscriptionListScreen(
                            onNavigateBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<Route.SmartInbox>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) {
                        val transactionViewModel: TransactionViewModel = viewModel()
                        SmartInboxScreen(
                            viewModel = transactionViewModel,
                            onReviewTransaction = { backStack.add(Route.TransactionReview(it.id)) },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun AppBottomNavBar(
    currentRoute: NavKey?,
    onItemSelected: (NavKey) -> Unit
) {
    val items = remember {
        listOf(
            NavigationItem("Home", Icons.Default.Dashboard, Icons.Outlined.Dashboard, Route.AccountList),
            NavigationItem("Transactions", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, Route.TransactionList),
            NavigationItem("Analytics", Icons.Default.Analytics, Icons.Outlined.Analytics, Route.Analytics),
            NavigationItem("Settings", Icons.Default.Settings, Icons.Outlined.Settings, Route.Settings)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = when (item.route) {
                    is Route.AccountList -> currentRoute is Route.AccountList
                    is Route.TransactionList -> currentRoute is Route.TransactionList
                    is Route.Analytics -> currentRoute is Route.Analytics
                    is Route.Settings -> currentRoute is Route.Settings
                    else -> false
                }
                
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    label = "navIconColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    label = "navTextColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 24.dp)
                        ) {
                            if (!isSelected) onItemSelected(item.route)
                        }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
