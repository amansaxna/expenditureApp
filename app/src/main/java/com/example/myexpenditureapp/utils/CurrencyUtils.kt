package com.example.myexpenditureapp.utils

import java.math.BigDecimal
import java.math.RoundingMode

object CurrencyUtils {

    /**
     * Formats any number into the Indian Numbering System:
     * - 100 -> 100
     * - 1000 -> 1,000 (1 Thousand)
     * - 10000 -> 10,000 (10 Thousand)
     * - 100000 -> 1,00,000 (1 Lakh)
     * - 1000000 -> 10,00,000 (10 Lakh)
     * - 10000000 -> 1,00,00,000 (1 Crore)
     */
    fun formatIndian(
        amount: BigDecimal?,
        includeSymbol: Boolean = true,
        includeDecimals: Boolean = false,
        stripZeroDecimals: Boolean = true
    ): String {
        if (amount == null) return if (includeSymbol) "₹0" else "0"

        val isNegative = amount < BigDecimal.ZERO
        val absAmount = amount.abs()

        val plainStr = if (includeDecimals) {
            if (stripZeroDecimals && absAmount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                absAmount.setScale(0, RoundingMode.HALF_UP).toPlainString()
            } else {
                absAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()
            }
        } else {
            absAmount.setScale(0, RoundingMode.HALF_UP).toPlainString()
        }

        val parts = plainStr.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1 && (!stripZeroDecimals || parts[1] != "00" && parts[1] != "0")) {
            "." + parts[1]
        } else {
            ""
        }

        val formattedInteger = formatIndianInteger(integerPart)
        val symbol = if (includeSymbol) "₹" else ""
        val sign = if (isNegative) "-" else ""

        return "$sign$symbol$formattedInteger$decimalPart"
    }

    fun formatIndian(amount: Double, includeSymbol: Boolean = true, includeDecimals: Boolean = false): String {
        return formatIndian(BigDecimal.valueOf(amount), includeSymbol, includeDecimals)
    }

    fun formatIndian(amount: Long, includeSymbol: Boolean = true): String {
        return formatIndian(BigDecimal.valueOf(amount), includeSymbol, false)
    }

    fun formatIndian(amount: Int, includeSymbol: Boolean = true): String {
        return formatIndian(BigDecimal.valueOf(amount.toLong()), includeSymbol, false)
    }

    fun formatIndian(amount: String, includeSymbol: Boolean = true, includeDecimals: Boolean = false): String {
        val bd = amount.replace("₹", "").replace(",", "").trim().toBigDecimalOrNull()
        return if (bd != null) formatIndian(bd, includeSymbol, includeDecimals) else amount
    }

    fun formatIndianInteger(intStr: String): String {
        val clean = intStr.trimStart('0').ifEmpty { "0" }
        if (clean.length <= 3) return clean
        val lastThree = clean.takeLast(3)
        val remaining = clean.dropLast(3)
        val groupedRemaining = remaining.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedRemaining,$lastThree"
    }
}

// Extension Functions for concise usage
fun BigDecimal?.formatIndian(includeSymbol: Boolean = true, includeDecimals: Boolean = false): String =
    CurrencyUtils.formatIndian(this, includeSymbol, includeDecimals)

fun Double?.formatIndian(includeSymbol: Boolean = true, includeDecimals: Boolean = false): String =
    if (this != null) CurrencyUtils.formatIndian(this, includeSymbol, includeDecimals) else if (includeSymbol) "₹0" else "0"

fun Long?.formatIndian(includeSymbol: Boolean = true): String =
    if (this != null) CurrencyUtils.formatIndian(this, includeSymbol) else if (includeSymbol) "₹0" else "0"

fun Int?.formatIndian(includeSymbol: Boolean = true): String =
    if (this != null) CurrencyUtils.formatIndian(this, includeSymbol) else if (includeSymbol) "₹0" else "0"
