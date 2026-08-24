package com.example.myexpenditureapp.sms

data class SmsTransaction(
    val amount: Double,
    val merchant: String,
    val type: String = "Expense"
)

object SmsParser {
    private val patterns = listOf(
        // Pattern 1: Spent/Paid Rs. 100.00 at MERCHANT
        Regex("""(?i)(?:spent|paid|debited)\s+(?:rs\.?|inr|₹)\s*([\d,.]+)\s+(?:at|to)\s+([^.\n]+)"""),
        // Pattern 2: Transaction of Rs. 100.00 at MERCHANT
        Regex("""(?i)transaction\s+of\s+(?:rs\.?|inr|₹)\s*([\d,.]+)\s+at\s+([^.\n]+)"""),
        // Pattern 3: Your account ... has been debited by Rs 100.00 on ... at MERCHANT
        Regex("""(?i)debited\s+by\s+(?:rs\.?|inr|₹)\s*([\d,.]+).+?at\s+([^.\n]+)""")
    )

    fun parse(message: String): SmsTransaction? {
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                val merchant = match.groupValues[2].trim()
                if (amount != null) {
                    return SmsTransaction(amount, merchant)
                }
            }
        }
        return null
    }
}
