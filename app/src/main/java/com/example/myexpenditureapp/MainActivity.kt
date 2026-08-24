package com.example.myexpenditureapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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

data class NavigationItem(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: NavKey
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.provide(this)
        enableEdgeToEdge()
        setContent {
            MyExpenditureAppTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen() {
    val backStack = rememberNavBackStack(Route.AccountList)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val currentRoute = backStack.lastOrNull()
                
                val items = listOf(
                    NavigationItem("Accounts", Icons.Default.AccountBalance, Icons.Outlined.AccountBalance, Route.AccountList),
                    NavigationItem("Categories", Icons.Default.Category, Icons.Outlined.Category, Route.CategoryList),
                    NavigationItem("Transactions", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, Route.TransactionList),
                    NavigationItem("Budgets", Icons.Default.Payments, Icons.Outlined.Payments, Route.BudgetList),
                    NavigationItem("Analytics", Icons.Default.Analytics, Icons.Outlined.Analytics, Route.Analytics)
                )

                items.forEach { item ->
                    val isSelected = when (item.route) {
                        is Route.AccountList -> currentRoute is Route.AccountList || currentRoute is Route.AccountEdit
                        is Route.CategoryList -> currentRoute is Route.CategoryList || currentRoute is Route.CategoryEdit
                        is Route.TransactionList -> currentRoute is Route.TransactionList || currentRoute is Route.TransactionEdit
                        is Route.BudgetList -> currentRoute is Route.BudgetList || currentRoute is Route.BudgetEdit
                        is Route.Analytics -> currentRoute is Route.Analytics
                        else -> false
                    }
                    
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { 
                            if (!isSelected) {
                                backStack.clear()
                                backStack.add(item.route)
                            }
                        },
                        icon = { 
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon, 
                                contentDescription = item.label 
                            ) 
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategy = listDetailStrategy,
            modifier = Modifier.padding(innerPadding),
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
                    val viewModel: AccountViewModel = viewModel()
                    AccountListScreen(
                        viewModel = viewModel,
                        onAddAccount = { backStack.add(Route.AccountEdit()) },
                        onEditAccount = { backStack.add(Route.AccountEdit(it.id)) }
                    )
                }
                entry<Route.AccountEdit>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) { route ->
                    val viewModel: AccountViewModel = viewModel()
                    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
                    val account = accounts.find { it.id == route.accountId }
                    AccountEditScreen(
                        account = account,
                        onSave = {
                            viewModel.saveAccount(it)
                            backStack.removeLastOrNull()
                        },
                        onBack = { backStack.removeLastOrNull() }
                    )
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
                    CategoryEditScreen(
                        category = category,
                        parentId = route.parentId,
                        onSave = {
                            viewModel.saveCategory(it)
                            backStack.removeLastOrNull()
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

                    if (route.transactionId == null || transactionToEdit != null) {
                        TransactionEditScreen(
                            transaction = transactionToEdit,
                            accounts = uiState.accounts,
                            categories = uiState.categories,
                            onSave = { accId, toAccId, catId, amt, merch, type, time, id ->
                                viewModel.saveTransaction(accId, toAccId, catId, amt, merch, type, time, id)
                                backStack.removeLastOrNull()
                            },
                            onDelete = {
                                viewModel.deleteTransaction(it)
                                backStack.removeLastOrNull()
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
                    BudgetEditScreen(
                        budget = budget,
                        categories = categories,
                        onSave = {
                            viewModel.saveBudget(it)
                            backStack.removeLastOrNull()
                        },
                        onDelete = {
                            viewModel.deleteBudget(it)
                            backStack.removeLastOrNull()
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
            }
        )
    }
}
