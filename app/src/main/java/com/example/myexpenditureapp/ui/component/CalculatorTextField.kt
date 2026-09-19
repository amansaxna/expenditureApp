package com.example.myexpenditureapp.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun CalculatorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isError by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            isError = false
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val result = evaluateExpression(value)
                if (result != null) {
                    onValueChange(result)
                    isError = false
                } else {
                    isError = true
                }
            }) {
                Icon(Icons.Default.Calculate, contentDescription = "Calculate")
            }
        },
        isError = isError,
        supportingText = {
            if (isError) {
                Text("Invalid expression")
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text) // Allow +, -, *, /
    )
}

fun evaluateExpression(expression: String): String? {
    return try {
        val cleaned = expression.replace("₹", "").replace(" ", "").trim()
        if (cleaned.isBlank()) return null
        
        // Handle basic addition/subtraction first
        val result = if (cleaned.contains("+")) {
            val parts = cleaned.split("+")
            parts.map { BigDecimal(it) }.reduce { acc, bigDecimal -> acc.add(bigDecimal) }
        } else if (cleaned.contains("-") && !cleaned.startsWith("-")) {
            val parts = cleaned.split("-")
            parts.map { BigDecimal(it) }.reduce { acc, bigDecimal -> acc.subtract(bigDecimal) }
        } else if (cleaned.contains("*")) {
            val parts = cleaned.split("*")
            parts.map { BigDecimal(it) }.reduce { acc, bigDecimal -> acc.multiply(bigDecimal) }
        } else if (cleaned.contains("/")) {
            val parts = cleaned.split("/")
            parts.map { BigDecimal(it) }.reduce { acc, bigDecimal -> acc.divide(bigDecimal, 2, RoundingMode.HALF_UP) }
        } else {
            BigDecimal(cleaned)
        }
        
        result.stripTrailingZeros().toPlainString()
    } catch (e: Exception) {
        null
    }
}
