package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.ui.viewmodel.BudgetViewModel
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    viewModel: BudgetViewModel,
    onAddBudget: () -> Unit,
    onEditBudget: (Budget) -> Unit
) {
    val budgets by viewModel.budgetsWithProgress.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val passedCount by viewModel.passedBudgetsCount.collectAsStateWithLifecycle()
    val failedCount by viewModel.failedBudgetsCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "BUDGET PLANNER", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            ) 
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddBudget,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthPicker(
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                onMonthYearSelected = { m, y -> viewModel.onMonthYearChanged(m, y) }
            )

            MonthlyStatusSummary(
                passed = passedCount,
                failed = failedCount
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (budgets.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillParentMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "No budgets for this month",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Plan your spending for ${getMonthName(selectedMonth)} $selectedYear",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(budgets) { budgetProgress ->
                        BudgetCard(
                            budgetProgress = budgetProgress,
                            onClick = { onEditBudget(budgetProgress.budget) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthPicker(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthYearSelected: (Int, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val months = (1..12).toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEach { month ->
                val isSelected = month == selectedMonth
                FilterChip(
                    selected = isSelected,
                    onClick = { onMonthYearSelected(month, selectedYear) },
                    label = { Text(getMonthName(month, true), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

@Composable
fun MonthlyStatusSummary(passed: Int, failed: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(
            label = "ON TRACK",
            count = passed,
            color = IncomeGreen,
            modifier = Modifier.weight(1f)
        )
        StatusCard(
            label = "OVER LIMIT",
            count = failed,
            color = ExpenseRed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count Categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetCard(
    budgetProgress: BudgetWithProgress,
    onClick: () -> Unit
) {
    val pct = (budgetProgress.progress * 100f).toInt()
    val isOver = budgetProgress.progress > 1f
    val isWarning = budgetProgress.progress in 0.8f..1f

    val gaugeColor = when {
        isOver -> ExpenseRed
        isWarning -> AmberWarning
        else -> IncomeGreen
    }

    val remaining = budgetProgress.budget.limitAmount.subtract(budgetProgress.currentSpending)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, gaugeColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = gaugeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, gaugeColor.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (budgetProgress.category?.icon != null) {
                                Text(budgetProgress.category.icon, fontSize = 20.sp)
                            } else {
                                Icon(Icons.Default.Category, contentDescription = null, tint = gaugeColor, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budgetProgress.category?.name ?: "General",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (remaining >= BigDecimal.ZERO) "${remaining.formatIndian()} left" 
                                   else "${remaining.negate().formatIndian()} exceeded",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonospaceFont),
                            color = if (remaining >= BigDecimal.ZERO) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
                        )
                    }
                }

                Surface(
                    color = gaugeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, gaugeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$pct%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = gaugeColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = budgetProgress.progress.coerceIn(0f, 1f),
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "budgetProgress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = gaugeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ${budgetProgress.currentSpending.formatIndian()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonospaceFont),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Limit: ${budgetProgress.budget.limitAmount.formatIndian()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonospaceFont),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditScreen(
    budget: Budget?,
    categories: List<Category>,
    onSave: (Budget) -> Unit,
    onDelete: (Budget) -> Unit,
    onBack: () -> Unit
) {
    var limitAmount by remember { mutableStateOf(budget?.limitAmount?.toString() ?: "") }
    var selectedCategoryId by remember { mutableStateOf(budget?.categoryId) }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (budget == null) "NEW BUDGET" else "EDIT BUDGET") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (budget != null) {
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
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("CATEGORY") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                selectedCategoryId = category.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = limitAmount,
                onValueChange = { limitAmount = it },
                label = { Text("MONTHLY LIMIT") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedCategoryId?.let { catId ->
                        onSave(
                            Budget(
                                id = budget?.id ?: 0,
                                categoryId = catId,
                                limitAmount = BigDecimal(limitAmount),
                                period = "Monthly",
                                month = budget?.month ?: 0,
                                year = budget?.year ?: 0
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = selectedCategoryId != null && limitAmount.toBigDecimalOrNull() != null
            ) {
                Text("SAVE BUDGET", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (showDeleteDialog && budget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to remove this budget?") },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(budget)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

private fun getMonthName(month: Int, short: Boolean = false): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, month - 1)
    return cal.getDisplayName(
        Calendar.MONTH, 
        if (short) Calendar.SHORT else Calendar.LONG, 
        Locale.getDefault()
    ) ?: ""
}
