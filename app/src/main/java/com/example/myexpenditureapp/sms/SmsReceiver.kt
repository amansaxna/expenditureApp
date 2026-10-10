package com.example.myexpenditureapp.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Transaction
import com.example.myexpenditureapp.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Graph.provide(context)
        NotificationHelper.createNotificationChannels(context)
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                val address = sms.displayOriginatingAddress ?: "Unknown"
                val timestamp = sms.timestampMillis
                Log.d("SmsReceiver", "Received SMS from $address: $body")

                val parsed = SmsParser.parse(body)
                if (parsed != null) {
                    Log.d("SmsReceiver", "Parsed transaction: $parsed")
                    saveTransaction(context, parsed, address, timestamp, body)
                    scope.launch {
                        SmsVerificationState.notifySmsParsed("SMS Parsed: ${parsed.merchant} - ₹${parsed.amount}")
                    }
                }
            }
        }
    }

    private fun saveTransaction(context: Context, smsTx: SmsTransaction, sender: String, timestamp: Long, rawMessage: String) {
        scope.launch {
            val txTime = if (timestamp > 0) timestamp else System.currentTimeMillis()
            val smsId = "sms_${sender}_${txTime}_${smsTx.amount}"
            
            // Check for duplicates
            val windowStart = txTime - 180_000L
            val windowEnd = txTime + 180_000L
            if (Graph.transactionRepository.existsBySmsId(smsId) ||
                Graph.transactionRepository.existsSimilarTransaction(smsTx.amount, smsTx.type, windowStart, windowEnd)) {
                Log.d("SmsReceiver", "Transaction already exists or duplicate detected for $smsId")
                return@launch
            }

            val accounts = Graph.accountRepository.getAllAccounts().first()
            var accountId = accounts.firstOrNull()?.id

            if (accountId == null) {
                Log.d("SmsReceiver", "No accounts found. Creating default Main Account.")
                val defaultAccount = com.example.myexpenditureapp.data.entity.Account(
                    name = "Main Wallet",
                    type = "Wallet",
                    balance = BigDecimal.ZERO
                )
                accountId = Graph.saveAccountUseCase(defaultAccount)
            }

            val matchedCategoryId = Graph.autoCategoryRuleRepository.getMatchingCategoryId(smsTx.merchant)
            val transaction = Transaction(
                accountId = accountId,
                    categoryId = matchedCategoryId,
                    amount = smsTx.amount,
                    merchant = smsTx.merchant,
                    timestamp = txTime,
                    type = smsTx.type,
                    smsId = smsId,
                    isReviewed = false,
                    rawMessage = rawMessage
                )
                Graph.saveTransactionUseCase(transaction)
                val savedTxId = Graph.transactionRepository.getUnreviewedTransactions().first().find { it.smsId == smsId }?.id ?: 0L
                if (savedTxId != 0L) {
                    com.example.myexpenditureapp.overlay.TransactionOverlayActivity.launchIfAllowed(context, savedTxId)
                }

                NotificationHelper.showReviewNotification(
                    context,
                    smsTx.merchant,
                    smsTx.amount.toPlainString()
                )
                NotificationHelper.triggerBudgetCheck(context)
                Log.d("SmsReceiver", "Saved transaction to review: ${smsTx.merchant}")
        }
    }
}
