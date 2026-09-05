package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.ui.component.BarChart
import com.example.myexpenditureapp.ui.component.LineChart
import com.example.myexpenditureapp.ui.component.PieChart
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.ui.viewmodel.AnalyticsViewModel
import com.example.myexpenditureapp.data.entity.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycle(initialValue = emptyMap())
    val weeklySpending by viewModel.weeklySpending.collectAsStateWithLifecycle(initialValue = emptyMap())
    val monthlyComparison by viewModel.monthlyComparison.collectAsStateWithLifecycle(initialValue = emptyMap())
    val spendingTrend by viewModel.spendingTrend.collectAsStateWithLifecycle(initialValue = emptyMap())
    val projectedTrend by viewModel.projectedTrend.collectAsStateWithLifecycle(initialValue = emptyMap())
    val kpis by viewModel.kpis.collectAsStateWithLifecycle(initialValue = emptyList())
    val performance by viewModel.monthlyPerformance.collectAsStateWithLifecycle(initialValue = null)
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    val context = androidx.compose.ui.platform.LocalContext.current

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "FINANCIAL INSIGHTS", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ) 
                },
                actions = {
                    IconButton(onClick = { viewModel.exportTransactionsToCsv(context) }) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Export CSV",
                            tint = OffWhite
                        )
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = "Filters",
                            tint = if (filterState.selectedAccountId != null || filterState.selectedCategoryId != null) 
                                AccentVibrant else OffWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NavyDeep,
                    titleContentColor = OffWhite,
                    actionIconContentColor = OffWhite
                )
            ) 
        },
        containerColor = NavyDeep
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                AnalyticsMonthPicker(
                    selectedMonth = filterState.month,
                    selectedYear = filterState.year,
                    onMonthYearSelected = { m, y -> viewModel.onMonthYearChange(m, y) }
                )
            }

            item {
                performance?.let { 
                    MonthlyPerformanceCard(it)
                }
            }

            item {
                KPISection(kpis)
            }

            item {
                SectionHeader("SPENDING TREND", "Daily insights")
                CompactChartCard {
                    LineChart(
                        data = spendingTrend,
                        projectedData = projectedTrend,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            item {
                SectionHeader("CATEGORY MIX", "Distribution")
                CompactChartCard {
                    PieChart(
                        data = categorySpending.mapKeys { it.key?.name ?: "Other" },
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            item {
                SectionHeader("WEEKLY SPENDING", "Selected month")
                CompactChartCard {
                    BarChart(
                        data = weeklySpending,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            
            item {
                SectionHeader("HISTORY", "Last 6 months")
                CompactChartCard {
                    BarChart(
                        data = monthlyComparison,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            item {
                SectionHeader("INTENSITY MAP", "Monthly activity")
                CompactChartCard {
                    SimpleCalendarView(viewModel)
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = NavyLight,
            contentColor = OffWhite,
            dragHandle = { BottomSheetDefaults.DragHandle(color = NavyAccent) }
        ) {
            AnalyticsFilters(
                filterState = filterState,
                accounts = accounts,
                categories = categories,
                onAccountSelect = { viewModel.onAccountFilterChange(it) },
                onCategorySelect = { viewModel.onCategoryFilterChange(it) },
                onTypeSelect = { viewModel.onTypeFilterChange(it) },
                onSourceSelect = { viewModel.onSourceFilterChange(it) }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun KPISection(kpis: List<com.example.myexpenditureapp.ui.viewmodel.AnalyticsViewModel.KPI>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            kpis.take(2).forEach { kpi ->
                KPICard(kpi, modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            kpis.drop(2).forEach { kpi ->
                KPICard(kpi, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun KPICard(kpi: com.example.myexpenditureapp.ui.viewmodel.AnalyticsViewModel.KPI, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyAccent.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = NavyLight,
            contentColor = OffWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = kpi.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Slate,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = kpi.value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonospaceFont),
                fontWeight = FontWeight.ExtraBold,
                color = OffWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = kpi.secondary,
                style = MaterialTheme.typography.labelSmall,
                color = AccentSuccess,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsFilters(
    filterState: com.example.myexpenditureapp.ui.viewmodel.AnalyticsUiState,
    accounts: List<com.example.myexpenditureapp.data.entity.Account>,
    categories: List<com.example.myexpenditureapp.data.entity.Category>,
    onAccountSelect: (Long?) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onTypeSelect: (String?) -> Unit,
    onSourceSelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "FILTERS", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = OffWhite
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ACCOUNTS", style = MaterialTheme.typography.labelSmall, color = Slate)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.selectedAccountId == null,
                    onClick = { onAccountSelect(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentVibrant,
                        selectedLabelColor = Color.White,
                        containerColor = NavyLighter,
                        labelColor = Slate
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterState.selectedAccountId == null, borderColor = NavyAccent)
                )
                accounts.forEach { account ->
                    FilterChip(
                        selected = filterState.selectedAccountId == account.id,
                        onClick = { onAccountSelect(account.id) },
                        label = { Text(account.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentVibrant,
                            selectedLabelColor = Color.White,
                            containerColor = NavyLighter,
                            labelColor = Slate
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterState.selectedAccountId == account.id, borderColor = NavyAccent)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TYPE", style = MaterialTheme.typography.labelSmall, color = Slate)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, "Expense", "Income", "Transfer").forEach { type ->
                    FilterChip(
                        selected = filterState.selectedTransactionType == type,
                        onClick = { onTypeSelect(type) },
                        label = { Text(type ?: "All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentVibrant,
                            selectedLabelColor = Color.White,
                            containerColor = NavyLighter,
                            labelColor = Slate
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterState.selectedTransactionType == type, borderColor = NavyAccent)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SOURCE", style = MaterialTheme.typography.labelSmall, color = Slate)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, "Manual", "SMS").forEach { source ->
                    FilterChip(
                        selected = filterState.selectedSource == source,
                        onClick = { onSourceSelect(source) },
                        label = { Text(source ?: "All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentVibrant,
                            selectedLabelColor = Color.White,
                            containerColor = NavyLighter,
                            labelColor = Slate
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterState.selectedSource == source, borderColor = NavyAccent)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CATEGORIES", style = MaterialTheme.typography.labelSmall, color = Slate)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.selectedCategoryId == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentVibrant,
                        selectedLabelColor = Color.White,
                        containerColor = NavyLighter,
                        labelColor = Slate
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterState.selectedCategoryId == null, borderColor = NavyAccent)
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = filterState.selectedCategoryId == category.id,
                        onClick = { onCategorySelect(category.id) },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentVibrant,
                            selectedLabelColor = Color.White,
                            containerColor = NavyLighter,
                            labelColor = Slate
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filterState.selectedCategoryId == category.id, borderColor = NavyAccent)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OffWhite,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Slate
        )
    }
}

@Composable
fun CompactChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NavyAccent.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = NavyLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SimpleCalendarView(viewModel: AnalyticsViewModel) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val calendar = Calendar.getInstance()
    
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    
    val dailyTotals = transactions.filter {
        calendar.timeInMillis = it.timestamp
        calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
    }.groupBy {
        calendar.timeInMillis = it.timestamp
        calendar.get(Calendar.DAY_OF_MONTH)
    }.mapValues { it.value.fold(java.math.BigDecimal.ZERO) { acc, tx -> if (tx.type == "Expense") acc.add(tx.amount) else acc } }

    Column(modifier = Modifier.padding(4.dp)) {
        dailyTotals.keys.sorted().chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { day ->
                    val spending = dailyTotals[day] ?: java.math.BigDecimal.ZERO
                    val intensity = spending.divide(java.math.BigDecimal(2000), 2, java.math.RoundingMode.HALF_UP).toFloat().coerceIn(0.1f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (spending > java.math.BigDecimal.ZERO) AccentVibrant.copy(alpha = intensity)
                                else NavyLighter
                            )
                            .border(
                                1.dp, 
                                if (spending > java.math.BigDecimal.ZERO) AccentVibrant.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (spending > java.math.BigDecimal.ZERO) Color.White else Slate
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AnalyticsMonthPicker(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthYearSelected: (Int, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val months = (1..12).toList()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        months.forEach { month ->
            val isSelected = month == selectedMonth
            val monthName = Calendar.getInstance().apply { set(Calendar.MONTH, month - 1) }
                .getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
            
            FilterChip(
                selected = isSelected,
                onClick = { onMonthYearSelected(month, selectedYear) },
                label = { Text(monthName ?: "") },
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

@Composable
fun MonthlyPerformanceCard(performance: com.example.myexpenditureapp.ui.viewmodel.AnalyticsViewModel.MonthlyPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NavyLight),
        border = BorderStroke(1.dp, NavyAccent.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MONTHLY STATUS", style = MaterialTheme.typography.labelSmall, color = Slate, fontWeight = FontWeight.Bold)
                    Text(
                        performance.grade.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = when(performance.grade) {
                            "Overspent" -> AccentError
                            "Near Limit" -> Gold
                            else -> AccentSuccess
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    color = if (performance.savingsChange >= java.math.BigDecimal.ZERO) AccentSuccess.copy(alpha = 0.1f) else AccentError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Using a simple text indicator instead of trending icons if they are missing
                        Text(
                            if (performance.savingsChange >= java.math.BigDecimal.ZERO) "↑" else "↓",
                            color = if (performance.savingsChange >= java.math.BigDecimal.ZERO) AccentSuccess else AccentError,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${performance.savingsChange.setScale(1, java.math.RoundingMode.HALF_UP).abs()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (performance.savingsChange >= java.math.BigDecimal.ZERO) AccentSuccess else AccentError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BUDGETED", style = MaterialTheme.typography.labelSmall, color = Slate)
                    Text(
                        "₹${performance.totalBudgeted.setScale(0, java.math.RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                        color = OffWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("SPENT", style = MaterialTheme.typography.labelSmall, color = Slate)
                    Text(
                        "₹${performance.totalSpent.setScale(0, java.math.RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                        color = if (performance.totalSpent > performance.totalBudgeted && performance.totalBudgeted > java.math.BigDecimal.ZERO) AccentError else OffWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("SAVINGS", style = MaterialTheme.typography.labelSmall, color = Slate)
                    Text(
                        "₹${performance.netSavings.setScale(0, java.math.RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                        color = if (performance.netSavings >= java.math.BigDecimal.ZERO) AccentSuccess else AccentError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
