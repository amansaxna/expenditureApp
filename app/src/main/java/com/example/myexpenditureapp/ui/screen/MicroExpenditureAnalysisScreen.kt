package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.domain.insights.MicroSpendAnalyzer
import com.example.myexpenditureapp.domain.insights.MicroSpendSummary
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicroExpenditureAnalysisScreen(
    allTransactions: List<Transaction>,
    allCategories: List<Category>,
    selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialIsOverall: Boolean = false,
    onBack: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isOverallHistory by remember { mutableStateOf(initialIsOverall) }
    var selectedThreshold by remember { mutableStateOf(BigDecimal("250")) }
    var expandedCategoryIds by remember { mutableStateOf(setOf<Long?>()) }

    val currentCal = Calendar.getInstance()
    val activeMonth = if (selectedMonth in 1..12) selectedMonth else currentCal.get(Calendar.MONTH) + 1
    val activeYear = if (selectedYear > 2000) selectedYear else currentCal.get(Calendar.YEAR)

    val monthDisplayName = remember(activeMonth, activeYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, activeMonth - 1)
            set(Calendar.YEAR, activeYear)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // Filter transactions according to selected timeframe
    val targetTransactions = remember(allTransactions, isOverallHistory, activeMonth, activeYear) {
        if (isOverallHistory) {
            allTransactions
        } else {
            allTransactions.filter { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                c.get(Calendar.MONTH) + 1 == activeMonth && c.get(Calendar.YEAR) == activeYear
            }
        }
    }

    // Calculate micro vs macro analysis reactively
    val summary: MicroSpendSummary = remember(targetTransactions, allCategories, selectedThreshold) {
        MicroSpendAnalyzer.analyze(
            transactions = targetTransactions,
            categories = allCategories,
            customThresholdOverride = selectedThreshold
        )
    }

    // Average transaction size for micro spend
    val avgMicroSpend = remember(summary) {
        if (summary.microTxCount > 0) {
            summary.totalMicroSpend.divide(BigDecimal(summary.microTxCount), 0, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    // Group micro transactions by merchant for concentration analysis
    val merchantConcentration = remember(summary.microTransactions) {
        summary.microTransactions
            .groupBy { it.merchant.ifBlank { "Uncategorized Merchant" } }
            .map { (name, txs) ->
                val total = txs.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
                Triple(name, total, txs.size)
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    // Time of day pattern analysis
    val timeOfDayBreakdown = remember(summary.microTransactions) {
        val groups = mutableMapOf(
            "Morning (5AM - 12PM)" to mutableListOf<Transaction>(),
            "Afternoon (12PM - 5PM)" to mutableListOf<Transaction>(),
            "Evening (5PM - 9PM)" to mutableListOf<Transaction>(),
            "Night (9PM - 5AM)" to mutableListOf<Transaction>()
        )
        val cal = Calendar.getInstance()
        summary.microTransactions.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> groups["Morning (5AM - 12PM)"]?.add(tx)
                in 12..16 -> groups["Afternoon (12PM - 5PM)"]?.add(tx)
                in 17..20 -> groups["Evening (5PM - 9PM)"]?.add(tx)
                else -> groups["Night (9PM - 5AM)"]?.add(tx)
            }
        }
        groups.map { (label, txs) ->
            val total = txs.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
            val icon = when {
                label.startsWith("Morning") -> "🌅"
                label.startsWith("Afternoon") -> "☀️"
                label.startsWith("Evening") -> "🌆"
                else -> "🌙"
            }
            Triple(label, total, Pair(txs.size, icon))
        }.filter { it.second > BigDecimal.ZERO }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Micro Spend Intelligence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "In-depth Small vs Major Outflow Analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PROMINENT SEGMENTED TIMEFRAME SELECTOR (Month vs All-Time)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Month Segment
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isOverallHistory) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isOverallHistory = false
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isOverallHistory) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shadowElevation = if (!isOverallHistory) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (!isOverallHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = monthDisplayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!isOverallHistory) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isOverallHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // All-Time Segment
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (!isOverallHistory) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isOverallHistory = true
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOverallHistory) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shadowElevation = if (isOverallHistory) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AllInclusive,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isOverallHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "All-Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isOverallHistory) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOverallHistory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // DYNAMIC THRESHOLD LIMIT TUNER
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "MICRO THRESHOLD DEFINITION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "<${selectedThreshold.formatIndian(includeSymbol = true, includeDecimals = false)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            "Purchases at or below this value are classified as Micro/Small expenditures. Tap a preset to recalculate:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                BigDecimal("100"),
                                BigDecimal("150"),
                                BigDecimal("250"),
                                BigDecimal("500"),
                                BigDecimal("1000")
                            ).forEach { preset ->
                                val isSelected = selectedThreshold.compareTo(preset) == 0
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedThreshold = preset
                                    },
                                    label = { Text("<${preset.formatIndian(includeSymbol = true, includeDecimals = false)}") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // EXECUTIVE KPI BENTO GRID
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Small Spend KPI
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("☕", fontSize = 14.sp)
                                Text(
                                    "SMALL SPEND",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = summary.totalMicroSpend.formatIndian(includeSymbol = true),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                                fontWeight = FontWeight.ExtraBold,
                                color = ExpenseRed
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${summary.microSpendPercentage}% outflow • ${summary.microTxCount} txs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Major Spend KPI
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🐘", fontSize = 14.sp)
                                Text(
                                    "MAJOR SPEND",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = summary.totalMacroSpend.formatIndian(includeSymbol = true),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonospaceFont),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${100 - summary.microSpendPercentage}% outflow • ${summary.macroTxCount} txs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // AVERAGE PURCHASE & LEAK IMPACT ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "AVG SMALL PURCHASE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${avgMicroSpend.formatIndian(includeSymbol = true)} / tx",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonospaceFont),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "LEAK VULNERABILITY",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            val vulnerability = when {
                                summary.microSpendPercentage > 20 -> "High Leak Drain ⚠️"
                                summary.microSpendPercentage > 10 -> "Moderate Leak 🔍"
                                else -> "Well Controlled 🛡️"
                            }
                            Text(
                                text = vulnerability,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.microSpendPercentage > 20) ExpenseRed else IncomeGreen
                            )
                        }
                    }
                }
            }

            // DUAL PROGRESS SPLIT BAR
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "OUTFLOW RATIO SPLIT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val totalCombined = summary.totalMicroSpend.add(summary.totalMacroSpend)
                            Text(
                                "Total: ${totalCombined.formatIndian(includeSymbol = true)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonospaceFont),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Split visual indicator
                        val microRatio = (summary.microSpendPercentage / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { microRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = ExpenseRed,
                            trackColor = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                                Text("Small Spend (${summary.microSpendPercentage}%)", style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Text("Major Spend (${100 - summary.microSpendPercentage}%)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // SECTION: SHOW ALL CATEGORIES (Interactive In-Depth Drill-Down)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ALL CATEGORIES BREAKDOWN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${summary.topMicroCategories.size} categories with small transactions • Tap to expand items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (summary.topMicroCategories.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🎉", fontSize = 28.sp)
                            Text(
                                "No Small Expenditures Found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "No expenses recorded below <${selectedThreshold.formatIndian(includeSymbol = true, includeDecimals = false)} for this timeframe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(summary.topMicroCategories) { cat ->
                    val isExpanded = cat.categoryId in expandedCategoryIds
                    val catTransactions = remember(cat.categoryId, summary.microTransactions) {
                        summary.microTransactions.filter { it.categoryId == cat.categoryId }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expandedCategoryIds = if (isExpanded) {
                                    expandedCategoryIds - cat.categoryId
                                } else {
                                    expandedCategoryIds + cat.categoryId
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Category Row Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(cat.categoryIcon, fontSize = 18.sp)
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = cat.categoryName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${cat.txCount} txs • ${cat.percentOfMicroTotal}% of small spend",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = cat.microTotal.formatIndian(includeSymbol = true),
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonospaceFont),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Category progress bar
                            LinearProgressIndicator(
                                progress = { (cat.percentOfMicroTotal / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Expandable list of transactions inside this category
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )

                                    catTransactions.forEach { tx ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.merchant.ifBlank { "Quick Purchase" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = tx.amount.formatIndian(includeSymbol = true),
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont),
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TOP SMALL-SPEND MERCHANTS CONCENTRATION
            if (merchantConcentration.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "TOP SMALL-SPEND MERCHANTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            merchantConcentration.forEach { (name, total, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("($count txs)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Text(
                                        text = total.formatIndian(includeSymbol = true),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TIME OF DAY HABIT CLUSTERS
            if (timeOfDayBreakdown.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "TIME OF DAY DRIFT CLUSTERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            timeOfDayBreakdown.forEach { (timeSlot, total, meta) ->
                                val (count, icon) = meta
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(icon, fontSize = 14.sp)
                                        Text(timeSlot, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("($count txs)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Text(
                                        text = total.formatIndian(includeSymbol = true),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM ACTION: VIEW ALL IN LEDGER
            item {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToTransactions()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "View In Transactions Ledger",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
