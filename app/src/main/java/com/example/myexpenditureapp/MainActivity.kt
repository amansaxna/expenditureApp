package com.example.myexpenditureapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
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
import com.example.myexpenditureapp.ui.component.ForexChartAnimation
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
    var isInitialLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(850)
        isInitialLoading = false
    }

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
            "subscriptions" -> {
                if (backStack.lastOrNull() !is Route.SubscriptionList) {
                    backStack.add(Route.SubscriptionList)
                }
            }
        }
    }

    MyExpenditureAppTheme(themeMode = themeMode) {
        val haptic = LocalHapticFeedback.current
        val currentRoute = backStack.lastOrNull()
        val topLevelRoutes = remember {
            listOf(Route.AccountList, Route.TransactionList, Route.Analytics, Route.Settings)
        }
        val currentTopLevelIndex = topLevelRoutes.indexOfFirst {
            when (it) {
                is Route.AccountList -> currentRoute is Route.AccountList
                is Route.TransactionList -> currentRoute is Route.TransactionList
                is Route.Analytics -> currentRoute is Route.Analytics
                is Route.Settings -> currentRoute is Route.Settings
                else -> false
            }
        }
        val isTopLevelRoute = currentTopLevelIndex >= 0

        var totalDragX by remember { mutableFloatStateOf(0f) }

        val swipeModifier = if (isTopLevelRoute) {
            Modifier.pointerInput(currentTopLevelIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val threshold = 65.dp.toPx()
                        if (totalDragX < -threshold && currentTopLevelIndex < topLevelRoutes.lastIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            backStack.clear()
                            backStack.add(topLevelRoutes[currentTopLevelIndex + 1])
                        } else if (totalDragX > threshold && currentTopLevelIndex > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            backStack.clear()
                            backStack.add(topLevelRoutes[currentTopLevelIndex - 1])
                        }
                        totalDragX = 0f
                    },
                    onDragCancel = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            }
        } else {
            // Inner pages (Add/Edit Transaction, Account Edit, etc.): Swipe right to go back
            Modifier.pointerInput(currentRoute) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val threshold = 55.dp.toPx()
                        if (totalDragX > threshold) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            backStack.removeLastOrNull()
                        }
                        totalDragX = 0f
                    },
                    onDragCancel = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(swipeModifier)
        ) {
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

            AnimatedVisibility(
                visible = isInitialLoading,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ForexChartAnimation(
                            size = 180.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SpendZen",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Loading financial vaults...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
    val haptic = LocalHapticFeedback.current
    val currentIndex = items.indexOfFirst { item ->
        when (item.route) {
            is Route.AccountList -> currentRoute is Route.AccountList
            is Route.TransactionList -> currentRoute is Route.TransactionList
            is Route.Analytics -> currentRoute is Route.Analytics
            is Route.Settings -> currentRoute is Route.Settings
            else -> false
        }
    }
    var navDragX by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(currentIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { navDragX = 0f },
                    onDragEnd = {
                        val threshold = 50.dp.toPx()
                        if (navDragX < -threshold && currentIndex >= 0 && currentIndex < items.lastIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onItemSelected(items[currentIndex + 1].route)
                        } else if (navDragX > threshold && currentIndex > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onItemSelected(items[currentIndex - 1].route)
                        }
                        navDragX = 0f
                    },
                    onDragCancel = { navDragX = 0f },
                    onHorizontalDrag = { _, dragAmount -> navDragX += dragAmount }
                )
            },
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
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navScale"
                )
                val pillColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                    label = "pillColor"
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pillColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
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
