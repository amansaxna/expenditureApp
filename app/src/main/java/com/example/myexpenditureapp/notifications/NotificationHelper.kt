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
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_REVIEW = "channel_review"
    const val CHANNEL_BUDGET = "channel_budget"
    const val CHANNEL_DAILY = "channel_daily"
    const val CHANNEL_MONTHLY = "channel_monthly"

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
            manager.createNotificationChannels(listOf(reviewChannel, budgetChannel, dailyChannel, monthlyChannel))
        }
    }

    @SuppressLint("MissingPermission")
    fun showReviewNotification(context: Context, merchant: String, amount: String) {
        if (!hasPostNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 100, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REVIEW)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Review Required")
            .setContentText("New transaction at $merchant: ₹$amount")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFFBBF24.toInt()) // Gold

        try {
            NotificationManagerCompat.from(context).notify(200, builder.build())
        } catch (e: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showBudgetAlert(context: Context, categoryName: String, percent: Int) {
        if (!hasPostNotificationPermission(context)) return

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Budget Alert: $categoryName")
            .setContentText("You've reached $percent% of your budget for $categoryName.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFFEF4444.toInt()) // Red

        try {
            NotificationManagerCompat.from(context).notify(categoryName.hashCode(), builder.build())
        } catch (e: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showDailyReflection(context: Context, todaySpend: String, monthlyNetFlow: String) {
        if (!hasPostNotificationPermission(context)) return

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Daily Reflection")
            .setContentText("Today's Spend: ₹$todaySpend | Month Net: ₹$monthlyNetFlow")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFF3B82F6.toInt()) // Blue

        try {
            NotificationManagerCompat.from(context).notify(300, builder.build())
        } catch (e: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showMonthlySummary(context: Context, netFlow: String) {
        if (!hasPostNotificationPermission(context)) return

        val builder = NotificationCompat.Builder(context, CHANNEL_MONTHLY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Monthly Victory!")
            .setContentText("Great month! Your net flow was ₹$netFlow. Keep it up!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFF10B981.toInt()) // Green

        try {
            NotificationManagerCompat.from(context).notify(400, builder.build())
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
    }

    fun triggerBudgetCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val budgetRequest = OneTimeWorkRequestBuilder<BudgetWorker>().build()
        workManager.enqueue(budgetRequest)
    }
}
