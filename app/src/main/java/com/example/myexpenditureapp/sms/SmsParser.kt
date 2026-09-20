package com.example.myexpenditureapp.sms

import java.math.BigDecimal

data class SmsTransaction(
    val amount: BigDecimal,
    val merchant: String,
    val type: String = "Expense"
)

object SmsParser {
    // Non-transaction blacklist: OTPs, login codes, 2FA requests (these are authorizations, NOT completed transactions)
    private val OTP_PATTERNS = listOf(
        Regex("""(?i)\b(?:otp|one time password|verification code|security code|secret code|login code|vpa registration|auth code)\b"""),
        Regex("""(?i)\bdo not share (?:this|your)?\b"""),
        Regex("""(?i)\bnever share (?:your)?\b"""),
        Regex("""(?i)\bvalid for \d+\s*(?:mins|minutes|seconds)\b""")
    )

    // Promotional & marketing blacklist
    private val PROMO_PATTERNS = listOf(
        Regex("""(?i)\b(?:flat\s+(?:rs\.?|inr|₹)?\s*\d+\s*off|%\s*off|discount|coupon|promo code|promo|voucher|deal of the day)\b"""),
        Regex("""(?i)\b(?:cashback of up to|win up to|claim now|is waiting|valid till|hurry|exclusive offer)\b"""),
        Regex("""(?i)\b(?:pre-approved|apply for loan|instant loan|pre approved|loan limit|credit limit)\b"""),
        Regex("""(?i)\b(?:recharge now|data balance|plan expires|pack expires|bill due|bill generated|payment reminder|due on|due date)\b"""),
        Regex("""(?i)\b(?:cash prize|luckydraw|spin to win|congratulations|congrats|click here|apply now|shop now|buy now)\b""")
    )

    // Strict bank / payment confirmation indicators (must be present if promo patterns match)
    private val STRICT_BANK_SETTLEMENT_PATTERNS = listOf(
        Regex("""(?i)\b(?:debited (?:from|by|for)|credited (?:to|with|for)|spent on your|card ending \d+|a/c (?:no\.?\s*)?[x\*\d]+ (?:is|has been)?\s*(?:debited|credited))\b"""),
        Regex("""(?i)\b(?:transferred to vpa|sent to vpa|vpa pay of)\b""")
    )

    private val INCOME_KEYWORDS = listOf(
        "credited", "received", "added to account", "refund", "refunded", "cashback credited", "deposited", "cr.", "cr ", "credit", "reversal", "salary"
    )

    private val EXPENSE_KEYWORDS = listOf(
        "debited", "spent", "paid", "dr.", "dr ", "debit", "sent", "purchased", "purchase of",
        "withdrawn", "withdrew", "transferred to", "transferred", "charged", "vpa pay",
        "payment to", "payment of", "payment for", "payment at",
        "transaction of", "transaction at", "txn of", "txn at", "successful from"
    )

    // Patterns specifically identifying the transaction amount
    private val TXN_AMOUNT_PATTERNS = listOf(
        Regex("""(?i)(?:(?:debited|credited|spent|paid|transferred|withdrawn|charged|added|received|refund(?:ed)?)\s+(?:by|for|of|with|amounting to)?\s*(?:rs\.?|inr|₹|re\.?|\$|usd|€|eur|£|gbp)?\s*([\d,]+\.?\d*))"""),
        Regex("""(?i)(?:rs\.?|inr|₹|re\.?|\$|usd|€|eur|£|gbp)\s*([\d,]+\.?\d*)\s*(?:(?:debited|credited|spent|paid|transferred|withdrawn|charged|added|received|refund(?:ed)?))"""),
        Regex("""(?i)(?:txn of|transaction of|payment of|vpa pay of|purchase of)\s*(?:rs\.?|inr|₹|re\.?|\$|usd|€|eur|£|gbp)?\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)(?:rs\.?|inr|₹|re\.?|\$|usd|€|eur|£|gbp)\s*([\d,]+\.?\d*)\s+spent\s+on"""),
        Regex("""(?i)(?:rs\.?|inr|₹|re\.?|\$|usd|€|eur|£|gbp)\s*([\d,]+\.?\d*)\s+(?:successful|done)\s+from"""),
        Regex("""(?i)(?:for|amount)\s+(?:rs\.?|inr|₹|re\.?|\$|usd|€|eur|£|gbp)\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)(?:rs\.?|inr|₹|re\.?|\$|usd|€|eur|£|gbp)\.?\s*([\d,]+\.?\d*)""")
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?i)\binfo:\s*([^.\n\r]+)"""),
        Regex("""(?i)\bspent\s+on\s+.*?\s+at\s+([^.\n\r]+)"""),
        Regex("""(?i)\b(?:card|acct|account|a/c)\s+(?:ending\s+\d+|\*+\d+|\d+)?\s*(?:at|to)\s+([^.\n\r]+)"""),
        Regex("""(?i)\b(?:purchased|spent)\s+at\s+([^.\n\r]+)"""),
        Regex("""(?i)\bpayment\s+(?:to|for)\s+([^.\n\r]+)"""),
        Regex("""(?i)\bpaid\s+(?:to|at)\s+([^.\n\r]+)"""),
        Regex("""(?i)\btransferred\s+to\s+([^.\n\r]+)"""),
        Regex("""(?i)\bin\s+favor\s+of\s+([^.\n\r]+)"""),
        Regex("""(?i)\btowards\s+([^.\n\r]+)"""),
        Regex("""(?i)\bvpa\s+([a-zA-Z0-9_\.\-]+(?:@[a-zA-Z0-9_\.\-]+)?)"""),
        Regex("""(?i)\bat\s+([^.\n\r]+)"""),
        Regex("""(?i)\bto\s+([^.\n\r]+)"""),
        Regex("""(?i)\b(?:received|credited)\s+from\s+([^.\n\r]+)"""),
        Regex("""(?i)\bfrom\s+([^.\n\r]+)""")
    )

    fun parse(message: String): SmsTransaction? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null

        // 1. Unconditionally reject OTPs and auth verification codes
        if (OTP_PATTERNS.any { it.containsMatchIn(trimmed) }) {
            return null
        }

        // 2. Reject marketing / promo / spam messages unless they are strict bank settlements
        val isPromo = PROMO_PATTERNS.any { it.containsMatchIn(trimmed) }
        val isStrictBankSettlement = STRICT_BANK_SETTLEMENT_PATTERNS.any { it.containsMatchIn(trimmed) }
        if (isPromo && !isStrictBankSettlement) {
            return null
        }

        // 3. Classify and require a clear transaction action (Income or Expense)
        val isIncome = INCOME_KEYWORDS.any { trimmed.contains(it, ignoreCase = true) }
        val isExpense = EXPENSE_KEYWORDS.any { trimmed.contains(it, ignoreCase = true) }

        // If neither income nor expense action is present, it's not a transactional message
        if (!isIncome && !isExpense) {
            return null
        }

        val type = when {
            isIncome && !isExpense -> "Income"
            isExpense && !isIncome -> "Expense"
            isIncome && isExpense -> {
                val creditIndex = trimmed.lowercase().indexOf("credit").let { if (it == -1) trimmed.lowercase().indexOf("credited") else it }
                val debitIndex = trimmed.lowercase().indexOf("debit").let { if (it == -1) trimmed.lowercase().indexOf("debited") else it }
                if (creditIndex in 0 until debitIndex) "Income" else "Expense"
            }
            else -> return null
        }

        // 4. Extract Amount
        var amount: BigDecimal? = null
        for (pattern in TXN_AMOUNT_PATTERNS) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val rawAmountStr = match.groupValues[1].replace(",", "").trim()
                if (rawAmountStr.isNotEmpty() && rawAmountStr != ".") {
                    val parsedAmt = try { BigDecimal(rawAmountStr) } catch (e: Exception) { null }
                    if (parsedAmt != null && parsedAmt > BigDecimal.ZERO && parsedAmt < BigDecimal("100000000")) {
                        amount = parsedAmt
                        break
                    }
                }
            }
        }
        if (amount == null) return null

        // 5. Extract Merchant / Beneficiary
        var merchant = "Unknown"
        val filteredMessage = trimmed
            .replace(Regex("""(?i)\bto your (?:account|acct|a/c)\b"""), "")
            .replace(Regex("""(?i)\bfrom your (?:account|acct|a/c)\b"""), "")
            .replace(Regex("""(?i)\bto (?:your|my) (?:bank\s+)?(?:account|acct|a/c)\b"""), "")
            .replace(Regex("""(?i)\bfrom (?:your|my) (?:bank\s+)?(?:account|acct|a/c)\b"""), "")

        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(filteredMessage)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                val lower = candidate.lowercase()
                if (lower != "pay" && lower != "payment" && lower != "your" && lower != "account" && lower != "acct" && lower != "a/c" && lower != "of") {
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
        clean = clean.replace(Regex("""(?i)^(?:your\s+)?(?:account|bank|card|vpa\s+pay\s+of|vpa\s+pay\s+to|vpa|a/c|vps\*|neft\*|upi\s+ref|txn|pay(?:ment)?\s+to)\s*[\*:]?\s*"""), "")
        clean = clean.replace(Regex("""(?i)^(?:dear\s+customer\s*,?\s*)"""), "")

        // Remove UPI VPA handle domain (@upi, @okaxis, @okhdfcbank, @icici, etc.)
        clean = clean.replace(Regex("""@[a-zA-Z0-9_\.\-]+"""), "")

        // Remove common endings like ending 1234
        clean = clean.replace(Regex("""(?i)\s+ending\s+\d+.*$"""), "")
        clean = clean.replace(Regex("""(?i)\s+ending\s+in\s+\d+.*$"""), "")
        clean = clean.replace(Regex("""(?i)\s+card\s+ending.*$"""), "")

        // Split by common separators/terminators (case insensitive with word boundaries where needed)
        val separators = listOf(
            "\\s+on\\s+", "\\s+date\\s+", "\\s+ref(?:erence)?\\b", "\\s+avl(?:\\s+bal)?\\b",
            "\\s+bal(?:ance)?\\b", "\\s+ending\\b", "\\s+for\\s+(?:rs|inr|₹|usd|eur|\\d)",
            "\\s+from\\s+", "\\s+is\\s+", "\\s+has\\s+", "\\s+with\\s+", "\\s+using\\s+",
            "\\s+\\(", "\\s+;", "\\s+,", "\\s+of\\s+(?:rs|inr|₹|usd|eur|\\d)",
            "\\s+txn\\b", "\\s+id\\b", "\\s+rrn\\b", "\\s+not\\s+you\\b", "\\s+call\\s+\\d+",
            "\\s+by\\s+a/c\\b", "\\s+linked\\s+to\\b"
        )

        for (sep in separators) {
            val regex = Regex("(?i)$sep")
            val match = regex.find(clean)
            if (match != null) {
                clean = clean.substring(0, match.range.first).trim()
            }
        }

        // Handle asterisk separated info (common in ICICI/HDFC, e.g. VPS*Zomato*123 or NEFT*SALARY)
        if (clean.contains("*")) {
            val parts = clean.split("*")
            clean = if (parts.size > 1 && (parts[0].trim().equals("VPS", ignoreCase = true) ||
                        parts[0].trim().equals("NEFT", ignoreCase = true) ||
                        parts[0].trim().equals("UPI", ignoreCase = true) ||
                        parts[0].trim().equals("IMPS", ignoreCase = true) ||
                        parts[0].trim().equals("RTGS", ignoreCase = true))) {
                parts[1]
            } else {
                parts[0]
            }
        }

        // Remove numeric IDs at the end (4 or more digits)
        clean = clean.replace(Regex("""\s+\d{4,}$"""), "")

        // Remove trailing non-alphanumeric chars except quotes
        clean = clean.replace(Regex("""[^a-zA-Z0-9\s'\.\-]+$"""), "").trim()

        return if (clean.isEmpty() || clean.length < 2) "UNKNOWN" else clean.uppercase()
    }
}
