package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myexpenditureapp.data.entity.Subscription
import com.example.myexpenditureapp.domain.radar.RadarBillItem
import com.example.myexpenditureapp.domain.radar.RadarStatus
import com.example.myexpenditureapp.domain.radar.SubscriptionSuggestion
import com.example.myexpenditureapp.ui.viewmodel.SubscriptionViewModel
import com.example.myexpenditureapp.ui.viewmodel.UiEvent
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = viewModel()
) {
    val radarSummary by viewModel.radarSummary.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Success -> {
                    showAddDialog = false
                    editingSubscription = null
                }
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bills & Subscriptions Radar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Predictive Cashflow & Due Dates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingSubscription = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Subscription") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Radar Forecast Card
            item {
                RadarForecastCard(
                    totalMonthly = radarSummary.totalMonthlyCommitment,
                    upcoming7Days = radarSummary.upcomingNext7Days,
                    paidThisMonth = radarSummary.paidThisMonth,
                    overdueAmount = radarSummary.overdueAmount
                )
            }

            // 2. Detected Suggestions Banner
            if (radarSummary.detectedSuggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "SMART RADAR DETECTIONS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(radarSummary.detectedSuggestions) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAccept = { viewModel.acceptSuggestion(suggestion) },
                        onDismiss = { viewModel.dismissSuggestion(suggestion) }
                    )
                }
            }

            // 3. Upcoming Schedule Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UPCOMING SCHEDULE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${radarSummary.billItems.size} active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Empty State or Bill Items
            if (radarSummary.billItems.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.EventRepeat,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No active recurring bills",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Track Netflix, Rent, WiFi, and EMIs to get cashflow alerts before money leaves your account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(radarSummary.billItems, key = { it.subscription.id }) { item ->
                    BillRadarRow(
                        item = item,
                        onLogPayment = { viewModel.logPayment(item) },
                        onEdit = {
                            editingSubscription = item.subscription
                            showAddDialog = true
                        },
                        onDelete = { viewModel.deleteSubscription(item.subscription) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditSubscriptionDialog(
            existing = editingSubscription,
            accounts = accounts,
            categories = categories,
            onDismiss = {
                showAddDialog = false
                editingSubscription = null
            },
            onSave = { name, amount, cycle, day, catId, accId, notes ->
                viewModel.saveSubscription(
                    name = name,
                    amount = amount,
                    billingCycle = cycle,
                    dueDayOfMonth = day,
                    categoryId = catId,
                    accountId = accId,
                    notes = notes,
                    id = editingSubscription?.id ?: 0L
                )
            }
        )
    }
}

@Composable
fun RadarForecastCard(
    totalMonthly: BigDecimal,
    upcoming7Days: BigDecimal,
    paidThisMonth: BigDecimal,
    overdueAmount: BigDecimal
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Monthly Commitment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${totalMonthly.setScale(0, RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                if (overdueAmount > BigDecimal.ZERO) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Overdue: ₹${overdueAmount.setScale(0, RoundingMode.HALF_UP)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Upcoming (7 Days)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "₹${upcoming7Days.setScale(0, RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paid this Month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "₹${paidThisMonth.setScale(0, RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF10B981)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: SubscriptionSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.merchantName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "₹${suggestion.estimatedAmount.setScale(0, RoundingMode.HALF_UP)}/mo • Due ~${suggestion.detectedDueDay}th (${suggestion.occurrences}x detected)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = onAccept,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Track", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun BillRadarRow(
    item: RadarBillItem,
    onLogPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    val statusBadgeColor = when (item.status) {
        RadarStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
        RadarStatus.DUE_TODAY -> Color(0xFFFEF3C7)
        RadarStatus.DUE_SOON -> MaterialTheme.colorScheme.primaryContainer
        RadarStatus.UPCOMING -> MaterialTheme.colorScheme.surfaceVariant
        RadarStatus.PAID_THIS_CYCLE -> Color(0xFFD1FAE5)
    }

    val statusTextColor = when (item.status) {
        RadarStatus.OVERDUE -> MaterialTheme.colorScheme.onErrorContainer
        RadarStatus.DUE_TODAY -> Color(0xFF92400E)
        RadarStatus.DUE_SOON -> MaterialTheme.colorScheme.onPrimaryContainer
        RadarStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
        RadarStatus.PAID_THIS_CYCLE -> Color(0xFF065F46)
    }

    val statusText = when (item.status) {
        RadarStatus.OVERDUE -> "Overdue by ${kotlin.math.abs(item.daysRemaining)}d"
        RadarStatus.DUE_TODAY -> "Due Today"
        RadarStatus.DUE_SOON -> "Due in ${item.daysRemaining}d"
        RadarStatus.UPCOMING -> "Due in ${item.daysRemaining}d"
        RadarStatus.PAID_THIS_CYCLE -> "Paid ✓"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category / Avatar Squircle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.subscription.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.subscription.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBadgeColor
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.subscription.billingCycle} • Due on ${item.subscription.dueDayOfMonth}th",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount & Action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${item.subscription.amount.setScale(0, RoundingMode.HALF_UP)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isPaidThisCycle) {
                        FilledTonalButton(
                            onClick = onLogPayment,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Pay / Log", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    expandedMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionDialog(
    existing: Subscription?,
    accounts: List<com.example.myexpenditureapp.data.entity.Account>,
    categories: List<com.example.myexpenditureapp.data.entity.Category>,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: BigDecimal, cycle: String, day: Int, catId: Long?, accId: Long?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toPlainString() ?: "") }
    var cycle by remember { mutableStateOf(existing?.billingCycle ?: "Monthly") }
    var dueDayText by remember { mutableStateOf((existing?.dueDayOfMonth ?: 1).toString()) }
    var selectedCategoryId by remember { mutableStateOf(existing?.categoryId) }
    var selectedAccountId by remember { mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    val cycles = listOf("Monthly", "Quarterly", "Yearly", "Weekly")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Add Recurring Bill" else "Edit Subscription",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service / Bill Name") },
                    placeholder = { Text("e.g. Netflix, Wifi, Rent, SIP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it },
                        label = { Text("Day of Month") },
                        placeholder = { Text("1-31") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Billing Cycle Selector
                Column {
                    Text(
                        text = "Billing Cycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cycles.forEach { c ->
                            FilterChip(
                                selected = cycle == c,
                                onClick = { cycle = c },
                                label = { Text(c, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmt = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val parsedDay = dueDayText.toIntOrNull() ?: 1
                    if (name.isNotBlank() && parsedAmt > BigDecimal.ZERO) {
                        onSave(name, parsedAmt, cycle, parsedDay, selectedCategoryId, selectedAccountId, notes)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
