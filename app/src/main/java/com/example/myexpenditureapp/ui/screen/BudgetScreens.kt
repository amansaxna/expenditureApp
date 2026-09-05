package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.data.entity.Budget
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.domain.model.BudgetWithProgress
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.ui.viewmodel.BudgetViewModel
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
                    containerColor = NavyDeep,
                    titleContentColor = OffWhite
                )
            ) 
        },
        containerColor = NavyDeep,
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddBudget,
                containerColor = AccentVibrant,
                contentColor = Color.White,
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
                                tint = Slate.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "No budgets for this month",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OffWhite
                            )
                            Text(
                                "Plan your spending for ${getMonthName(selectedMonth)} $selectedYear",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEach { month ->
                val isSelected = month == selectedMonth
                FilterChip(
                    selected = isSelected,
                    onClick = { onMonthYearSelected(month, selectedYear) },
                    label = { Text(getMonthName(month, true)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentVibrant,
                        selectedLabelColor = Color.White,
                        containerColor = NavyLight,
                        labelColor = Slate
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) AccentVibrant else NavyAccent
                    )
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(
            label = "PASSED",
            count = passed,
            color = AccentSuccess,
            modifier = Modifier.weight(1f)
        )
        StatusCard(
            label = "FAILED",
            count = failed,
            color = AccentError,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavyLight),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    color = OffWhite,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun BudgetCard(
    budgetProgress: BudgetWithProgress,
    onClick: () -> Unit
) {
    val isOver = budgetProgress.progress > 1f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyLight),
        border = BorderStroke(1.dp, if (isOver) AccentError.copy(alpha = 0.3f) else NavyAccent.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budgetProgress.category?.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )
                Surface(
                    color = if (isOver) AccentError.copy(alpha = 0.1f) else AccentSuccess.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isOver) "FAILED" else "PASSED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isOver) AccentError else AccentSuccess
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { budgetProgress.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (isOver) AccentError else AccentVibrant,
                trackColor = NavyLighter
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SPENT", style = MaterialTheme.typography.labelSmall, color = Slate)
                    Text(
                        "₹${budgetProgress.currentSpending.setScale(0, RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonospaceFont),
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BUDGET", style = MaterialTheme.typography.labelSmall, color = Slate)
                    Text(
                        "₹${budgetProgress.budget.limitAmount.setScale(0, RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonospaceFont),
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                }
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
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentError)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDeep,
                    titleContentColor = OffWhite,
                    navigationIconContentColor = OffWhite
                )
            )
        },
        containerColor = NavyDeep
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
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite,
                        focusedBorderColor = AccentVibrant,
                        unfocusedBorderColor = NavyAccent,
                        focusedLabelColor = AccentVibrant,
                        unfocusedLabelColor = Slate
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = NavyLight
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name, color = OffWhite) },
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
                prefix = { Text("₹") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedBorderColor = AccentVibrant,
                    unfocusedBorderColor = NavyAccent,
                    focusedLabelColor = AccentVibrant,
                    unfocusedLabelColor = Slate
                )
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
                                month = budget?.month ?: 0, // ViewModel will handle this for new budgets
                                year = budget?.year ?: 0
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = selectedCategoryId != null && limitAmount.toBigDecimalOrNull() != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentVibrant,
                    contentColor = Color.White,
                    disabledContainerColor = NavyLight,
                    disabledContentColor = Slate
                )
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
            containerColor = NavyLight,
            titleContentColor = OffWhite,
            textContentColor = OffWhite,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(budget)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentError)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = OffWhite)) {
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
