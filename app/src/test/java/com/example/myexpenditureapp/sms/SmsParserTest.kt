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
        assertEquals(100.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("STARBUCKS", parsed.merchant)
    }

    @Test
    fun testParsePaid() {
        val message = "Paid INR 50.0 to Amazon"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(50.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("AMAZON", parsed.merchant)
    }

    @Test
    fun testParseTransaction() {
        val message = "Transaction of Rs. 500.00 at Swiggy"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("SWIGGY", parsed.merchant)
    }

    @Test
    fun testParseDebited() {
        val message = "Your account has been debited by Rs 1200.00 on 20-08-2026 at Walmart"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("WALMART", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParseCredited() {
        val message = "Credited ₹ 5000.0 to your account from HDFC Bank"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(5000.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("HDFC BANK", parsed.merchant)
        assertEquals("Income", parsed.type)
    }

    @Test
    fun testParseVpaPay() {
        val message = "VPA Pay of Rs. 200.00 to Zomato ref 123456"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(200.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun testParseMerchantWithDate() {
        val message = "Spent Rs. 150.00 at Netflix on 21-08-2026"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(150.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("NETFLIX", parsed.merchant)
    }

    @Test
    fun testParseHdfcCreditCard() {
        val message = "Alert: You've spent Rs. 1500.00 on your HDFC Bank Credit Card ending 1234 at AMAZON INDIA on 2026-08-25. Avl Bal: Rs. 50000.00"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(1500.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("AMAZON INDIA", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParseIciciDebit() {
        val message = "Dear Customer, your Acct XXXXXX123 has been debited for INR 500.00 on 25-Aug-26. Info: VPS*Zomato*123. Avail Bal: INR 10000.00"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("ZOMATO", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParseSbiUpi() {
        val message = "Your a/c no. XXXXXXXX1234 is debited for Rs 200.00 on 25-08-26 by a/c linked to VPA swiggy@upi. (Gen Ref No 1234567890)"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(200.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("SWIGGY", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParseUpiMoneySent() {
        val message = "Money Sent: Rs. 100.00 to JOHN DOE (zomato@upi) from your Bank Acct ... Ref 123456."
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("JOHN DOE", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParseCreditedSalary() {
        val message = "Dear Customer, your Acct XXXXXX123 has been credited with INR 50000.00 on 25-Aug-26. Info: NEFT*SALARY. Avail Bal: INR 61000.00"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(50000.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("SALARY", parsed.merchant)
        assertEquals("Income", parsed.type)
    }

    @Test
    fun testParseAxisCard() {
        val message = "Transaction alert: INR 150.00 spent on Axis Bank Card ending 1234 at FLIPKART on 25-08-26. Total Bal: INR 2000.00"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(150.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("FLIPKART", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParseHdfcDebit() {
        val message = "Rs.2000.00 debited from a/c **1234 on 25-08-26 to VPA swiggy@upi (Ref no 1234567890). Not you? Call 1800..."
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(2000.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("SWIGGY", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParseSbiCredit() {
        val message = "Dear Customer, your a/c no. XXXXXXXX1234 is credited by Rs 500.00 on 25-08-26 by a/c linked to VPA cash@upi. (Gen Ref No 1234567890)"
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("CASH", parsed.merchant)
        assertEquals("Income", parsed.type)
    }

    @Test
    fun testParseUpiPaymentTo() {
        val message = "Payment to McDonald's of Rs. 450.00 successful from your Bank Account ending 1234."
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(450.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("MCDONALD'S", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }

    @Test
    fun testParsePurchasedAt() {
        val message = "You have purchased at BIG BAZAAR for Rs. 1200.00 using your Card ending 5678 on 2026-08-26."
        val parsed = SmsParser.parse(message)
        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount.toDouble(), 0.0)
        assertEquals("BIG BAZAAR", parsed.merchant)
        assertEquals("Expense", parsed.type)
    }
}
