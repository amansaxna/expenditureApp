package com.example.myexpenditureapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myexpenditureapp.data.Graph

class LiveStatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Graph.provide(applicationContext)
        LiveStatusNotificationManager.refresh(applicationContext)
        return Result.success()
    }
}
