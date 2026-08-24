package com.example.myexpenditureapp.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun testParseSpent() {
        val message = "Spent Rs. 100.00 at Starbucks"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!.amount, 0.0)
        assertEquals("Starbucks", parsed.merchant)
    }

    @Test
    fun testParsePaid() {
        val message = "Paid INR 50.0 to Amazon"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(50.0, parsed!!.amount, 0.0)
        assertEquals("Amazon", parsed.merchant)
    }

    @Test
    fun testParseTransaction() {
        val message = "Transaction of Rs. 500.00 at Swiggy"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.0)
        assertEquals("Swiggy", parsed.merchant)
    }

    @Test
    fun testParseDebited() {
        val message = "Your account has been debited by Rs 1200.00 on 20-08-2026 at Walmart"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount, 0.0)
        assertEquals("Walmart", parsed.merchant)
    }
}
