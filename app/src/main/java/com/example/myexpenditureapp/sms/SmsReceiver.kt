package com.example.myexpenditureapp.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Graph.provide(context)
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                val address = sms.displayOriginatingAddress ?: "Unknown"
                Log.d("SmsReceiver", "Received SMS from $address: $body")

                val parsed = SmsParser.parse(body)
                if (parsed != null) {
                    Log.d("SmsReceiver", "Parsed transaction: $parsed")
                    saveTransaction(parsed, address)
                }
            }
        }
    }

    private fun saveTransaction(smsTx: SmsTransaction, sender: String) {
        scope.launch {
            val accounts = Graph.accountRepository.getAllAccounts().first()
            val accountId = accounts.firstOrNull()?.id ?: return@launch // No account to link to

            val transaction = Transaction(
                accountId = accountId,
                categoryId = null,
                amount = smsTx.amount,
                merchant = smsTx.merchant,
                timestamp = System.currentTimeMillis(),
                type = smsTx.type,
                smsId = sender + "_" + System.currentTimeMillis()
            )
            Graph.saveTransactionUseCase(transaction)
        }
    }
}
