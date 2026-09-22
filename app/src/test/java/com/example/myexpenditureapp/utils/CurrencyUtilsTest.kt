package com.example.myexpenditureapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CurrencyUtilsTest {

    @Test
    fun testIndianNumberingSystemFormatting() {
        assertEquals("0", CurrencyUtils.formatIndian(0L, includeSymbol = false))
        assertEquals("₹0", CurrencyUtils.formatIndian(0L, includeSymbol = true))
        
        // Hundreds
        assertEquals("₹500", CurrencyUtils.formatIndian(500L, includeSymbol = true))
        
        // 1 Thousand: 1,000
        assertEquals("1,000", CurrencyUtils.formatIndian(1000L, includeSymbol = false))
        assertEquals("₹1,000", CurrencyUtils.formatIndian(1000L, includeSymbol = true))
        
        // 10 Thousand: 10,000
        assertEquals("₹10,000", CurrencyUtils.formatIndian(10000L, includeSymbol = true))
        
        // 1 Lakh: 1,00,000
        assertEquals("1,00,000", CurrencyUtils.formatIndian(100000L, includeSymbol = false))
        assertEquals("₹1,00,000", CurrencyUtils.formatIndian(100000L, includeSymbol = true))
        
        // 10 Lakhs: 10,00,000
        assertEquals("₹10,00,000", CurrencyUtils.formatIndian(1000000L, includeSymbol = true))
        
        // 1 Crore: 1,00,00,000
        assertEquals("₹1,00,00,000", CurrencyUtils.formatIndian(10000000L, includeSymbol = true))
        
        // Multi-crore: 12,34,56,789
        assertEquals("₹12,34,56,789", CurrencyUtils.formatIndian(123456789L, includeSymbol = true))
    }

    @Test
    fun testNegativeAndDecimalFormatting() {
        // Negative 1 Lakh
        assertEquals("-₹1,00,000", CurrencyUtils.formatIndian(BigDecimal("-100000"), includeSymbol = true))
        
        // Decimals
        val amountWithDecimals = BigDecimal("100000.75")
        assertEquals("₹1,00,000.75", CurrencyUtils.formatIndian(amountWithDecimals, includeSymbol = true, includeDecimals = true))
    }
}
