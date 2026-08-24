package com.example.myexpenditureapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.ui.component.BarChart
import com.example.myexpenditureapp.ui.component.LineChart
import com.example.myexpenditureapp.ui.component.PieChart
import com.example.myexpenditureapp.ui.theme.Gold
import com.example.myexpenditureapp.ui.theme.NavyDeep
import com.example.myexpenditureapp.ui.viewmodel.AnalyticsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycle(initialValue = emptyMap())
    val monthlyComparison by viewModel.monthlyComparison.collectAsStateWithLifecycle(initialValue = emptyMap())
    val spendingTrend by viewModel.spendingTrend.collectAsStateWithLifecycle(initialValue = emptyMap())
    val kpis by viewModel.kpis.collectAsStateWithLifecycle(initialValue = emptyMap())
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Financial Insights", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                }
            ) 
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                KPIHeader(kpis)
            }

            if (showFilters) {
                item {
                    AnalyticsFilters(
                        filterState = filterState,
                        accounts = accounts,
                        categories = categories,
                        onAccountSelect = { viewModel.onAccountFilterChange(it) },
                        onCategorySelect = { viewModel.onCategoryFilterChange(it) }
                    )
                }
            }

            item {
                SectionHeader("Spending Trend")
                CompactChartCard {
                    LineChart(
                        data = spendingTrend,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                SectionHeader("Spending by Category")
                CompactChartCard {
                    PieChart(
                        data = categorySpending.mapKeys { it.key?.name ?: "Other" },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            item {
                SectionHeader("Monthly Comparison")
                CompactChartCard {
                    BarChart(
                        data = monthlyComparison,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                SectionHeader("Spending Patterns")
                CompactChartCard {
                    SimpleCalendarView(viewModel)
                }
            }
        }
    }
}

@Composable
fun KPIHeader(kpis: Map<String, String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        kpis.forEach { (label, value) ->
            item {
                Card(
                    modifier = Modifier.width(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
    onCategorySelect: (Long?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Filters", style = MaterialTheme.typography.titleSmall)
        
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = filterState.selectedAccountId == null,
                onClick = { onAccountSelect(null) },
                label = { Text("All Accounts") }
            )
            accounts.forEach { account ->
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = filterState.selectedAccountId == account.id,
                    onClick = { onAccountSelect(account.id) },
                    label = { Text(account.name) }
                )
            }
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = filterState.selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Categories") }
            )
            categories.forEach { category ->
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = filterState.selectedCategoryId == category.id,
                    onClick = { onCategorySelect(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun CompactChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        content()
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
    }.mapValues { it.value.sumOf { tx -> if (tx.type == "Expense") tx.amount else 0.0 } }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Current Month Spending",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Simple 7x5 grid representation
        dailyTotals.keys.sorted().chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { day ->
                    val spending = dailyTotals[day] ?: 0.0
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (spending > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = (spending / 1000).toFloat().coerceIn(0.2f, 1f))
                                else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
