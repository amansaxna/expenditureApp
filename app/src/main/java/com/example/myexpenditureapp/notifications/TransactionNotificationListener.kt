package com.example.myexpenditureapp.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Account
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TransactionNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "TxNotifListener"
        // Known banking and UPI app packages
        private val PAYMENT_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "net.one97.paytm",                        // Paytm
            "com.phonepe.app",                        // PhonePe
            "com.freecharge.android",                 // FreeCharge
            "in.org.npci.upiapp",                     // BHIM
            "com.mobikwik_new",                       // MobiKwik
            "com.dreamplug.androidapp",               // CRED
            "com.sbi.lotusintouch",                   // YONO SBI
            "com.csam.icici.bank.imobile",            // iMobile ICICI
            "com.hdfcbank.netbanking",                // HDFC MobileBanking
            "com.axis.mobile",                        // Axis Mobile
            "com.kotak.kotak_bank",                   // Kotak Mobile Banking
            "com.amazon.mShop.android.shopping",      // Amazon Pay
            "com.whatsapp"                            // WhatsApp Pay
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: ""
        val extras = sbn.notification.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val combinedMessage = "$title. $text $bigText".trim()
        if (combinedMessage.isBlank()) return

        // Process if package is a payment app OR message contains financial keywords
        val isPaymentApp = PAYMENT_PACKAGES.contains(pkg)
        val hasFinancialKeywords = combinedMessage.contains("debited", ignoreCase = true) ||
                combinedMessage.contains("credited", ignoreCase = true) ||
                combinedMessage.contains("paid", ignoreCase = true) ||
                combinedMessage.contains("sent", ignoreCase = true) ||
                combinedMessage.contains("received", ignoreCase = true) ||
                combinedMessage.contains("spent", ignoreCase = true) ||
                combinedMessage.contains("UPI", ignoreCase = true) ||
                combinedMessage.contains("₹", ignoreCase = true) ||
                combinedMessage.contains("Rs", ignoreCase = true) ||
                combinedMessage.contains("INR", ignoreCase = true)

        if (!isPaymentApp && !hasFinancialKeywords) return

        Log.d(TAG, "Analyzing notification from $pkg: $combinedMessage")

        val parsed = SmsParser.parse(combinedMessage)
        if (parsed != null) {
            Log.d(TAG, "Parsed notification transaction: $parsed")
            val notificationId = "notif_${pkg}_${sbn.postTime}_${parsed.amount}"
            processTransaction(notificationId, parsed.amount, parsed.merchant, parsed.type, combinedMessage)
        }
    }

    private fun processTransaction(uniqueId: String, amount: BigDecimal, merchant: String, type: String, rawMessage: String) {
        scope.launch {
            try {
                Graph.provide(applicationContext)

                // Check for duplicate
                if (Graph.transactionRepository.existsBySmsId(uniqueId)) {
                    Log.d(TAG, "Duplicate transaction for $uniqueId")
                    return@launch
                }

                val accounts = Graph.accountRepository.getAllAccounts().first()
                var accountId = accounts.firstOrNull()?.id

                if (accountId == null) {
                    val defaultAccount = Account(
                        name = "Main Wallet",
                        type = "Wallet",
                        balance = BigDecimal.ZERO
                    )
                    accountId = Graph.saveAccountUseCase(defaultAccount)
                }

                // Check Auto-Categorization Rule
                val matchedCategoryId = Graph.autoCategoryRuleRepository.getMatchingCategoryId(merchant)

                val transaction = Transaction(
                    accountId = accountId,
                    categoryId = matchedCategoryId,
                    amount = amount,
                    merchant = merchant,
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    smsId = uniqueId,
                    isReviewed = false,
                    rawMessage = rawMessage
                )

                Graph.saveTransactionUseCase(transaction)
                NotificationHelper.showReviewNotification(
                    applicationContext,
                    merchant,
                    amount.toPlainString()
                )
                NotificationHelper.triggerBudgetCheck(applicationContext)
                Log.d(TAG, "Successfully captured & saved notification transaction: $merchant")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification transaction", e)
            }
        }
    }
}
