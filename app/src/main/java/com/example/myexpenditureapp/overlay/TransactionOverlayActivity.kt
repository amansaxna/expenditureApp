package com.example.myexpenditureapp.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import com.example.myexpenditureapp.ui.component.CategorySelectionBottomSheet
import com.example.myexpenditureapp.ui.component.GeometricMascotBot
import com.example.myexpenditureapp.ui.component.MascotMood
import androidx.lifecycle.lifecycleScope
import com.example.myexpenditureapp.MainActivity
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.utils.formatIndian
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TransactionOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"

        fun launchIfAllowed(context: Context, transactionId: Long) {
            val isDrawOverlaysAllowed = OverlayHelper.canDrawOverlays(context)
            val isSettingEnabled = OverlayHelper.isInstantOverlayEnabled(context)

            if (OverlayHelper.shouldLaunchOverlay(isDrawOverlaysAllowed, isSettingEnabled)) {
                val intent = Intent(context, TransactionOverlayActivity::class.java).apply {
                    putExtra(EXTRA_TRANSACTION_ID, transactionId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        if (transactionId == -1L) {
            finish()
            return
        }

        Graph.provide(applicationContext)
        val actionHandler = OverlayActionHandler(
            Graph.transactionRepository,
            Graph.autoCategoryRuleRepository
        )

        setContent {
            MyExpenditureAppTheme {
                var transaction by remember { mutableStateOf<Transaction?>(null) }
                var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

                LaunchedEffect(transactionId) {
                    transaction = Graph.transactionRepository.getTransactionById(transactionId)
                    categories = Graph.categoryRepository.getAllCategories().first()
                }

                transaction?.let { currentTx ->
                    TransactionOverlayDialog(
                        transaction = currentTx,
                        categories = categories,
                        onConfirm = { merchant, amount, type, categoryId, message, saveAsRule ->
                            lifecycleScope.launch {
                                actionHandler.confirmTransaction(
                                    transaction = currentTx,
                                    updatedMerchant = merchant,
                                    updatedAmount = amount,
                                    updatedType = type,
                                    categoryId = categoryId,
                                    updatedMessage = message,
                                    saveAsRule = saveAsRule
                                )
                                Toast.makeText(applicationContext, "Transaction Confirmed!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        },
                        onDiscard = {
                            lifecycleScope.launch {
                                actionHandler.discardTransaction(currentTx)
                                Toast.makeText(applicationContext, "Transaction Discarded", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        },
                        onOpenFullEdit = {
                            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                            finish()
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionOverlayDialog(
    transaction: Transaction,
    categories: List<Category>,
    onConfirm: (merchant: String, amount: BigDecimal, type: String, categoryId: Long?, message: String?, saveAsRule: Boolean) -> Unit,
    onDiscard: () -> Unit,
    onOpenFullEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var amountText by remember { mutableStateOf(transaction.amount.stripTrailingZeros().toPlainString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var messageText by remember { mutableStateOf(transaction.rawMessage ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var saveAsRule by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val parsedAmount = remember(amountText) {
        amountText.toBigDecimalOrNull() ?: transaction.amount
    }

    var showCategorySheet by remember { mutableStateOf(false) }

    BackHandler(enabled = !showCategorySheet) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GeometricMascotBot(
                            mood = MascotMood.SCANNING,
                            size = 28.dp
                        )
                        Text(
                            text = "Auto Payment Detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Big Formatted Amount Preview
                Text(
                    text = parsedAmount.formatIndian(
                        includeSymbol = true,
                        includeDecimals = parsedAmount.scale() > 0 || parsedAmount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonospaceFont),
                    color = if (type == "Income") IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Segmented Type Selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val types = listOf(
                            Triple("Expense", ExpenseRed, Icons.AutoMirrored.Filled.TrendingDown),
                            Triple("Income", IncomeGreen, Icons.AutoMirrored.Filled.TrendingUp),
                            Triple("Transfer", BlueAccent, Icons.Default.SwapHoriz)
                        )
                        types.forEach { (t, activeColor, icon) ->
                            val isSelected = type == t
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        type = t
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) activeColor else Color.Transparent,
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = t,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Editable Fields: Merchant & Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant / Title") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Editable Message / SMS Notes Field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message / SMS Notes") },
                    placeholder = { Text("Original bank SMS or notes") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceFont)
                )

                // Category & Subcategory Selector (Same format as Transaction Edit screen)
                val selectedCategory = remember(categories, selectedCategoryId) {
                    categories.find { it.id == selectedCategoryId }
                }
                val parentCategory = remember(categories, selectedCategory) {
                    if (selectedCategory?.parentId != null) {
                        categories.find { it.id == selectedCategory.parentId }
                    } else null
                }
                val categoryDisplayText = remember(selectedCategory, parentCategory) {
                    if (parentCategory != null) {
                        "${parentCategory.icon ?: ""} ${parentCategory.name} > ${selectedCategory?.icon ?: ""} ${selectedCategory?.name ?: ""}".trim()
                    } else if (selectedCategory != null) {
                        "${selectedCategory.icon ?: ""} ${selectedCategory.name}".trim()
                    } else {
                        "Select Category & Subcategory"
                    }
                }

                Text(
                    text = "Category & Subcategory",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showCategorySheet = true
                        }
                ) {
                    OutlinedTextField(
                        value = categoryDisplayText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category & Subcategory") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = if (selectedCategoryId != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        leadingIcon = {
                            val iconEmoji = selectedCategory?.icon ?: parentCategory?.icon
                            if (!iconEmoji.isNullOrBlank()) {
                                Text(iconEmoji, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
                            } else {
                                Icon(Icons.Default.Category, contentDescription = null)
                            }
                        }
                    )
                }

                // Quick Root Category Chips with Real Emojis
                val rootCategories = remember(categories) { categories.filter { it.parentId == null } }
                if (rootCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rootCategories.forEach { rootCat ->
                            val isRootActive = selectedCategoryId == rootCat.id || parentCategory?.id == rootCat.id
                            FilterChip(
                                selected = isRootActive,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val subcategories = categories.filter { it.parentId == rootCat.id }
                                    if (subcategories.isEmpty()) {
                                        selectedCategoryId = if (selectedCategoryId == rootCat.id) null else rootCat.id
                                    } else {
                                        selectedCategoryId = rootCat.id
                                        showCategorySheet = true
                                    }
                                },
                                label = { Text("${rootCat.icon ?: ""} ${rootCat.name}".trim()) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        SuggestionChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCategorySheet = true
                            },
                            label = { Text("More...") },
                            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Save as Rule Checkbox
                if (selectedCategoryId != null) {
                    val catName = categories.find { it.id == selectedCategoryId }?.name ?: "this category"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveAsRule = !saveAsRule }
                    ) {
                        Checkbox(
                            checked = saveAsRule,
                            onCheckedChange = { saveAsRule = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Always categorize '$merchant' as $catName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Primary & Secondary Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val finalAmount = amountText.toBigDecimalOrNull() ?: transaction.amount
                            val finalMerchant = merchant.trim().ifBlank { transaction.merchant }
                            val finalMessage = messageText.trim().ifBlank { null }
                            onConfirm(finalMerchant, finalAmount, type, selectedCategoryId, finalMessage, saveAsRule)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm & Save", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenFullEdit,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Full Edit", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = onDiscard,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discard", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (showCategorySheet) {
            CategorySelectionBottomSheet(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = {
                    selectedCategoryId = it
                    showCategorySheet = false
                },
                onAddNewCategory = {
                    showCategorySheet = false
                },
                onDismiss = {
                    showCategorySheet = false
                }
            )
        }
    }
}
