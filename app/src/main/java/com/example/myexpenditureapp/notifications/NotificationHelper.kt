package com.example.myexpenditureapp.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.myexpenditureapp.MainActivity
import com.example.myexpenditureapp.R
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_REVIEW = "channel_review"
    const val CHANNEL_BUDGET = "channel_budget"
    const val CHANNEL_DAILY = "channel_daily"
    const val CHANNEL_MONTHLY = "channel_monthly"
    const val CHANNEL_SUBSCRIPTION = "channel_subscription"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reviewChannel = NotificationChannel(
                CHANNEL_REVIEW,
                "Transaction Review",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new transactions that need review"
            }

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when you approach or exceed your budget limits"
            }

            val subscriptionChannel = NotificationChannel(
                CHANNEL_SUBSCRIPTION,
                "Subscription & Bill Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming and due recurring subscription payments"
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "Daily Reflection",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily summary of your spending"
            }

            val monthlyChannel = NotificationChannel(
                CHANNEL_MONTHLY,
                "Monthly Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Monthly net flow summary"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(reviewChannel, budgetChannel, subscriptionChannel, dailyChannel, monthlyChannel))
        }
    }

    @SuppressLint("MissingPermission")
    fun showReviewNotification(context: Context, merchant: String, amount: String) {
        if (!hasPostNotificationPermission(context)) return

        val formattedAmt = amount.toBigDecimalOrNull()?.formatIndian() ?: "₹$amount"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 100, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("New Transaction: $merchant")
            .bigText("$formattedAmt was captured from SMS/UPI.\nTap to review and assign category or save as rule.")
            .setSummaryText("Smart Inbox")

        val builder = NotificationCompat.Builder(context, CHANNEL_REVIEW)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Review Required: $merchant")
            .setContentText("$formattedAmt • Tap to review category")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Open Review", pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFFBBF24.toInt()) // Gold Accent

        try {
            NotificationManagerCompat.from(context).notify(200, builder.build())
        } catch (e: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showBudgetAlert(context: Context, categoryName: String, percent: Int) {
        if (!hasPostNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 101, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("⚠️ Budget Limit Alert: $categoryName")
            .bigText("You have reached $percent% of your monthly budget limit for $categoryName.\nConsider pacing your expenses.")
            .setSummaryText("Budget Guard")

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Budget Alert: $categoryName ($percent%)")
            .setContentText("You've reached $percent% of your budget for $categoryName.")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFF43F5E.toInt()) // Expense Red

        try {
            NotificationManagerCompat.from(context).notify(categoryName.hashCode(), builder.build())
        } catch (e: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showDailyReflection(context: Context, todaySpend: String, monthlyNetFlow: String) {
        if (!hasPostNotificationPermission(context)) return

        val formattedToday = todaySpend.toBigDecimalOrNull()?.formatIndian() ?: "₹$todaySpend"
        val formattedNet = monthlyNetFlow.toBigDecimalOrNull()?.formatIndian() ?: "₹$monthlyNetFlow"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 102, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("🌙 Daily Spending Reflection")
            .bigText("Today's Total Outflow: $formattedToday\nMonth-to-Date Net Flow: $formattedNet\nKeep track of your financial habits!")
            .setSummaryText("Daily Summary")

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Daily Reflection")
            .setContentText("Today: $formattedToday | Net Month: $formattedNet")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF6366F1.toInt()) // Indigo Primary

        try {
            NotificationManagerCompat.from(context).notify(300, builder.build())
        } catch (e: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showMonthlySummary(context: Context, netFlow: String) {
        if (!hasPostNotificationPermission(context)) return

        val formattedNet = netFlow.toBigDecimalOrNull()?.formatIndian() ?: "₹$netFlow"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 103, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("🎉 Monthly Financial Victory!")
            .bigText("You closed the month with a net savings flow of $formattedNet.\nGreat discipline! Keep building your wealth.")
            .setSummaryText("Monthly Report")

        val builder = NotificationCompat.Builder(context, CHANNEL_MONTHLY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Monthly Victory: $formattedNet")
            .setContentText("Great month! Your net flow was $formattedNet.")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF10B981.toInt()) // Income Green

        try {
            NotificationManagerCompat.from(context).notify(400, builder.build())
        } catch (e: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showSubscriptionDueNotification(
        context: Context,
        subscriptionId: Long,
        subscriptionName: String,
        amount: String,
        status: com.example.myexpenditureapp.domain.radar.RadarStatus,
        daysDiff: Int
    ) {
        if (!hasPostNotificationPermission(context)) return

        val formattedAmt = amount.toBigDecimalOrNull()?.formatIndian() ?: "₹$amount"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("shortcut_action", "subscriptions")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, (1000 + subscriptionId).toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (title, text, accentColor) = when (status) {
            com.example.myexpenditureapp.domain.radar.RadarStatus.DUE_TODAY -> Triple(
                "📅 Subscription Due Today: $subscriptionName",
                "Your payment of $formattedAmt is due today. Ensure sufficient balance in your account.",
                0xFFF59E0B.toInt() // Amber Warning
            )
            com.example.myexpenditureapp.domain.radar.RadarStatus.OVERDUE -> Triple(
                "⚠️ Overdue Payment: $subscriptionName",
                "Payment of $formattedAmt was due ${Math.abs(daysDiff)} days ago. Tap to mark as paid or review.",
                0xFFF43F5E.toInt() // Expense Red
            )
            else -> Triple(
                "🔔 Upcoming Bill: $subscriptionName",
                "$formattedAmt is due in $daysDiff days. Tap to manage in Subscription Radar.",
                0xFF6366F1.toInt() // Indigo Primary
            )
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText("$text\n\nTrack all recurring commitments in Subscription Radar.")
            .setSummaryText("Subscription Radar")

        val builder = NotificationCompat.Builder(context, CHANNEL_SUBSCRIPTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "View Radar", pendingIntent)
            .setAutoCancel(true)
            .setColor(accentColor)

        try {
            NotificationManagerCompat.from(context).notify((5000 + subscriptionId).toInt(), builder.build())
        } catch (e: SecurityException) {
        }
    }

    private fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Daily Reflection at 9:00 PM
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 21)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = calendar.timeInMillis - now

        val dailyRequest = PeriodicWorkRequestBuilder<DailyReflectionWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyReflection",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )

        // Budget Worker - Periodic every 4 hours as a backup
        val budgetRequest = PeriodicWorkRequestBuilder<BudgetWorker>(4, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "BudgetCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetRequest
        )

        // Subscription Reminder Worker - Periodic every 12 hours
        val subscriptionRequest = PeriodicWorkRequestBuilder<SubscriptionWorker>(12, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "SubscriptionReminderCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            subscriptionRequest
        )
    }

    fun triggerBudgetCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val budgetRequest = OneTimeWorkRequestBuilder<BudgetWorker>().build()
        workManager.enqueue(budgetRequest)
    }

    fun triggerSubscriptionCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val subscriptionRequest = OneTimeWorkRequestBuilder<SubscriptionWorker>().build()
        workManager.enqueue(subscriptionRequest)
    }

    fun isNotificationListenerAccessGranted(context: Context): Boolean {
        val packageName = context.packageName
        val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isEnabledInSettings = flat != null && flat.contains(packageName)
        val isEnabledInCompat = NotificationManagerCompat.getEnabledListenerPackages(context).contains(packageName)
        return isEnabledInSettings || isEnabledInCompat
    }
}
