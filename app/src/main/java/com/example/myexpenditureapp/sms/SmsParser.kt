package com.example.myexpenditureapp.sms

import java.math.BigDecimal

data class SmsTransaction(
    val amount: BigDecimal,
    val merchant: String,
    val type: String = "Expense"
)

object SmsParser {
    private val AMOUNT_PATTERN = Regex("""(?i)(?:rs\.?|inr|₹|re\.?)\s*([\d,]+\.?\d*)""")
    
    private val INCOME_KEYWORDS = listOf("credited", "received", "added", "refund", "cashback", "deposited", "cr ", "credit")
    private val EXPENSE_KEYWORDS = listOf("debited", "spent", "paid", "dr ", "debit", "sent")
    
    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?i)info:\s*([^.\n]+)"""),
        Regex("""(?i)purchased\s+at\s+([^.\n]+)"""),
        Regex("""(?i)transferred\s+to\s+([^.\n]+)"""),
        Regex("""(?i)payment\s+to\s+([^.\n]+)"""),
        Regex("""(?i)vpa\s+([^@\s\.]+)(?:@upi)?"""),
        Regex("""(?i)to\s+([^.\n\d]+)(?:\s+\(|from)"""),
        Regex("""(?i)from\s+([^.\n\d]+)"""),
        Regex("""(?i)\s+at\s+([^.\n\d]+)"""),
        Regex("""(?i)\s+to\s+([^.\n\d]+)"""),
        Regex("""(?i)\s+on\s+([^.\n\d]+)""")
    )

    fun parse(message: String): SmsTransaction? {
        val amountMatch = AMOUNT_PATTERN.find(message) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = try { BigDecimal(amountStr) } catch (e: Exception) { null } ?: return null

        val isIncome = INCOME_KEYWORDS.any { message.contains(it, ignoreCase = true) }
        val isExpense = EXPENSE_KEYWORDS.any { message.contains(it, ignoreCase = true) }
        
        val type = when {
            isIncome && !isExpense -> "Income"
            isExpense && !isIncome -> "Expense"
            isIncome && isExpense -> {
                if (message.lowercase().indexOf("credit") < message.lowercase().indexOf("debit")) "Income" else "Expense"
            }
            else -> "Expense"
        }

        var merchant = "Unknown"
        
        // Filter out "your account" etc from the start
        val filteredMessage = message.replace(Regex("""(?i)to your (?:account|a/c)"""), "")

        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(filteredMessage)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.lowercase() != "pay" && candidate.lowercase() != "payment") {
                    merchant = candidate
                    break
                }
            }
        }

        merchant = cleanMerchant(merchant)

        return SmsTransaction(amount, merchant, type)
    }

    private fun cleanMerchant(raw: String): String {
        var clean = raw
        
        // Remove common prefixes
        clean = clean.replace(Regex("""(?i)^(?:your\s+)?(?:account|bank|card|vpa|a/c|vps\*|neft\*|upi\s+ref|txn|pay(?:ment)?\s+to)\s*[\*:]?\s*"""), "")
        
        // Remove common endings like ending 1234
        clean = clean.replace(Regex("""(?i)\s+ending\s+\d+.*$"""), "")
        
        // Split by common separators/terminators (case insensitive)
        val separators = listOf(
            "\\s+on\\s+", "\\s+date\\s+", "\\s+ref(?:erence)?\\s+", "\\s+avl\\s+", 
            "\\s+ending\\s+", "\\s+for\\s+", "\\s+at\\s+", "\\s+from\\s+", 
            "\\s+to\\s+", "\\s+is\\s+", "\\s+has\\s+", "\\s+with\\s+", 
            "\\s+\\(", "\\s+;", "\\s+,", "\\s+of\\s+rs", "\\s+on$", "\\s+at$",
            "\\s+ref\\b", "\\s+txn\\b", "\\s+id\\b"
        )
        
        for (sep in separators) {
            val regex = Regex("(?i)$sep")
            val match = regex.find(clean)
            if (match != null) {
                clean = clean.substring(0, match.range.first).trim()
            }
        }
        
        // Handle asterisk separated info (common in ICICI/HDFC)
        if (clean.contains("*")) {
             val parts = clean.split("*")
             // Often it's VPS*Zomato*123 or NEFT*SALARY
             clean = if (parts.size > 1 && (parts[0].trim().equals("VPS", ignoreCase = true) || parts[0].trim().equals("NEFT", ignoreCase = true) || parts[0].trim().equals("UPI", ignoreCase = true))) {
                 parts[1]
             } else {
                 parts[0]
             }
        }

        // Remove numeric IDs at the end (4 or more digits)
        clean = clean.replace(Regex("""\s+\d{4,}$"""), "")

        // Remove trailing non-alphanumeric chars
        clean = clean.replace(Regex("""[^a-zA-Z0-9\s']+$"""), "").trim()
        
        return if (clean.isEmpty() || clean.length < 2) "UNKNOWN" else clean.uppercase()
    }
}
