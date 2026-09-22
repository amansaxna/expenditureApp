package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.data.entity.Category
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.ui.theme.IncomeGreen
import com.example.myexpenditureapp.ui.theme.ExpenseRed
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import com.example.myexpenditureapp.utils.formatIndian

@Composable
fun StandardTransactionRow(
    transaction: Transaction,
    category: Category?,
    accountName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == "Expense"
    val isIncome = transaction.type == "Income"

    val amountColor = when {
        isIncome -> IncomeGreen
        isExpense -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.primary
    }

    val amountPrefix = when {
        isIncome -> "+₹"
        isExpense -> "-₹"
        else -> "₹"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rowPressScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon Squircle Container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category?.icon ?: if (isIncome) "💰" else "💳",
                    fontSize = 18.sp
                )
            }

            // Merchant Title & Structured Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant.ifBlank { category?.name ?: "Transaction" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(transaction.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = category?.name ?: "General",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tabular Monospaced Currency Figure
            Text(
                text = "$amountPrefix${transaction.amount.formatIndian(includeSymbol = false, includeDecimals = false)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            )
        }
    }
}
