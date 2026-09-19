package com.example.myexpenditureapp.domain

import com.example.myexpenditureapp.ui.component.evaluateExpression
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorAndSplitTest {

    @Test
    fun testSimpleAddition() {
        val res = evaluateExpression("100 + 250")
        assertEquals("350", res)
    }

    @Test
    fun testMultiAddition() {
        val res = evaluateExpression("50 + 25 + 25")
        assertEquals("100", res)
    }

    @Test
    fun testSubtraction() {
        val res = evaluateExpression("500 - 150")
        assertEquals("350", res)
    }

    @Test
    fun testMultiplication() {
        val res = evaluateExpression("45 * 4")
        assertEquals("180", res)
    }

    @Test
    fun testDivision() {
        val res = evaluateExpression("1000 / 4")
        assertEquals("250", res)
    }

    @Test
    fun testDivisionWithDecimals() {
        val res = evaluateExpression("100 / 3")
        assertEquals("33.33", res)
    }

    @Test
    fun testSingleNumber() {
        val res = evaluateExpression("500.50")
        assertEquals("500.5", res)
    }

    @Test
    fun testInvalidExpressionReturnsNull() {
        assertNull(evaluateExpression("abc + 10"))
        assertNull(evaluateExpression("100 / 0"))
        assertNull(evaluateExpression(""))
    }

    @Test
    fun testSplitBillLogic() {
        val totalAmount = BigDecimal("1500.00")
        val numPeople = 5
        val perPerson = totalAmount.divide(BigDecimal(numPeople), 2, RoundingMode.HALF_UP)
        
        assertEquals(BigDecimal("300.00"), perPerson)
        
        val summary = "Bill: Total ₹${totalAmount.toPlainString()} split among $numPeople people = ₹${perPerson.toPlainString()} each."
        assertTrue(summary.contains("₹300.00 each"))
    }

    @Test
    fun testSplitBillOddAmountRounding() {
        val totalAmount = BigDecimal("100.00")
        val numPeople = 3
        val perPerson = totalAmount.divide(BigDecimal(numPeople), 2, RoundingMode.HALF_UP)
        
        assertEquals(BigDecimal("33.33"), perPerson)
    }
}
