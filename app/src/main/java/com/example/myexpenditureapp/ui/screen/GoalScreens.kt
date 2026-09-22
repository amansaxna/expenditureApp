package com.example.myexpenditureapp.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myexpenditureapp.data.entity.SavingGoal
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.ui.viewmodel.GoalViewModel
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    viewModel: GoalViewModel,
    onBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingGoal?>(null) }
    var actionGoal by remember { mutableStateOf<SavingGoal?>(null) }
    var isDepositAction by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    val totalSaved = remember(goals) {
        goals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.currentAmount) }
    }
    val totalTarget = remember(goals) {
        goals.fold(BigDecimal.ZERO) { acc, g -> acc.add(g.targetAmount) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SAVINGS VAULTS & POCKETS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingGoal = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Goal", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        "No savings vaults yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Create target pockets (Vacation, Emergency Fund, Tech) and lock away your savings toward your dreams!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total Vault Summary Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            IndigoPrimary.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "TOTAL VAULT BALANCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        totalSaved.formatIndian(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = MonospaceFont,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Target: ${totalTarget.formatIndian()} across ${goals.size} vaults",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = IncomeGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        onDeposit = {
                            actionGoal = goal
                            isDepositAction = true
                        },
                        onWithdraw = {
                            actionGoal = goal
                            isDepositAction = false
                        },
                        onEdit = {
                            editingGoal = goal
                            showAddDialog = true
                        },
                        onDelete = {
                            viewModel.deleteGoal(goal)
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddEditGoalDialog(
                initialGoal = editingGoal,
                onDismiss = { showAddDialog = false },
                onSave = { name, target, current, targetDate, icon, colorHex, id ->
                    viewModel.saveGoal(name, target, current, targetDate, icon, colorHex, id)
                    showAddDialog = false
                }
            )
        }

        actionGoal?.let { goal ->
            FundTransferDialog(
                goal = goal,
                isDeposit = isDepositAction,
                onDismiss = { actionGoal = null },
                onConfirm = { amount ->
                    if (isDepositAction) {
                        viewModel.depositFunds(goal.id, amount)
                    } else {
                        viewModel.withdrawFunds(goal.id, amount)
                    }
                    actionGoal = null
                }
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: SavingGoal,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > BigDecimal.ZERO) {
        goal.currentAmount.divide(goal.targetAmount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "goalProgress"
    )
    val percentInt = (progress * 100).toInt()
    val isDone = goal.isCompleted || progress >= 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            1.dp,
            if (isDone) IncomeGreen.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDone) IncomeGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(goal.icon, style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isDone) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = IncomeGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "COMPLETED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (goal.targetDateEpochMs != null) {
                        val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                        Text(
                            text = "Target Date: ${sdf.format(Date(goal.targetDateEpochMs))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Monetary Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "SAVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        goal.currentAmount.formatIndian(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        fontFamily = MonospaceFont,
                        color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "TARGET: ${goal.targetAmount.formatIndian()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = MonospaceFont,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$percentInt%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MonospaceFont,
                        color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (isDone) IncomeGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Deposit & Withdraw action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDeposit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deposit", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Withdraw", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddEditGoalDialog(
    initialGoal: SavingGoal?,
    onDismiss: () -> Unit,
    onSave: (name: String, target: BigDecimal, current: BigDecimal, targetDate: Long?, icon: String, colorHex: String, id: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var targetText by remember { mutableStateOf(initialGoal?.targetAmount?.toPlainString() ?: "") }
    var currentText by remember { mutableStateOf(initialGoal?.currentAmount?.toPlainString() ?: "0") }
    var selectedIcon by remember { mutableStateOf(initialGoal?.icon ?: "🎯") }
    val icons = listOf("🎯", "🏖️", "🚗", "💻", "🏠", "💍", "🚑", "🎓", "✈️", "🚀", "📱", "🎮", "🛡️", "💰")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialGoal == null) "Create Saving Vault" else "Edit Vault", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vault Name (e.g. MacBook Pro)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Amount (₹)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Initial Deposit (₹)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Vault Icon", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(icons) { ic ->
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedIcon = ic },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedIcon == ic) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedIcon == ic) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(ic, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    val target = try { BigDecimal(targetText) } catch (e: Exception) { BigDecimal.ZERO }
                    val current = try { BigDecimal(currentText) } catch (e: Exception) { BigDecimal.ZERO }
                    if (name.isNotBlank() && target > BigDecimal.ZERO) {
                        onSave(name, target, current, null, selectedIcon, "#3B82F6", initialGoal?.id ?: 0L)
                    }
                }
            ) {
                Text("Save Vault", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FundTransferDialog(
    goal: SavingGoal,
    isDeposit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val quickPills = listOf(500, 1000, 2000, 5000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isDeposit) "Deposit to ${goal.name}" else "Withdraw from ${goal.name}",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Balance", style = MaterialTheme.typography.bodySmall)
                        Text(
                            goal.currentAmount.formatIndian(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonospaceFont
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Increment Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPills.forEach { pill ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (currentVal + pill).toInt().toString()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${pill.formatIndian()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MonospaceFont,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    val amt = try { BigDecimal(amountText) } catch (e: Exception) { BigDecimal.ZERO }
                    if (amt > BigDecimal.ZERO) {
                        onConfirm(amt)
                    }
                }
            ) {
                Text(if (isDeposit) "Deposit" else "Withdraw", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
