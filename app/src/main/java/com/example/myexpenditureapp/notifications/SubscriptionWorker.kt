package com.example.myexpenditureapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.domain.radar.RadarStatus
import com.example.myexpenditureapp.domain.radar.SubscriptionRadarEngine
import kotlinx.coroutines.flow.first
import java.math.RoundingMode
import java.util.Calendar

class SubscriptionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Graph.provide(applicationContext)

        val prefs = applicationContext.getSharedPreferences("subscription_notifications", Context.MODE_PRIVATE)
        val todayDateKey = Calendar.getInstance().let {
            "${it.get(Calendar.YEAR)}_${it.get(Calendar.MONTH)}_${it.get(Calendar.DAY_OF_MONTH)}"
        }

        val activeSubscriptions = Graph.subscriptionRepository.getActiveSubscriptions().first()
        val now = System.currentTimeMillis()

        activeSubscriptions.forEach { subscription ->
            val billItem = SubscriptionRadarEngine.calculateBillItem(subscription, now)
            val notifKey = "notified_${subscription.id}_${billItem.status}_$todayDateKey"

            // Prevent notifying more than once per day for the same subscription status
            if (!prefs.getBoolean(notifKey, false)) {
                when (billItem.status) {
                    RadarStatus.DUE_TODAY,
                    RadarStatus.OVERDUE -> {
                        NotificationHelper.showSubscriptionDueNotification(
                            applicationContext,
                            subscriptionId = subscription.id,
                            subscriptionName = subscription.name,
                            amount = subscription.amount.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                            status = billItem.status,
                            daysDiff = billItem.daysRemaining
                        )
                        prefs.edit().putBoolean(notifKey, true).apply()
                    }
                    RadarStatus.DUE_SOON -> {
                        if (billItem.daysRemaining in 1..2) {
                            NotificationHelper.showSubscriptionDueNotification(
                                applicationContext,
                                subscriptionId = subscription.id,
                                subscriptionName = subscription.name,
                                amount = subscription.amount.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                                status = billItem.status,
                                daysDiff = billItem.daysRemaining
                            )
                            prefs.edit().putBoolean(notifKey, true).apply()
                        }
                    }
                    RadarStatus.PAID_THIS_CYCLE,
                    RadarStatus.UPCOMING -> {
                        // No notification required for paid or far upcoming bills
                    }
                }
            }
        }

        return Result.success()
    }
}
