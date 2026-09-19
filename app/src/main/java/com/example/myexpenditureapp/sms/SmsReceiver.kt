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
                Log.d("SmsReceiver", "Received SMS from $address: $body")

                val parsed = SmsParser.parse(body)
                if (parsed != null) {
                    Log.d("SmsReceiver", "Parsed transaction: $parsed")
                    saveTransaction(context, parsed, address, body)
                    scope.launch {
                        SmsVerificationState.notifySmsParsed("SMS Parsed: ${parsed.merchant} - ₹${parsed.amount}")
                    }
                }
            }
        }
    }

    private fun saveTransaction(context: Context, smsTx: SmsTransaction, sender: String, rawMessage: String) {
        scope.launch {
            val smsId = sender + "_" + System.currentTimeMillis()
            
            // Check for duplicates
            if (Graph.transactionRepository.existsBySmsId(smsId)) {
                Log.d("SmsReceiver", "Transaction already exists for $smsId")
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

            if (accountId != null) {
                val matchedCategoryId = Graph.autoCategoryRuleRepository.getMatchingCategoryId(smsTx.merchant)
                val transaction = Transaction(
                    accountId = accountId,
                    categoryId = matchedCategoryId,
                    amount = smsTx.amount,
                    merchant = smsTx.merchant,
                    timestamp = System.currentTimeMillis(),
                    type = smsTx.type,
                    smsId = smsId,
                    isReviewed = false,
                    rawMessage = rawMessage
                )
                Graph.saveTransactionUseCase(transaction)
                NotificationHelper.showReviewNotification(
                    context,
                    smsTx.merchant,
                    smsTx.amount.toPlainString()
                )
                NotificationHelper.triggerBudgetCheck(context)
                Log.d("SmsReceiver", "Saved transaction to review: ${smsTx.merchant}")
            } else {
                Log.e("SmsReceiver", "Failed to resolve account ID even after creation.")
            }
        }
    }
}
