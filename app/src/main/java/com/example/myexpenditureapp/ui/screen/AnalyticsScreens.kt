package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.data.entity.*
import com.example.myexpenditureapp.ui.component.*
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.ui.viewmodel.AnalyticsViewModel
import com.example.myexpenditureapp.ui.viewmodel.DrillLevel
import com.example.myexpenditureapp.ui.viewmodel.DrillNode
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
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
    val insights by viewModel.insights.collectAsStateWithLifecycle(initialValue = emptyList())
    val drillState by viewModel.drillState.collectAsStateWithLifecycle()
    val drillDownTransactions by viewModel.drillDownTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val categorySubBreakdown by viewModel.categorySubBreakdown.collectAsStateWithLifecycle(initialValue = emptyMap())
    val isParentRollupEnabled by viewModel.isParentRollupEnabled.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (drillState.level != DrillLevel.MACRO) "DRILL-DOWN VIEW" else "FINANCIAL INTELLIGENCE", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ) 
                },
                navigationIcon = {
                    if (drillState.level != DrillLevel.MACRO) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.drillUp()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Drill Up")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = "Filters",
                            tint = if (filterState.selectedAccountId != null || filterState.selectedCategoryId != null) 
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            ) 
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Month Picker Bar
            item {
                AnalyticsMonthPicker(
                    selectedMonth = filterState.month,
                    selectedYear = filterState.year,
                    onMonthYearSelected = { m, y -> viewModel.onMonthYearChange(m, y) }
                )
            }

            // Interactive Breadcrumb Header when Drilled Down
            if (drillState.level != DrillLevel.MACRO) {
                item {
                    DrillBreadcrumbHeader(
                        drillNode = drillState,
                        transactionCount = drillDownTransactions.size,
                        totalSum = drillDownTransactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) },
                        onDrillUp = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.drillUp()
                        }
                    )
                }
            }

            // Smart Insights Digest
            if (insights.isNotEmpty() && drillState.level == DrillLevel.MACRO) {
                item {
                    FinancialDigestSection(
                        insights = insights,
                        onInsightClick = { ins ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.drillDownInsight(ins)
                        }
                    )
                }
            }

            // Monthly Health Summary
            if (drillState.level == DrillLevel.MACRO) {
                item {
                    performance?.let { 
                        MonthlyPerformanceCard(it)
                    }
                }

                item {
                    KPISection(kpis)
                }

                item {
                    SectionHeader("SPENDING TREND", "Daily timeline velocity")
                    CompactChartCard {
                        LineChart(
                            data = spendingTrend,
                            projectedData = projectedTrend,
                            lineColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Category Mix & Interactive Drill-Down
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        if (drillState.level == DrillLevel.CATEGORY) "CATEGORY DRILL-DOWN" else "CATEGORY MIX",
                        if (drillState.level == DrillLevel.CATEGORY) "Merchant outlays for this category" else "Tap a category below to drill down"
                    )
                    if (drillState.level == DrillLevel.CATEGORY) {
                        TextButton(onClick = { viewModel.drillUp() }) {
                            Text("Drill Up ↑", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        FilterChip(
                            selected = isParentRollupEnabled,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleParentRollup()
                            },
                            label = {
                                Text(
                                    if (isParentRollupEnabled) "Rollup to Parents" else "Detailed Leaf",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isParentRollupEnabled) Icons.Default.AccountTree else Icons.Default.ViewList,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (drillState.level == DrillLevel.CATEGORY) {
                    // Merchant breakdown within the category
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "TOP SPEND DESTINATIONS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            if (categorySubBreakdown.isEmpty()) {
                                Text("No merchant data found for this category.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                val maxVal = categorySubBreakdown.values.maxOrNull()?.coerceAtLeast(BigDecimal.ONE) ?: BigDecimal.ONE
                                categorySubBreakdown.forEach { (merchant, amount) ->
                                    val pct = amount.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0.05f, 1f)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "₹${amount.toPlainString()}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = MonospaceFont,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    CompactChartCard {
                        PieChart(
                            data = categorySpending.mapKeys { it.key?.name ?: "Other" },
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tap-to-Drill Category Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categorySpending.forEach { (cat, amount) ->
                            if (cat != null && amount > BigDecimal.ZERO) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.drillDownCategory(cat)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cat.icon ?: "📁", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cat.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "₹${amount.setScale(0, RoundingMode.HALF_UP).toPlainString()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = MonospaceFont,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Intensity Calendar Map (Clickable days to drill-down)
            if (drillState.level == DrillLevel.MACRO) {
                item {
                    SectionHeader("INTENSITY MAP", "Tap any day to drill down")
                    CompactChartCard {
                        SimpleCalendarView(
                            viewModel = viewModel,
                            onDayClick = { day ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.drillDownDay(day)
                            }
                        )
                    }
                }

                item {
                    SectionHeader("WEEKLY SPENDING", "Tap a week to drill down")
                    CompactChartCard {
                        WeeklySpendingDrillList(
                            weeklySpending = weeklySpending,
                            onWeekClick = { week ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.drillDownWeek(week)
                            }
                        )
                    }
                }

                item {
                    SectionHeader("MULTI-MONTH COMPARISON", "Historical trajectory")
                    CompactChartCard {
                        BarChart(
                            data = monthlyComparison,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            // Drilled-Down Focused Transaction Stream
            if (drillState.level != DrillLevel.MACRO) {
                item {
                    SectionHeader("TRANSACTIONS IN FOCUS", "${drillDownTransactions.size} records matched")
                }

                if (drillDownTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No transactions recorded for this selection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(drillDownTransactions, key = { it.id }) { tx ->
                        DrillTransactionCard(transaction = tx, categories = categories)
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
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
fun DrillBreadcrumbHeader(
    drillNode: DrillNode,
    transactionCount: Int,
    totalSum: BigDecimal,
    onDrillUp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "DRILL LEVEL: ${drillNode.level.name}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    drillNode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$transactionCount transactions • Total: ₹${totalSum.toPlainString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onDrillUp,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Drill Up", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WeeklySpendingDrillList(
    weeklySpending: Map<String, BigDecimal>,
    onWeekClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        weeklySpending.forEach { (weekStr, amt) ->
            val weekNum = weekStr.replace("Week ", "").toIntOrNull() ?: 1
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWeekClick(weekNum) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$weekNum", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(weekStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "₹${amt.toPlainString()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MonospaceFont,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrillTransactionCard(
    transaction: Transaction,
    categories: List<Category>
) {
    val category = categories.find { it.id == transaction.categoryId }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(category?.icon ?: "💳", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant.ifBlank { "Untitled Transaction" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${category?.name ?: "General"} • ${sdf.format(Date(transaction.timestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (transaction.type == "Expense") "-" else "+"}₹${transaction.amount.toPlainString()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MonospaceFont,
                color = if (transaction.type == "Expense") ExpenseRed else IncomeGreen
            )
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = kpi.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = kpi.value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonospaceFont),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = kpi.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
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
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ACCOUNTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.selectedAccountId == null,
                    onClick = { onAccountSelect(null) },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp)
                )
                accounts.forEach { account ->
                    FilterChip(
                        selected = filterState.selectedAccountId == account.id,
                        onClick = { onAccountSelect(account.id) },
                        label = { Text(account.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, "Expense", "Income", "Transfer").forEach { type ->
                    FilterChip(
                        selected = filterState.selectedTransactionType == type,
                        onClick = { onTypeSelect(type) },
                        label = { Text(type ?: "All") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SOURCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, "Manual", "SMS").forEach { source ->
                    FilterChip(
                        selected = filterState.selectedSource == source,
                        onClick = { onSourceSelect(source) },
                        label = { Text(source ?: "All") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CATEGORIES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.selectedCategoryId == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp)
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = filterState.selectedCategoryId == category.id,
                        onClick = { onCategorySelect(category.id) },
                        label = { Text(category.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompactChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SimpleCalendarView(
    viewModel: AnalyticsViewModel,
    onDayClick: (Int) -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val calendar = Calendar.getInstance()
    
    val selectedMonth = filterState.month
    val selectedYear = filterState.year
    
    val dailyTotals = transactions.filter {
        calendar.timeInMillis = it.timestamp
        calendar.get(Calendar.MONTH) + 1 == selectedMonth && calendar.get(Calendar.YEAR) == selectedYear
    }.groupBy {
        calendar.timeInMillis = it.timestamp
        calendar.get(Calendar.DAY_OF_MONTH)
    }.mapValues { it.value.fold(java.math.BigDecimal.ZERO) { acc, tx -> if (tx.type == "Expense") acc.add(tx.amount) else acc } }

    val daysInMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, selectedYear)
        set(Calendar.MONTH, selectedMonth - 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    val allDays = (1..daysInMonth).toList()

    Column(modifier = Modifier.padding(4.dp)) {
        allDays.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { day ->
                    val spending = dailyTotals[day] ?: java.math.BigDecimal.ZERO
                    val intensity = spending.divide(java.math.BigDecimal(2000), 2, java.math.RoundingMode.HALF_UP).toFloat().coerceIn(0.15f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDayClick(day) }
                            .background(
                                if (spending > java.math.BigDecimal.ZERO) MaterialTheme.colorScheme.primary.copy(alpha = intensity)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp, 
                                if (spending > java.math.BigDecimal.ZERO) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (spending > java.math.BigDecimal.ZERO) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Fill remainder of row if week is incomplete
                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        months.forEach { month ->
            val isSelected = month == selectedMonth
            val monthName = Calendar.getInstance().apply { set(Calendar.MONTH, month - 1) }
                .getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
            
            FilterChip(
                selected = isSelected,
                onClick = { onMonthYearSelected(month, selectedYear) },
                label = { Text(monthName ?: "", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun MonthlyPerformanceCard(performance: com.example.myexpenditureapp.ui.viewmodel.AnalyticsViewModel.MonthlyPerformance) {
    val gradeColor = when(performance.grade) {
        "Overspent" -> ExpenseRed
        "Near Limit" -> AmberWarning
        else -> IncomeGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(gradeColor.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MONTHLY HEALTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            performance.grade.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = gradeColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Surface(
                        color = if (performance.savingsChange >= java.math.BigDecimal.ZERO) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (performance.savingsChange >= java.math.BigDecimal.ZERO) IncomeGreen.copy(alpha = 0.3f) else ExpenseRed.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (performance.savingsChange >= java.math.BigDecimal.ZERO) "↑" else "↓",
                                color = if (performance.savingsChange >= java.math.BigDecimal.ZERO) IncomeGreen else ExpenseRed,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${performance.savingsChange.setScale(1, java.math.RoundingMode.HALF_UP).abs()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (performance.savingsChange >= java.math.BigDecimal.ZERO) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BUDGETED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(
                            "₹${performance.totalBudgeted.setScale(0, java.math.RoundingMode.HALF_UP)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SPENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(
                            "₹${performance.totalSpent.setScale(0, java.math.RoundingMode.HALF_UP)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                            color = if (performance.totalSpent > performance.totalBudgeted && performance.totalBudgeted > java.math.BigDecimal.ZERO) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SAVINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(
                            "₹${performance.netSavings.setScale(0, java.math.RoundingMode.HALF_UP)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                            color = if (performance.netSavings >= java.math.BigDecimal.ZERO) IncomeGreen else ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
