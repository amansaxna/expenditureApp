package com.example.myexpenditureapp.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.ui.theme.MonospaceFont
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillDialog(
    initialAmount: BigDecimal,
    merchant: String,
    onDismiss: () -> Unit,
    onApplySplit: (perPersonShare: BigDecimal, splitCount: Int, appliedTag: String) -> Unit
) {
    var totalAmountText by remember { mutableStateOf(if (initialAmount > BigDecimal.ZERO) initialAmount.toPlainString() else "") }
    var numPeople by remember { mutableIntStateOf(2) }
    var tipPercent by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val totalAmount = remember(totalAmountText) {
        try {
            BigDecimal(totalAmountText.ifBlank { "0" })
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    val tipAmount = remember(totalAmount, tipPercent) {
        if (tipPercent > 0 && totalAmount > BigDecimal.ZERO) {
            totalAmount.multiply(BigDecimal(tipPercent)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    val finalTotal = remember(totalAmount, tipAmount) {
        totalAmount.add(tipAmount)
    }

    val perPersonShare = remember(finalTotal, numPeople) {
        if (numPeople > 0 && finalTotal > BigDecimal.ZERO) {
            finalTotal.divide(BigDecimal(numPeople), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Split Bill Calculator", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it },
                    label = { Text("Total Bill Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // People Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("People (${numPeople})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (numPeople > 2) numPeople-- },
                            enabled = numPeople > 2
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                        }
                        Text(
                            "$numPeople",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { if (numPeople < 50) numPeople++ }
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                        }
                    }
                }

                // Tip Options
                Column {
                    Text("Tip / Tax / Surcharge", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 5, 10, 15, 20).forEach { pct ->
                            FilterChip(
                                selected = tipPercent == pct,
                                onClick = { tipPercent = pct },
                                label = { Text("$pct%") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Each Person Pays:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                perPersonShare.formatIndian(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MonospaceFont,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (tipPercent > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Includes $tipPercent% tip:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${tipAmount.formatIndian()} (Total: ${finalTotal.formatIndian()})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Copy Share button
                OutlinedButton(
                    onClick = {
                        val summary = "Bill for $merchant: Total ${finalTotal.formatIndian()} split among $numPeople people = ${perPersonShare.formatIndian()} each."
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Split Bill", summary))
                        Toast.makeText(context, "Breakdown copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Breakdown to Share")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplySplit(perPersonShare, numPeople, "split")
                }
            ) {
                Text("Set My Share (${perPersonShare.formatIndian()})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
