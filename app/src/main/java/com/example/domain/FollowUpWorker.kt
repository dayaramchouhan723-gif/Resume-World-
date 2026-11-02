package com.example.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import java.util.concurrent.TimeUnit

class FollowUpWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val dao = database.appDao()

        val appliedJobs = dao.getAllAppliedJobsSync()
        val nowMs = System.currentTimeMillis()
        val fourWeeksMs = 28L * 24 * 60 * 60 * 1000

        var followUpCount = 0

        for (job in appliedJobs) {
            val appliedDateMs = job.dateApplied
            if (nowMs - appliedDateMs >= fourWeeksMs && job.status == "Applied") {
                followUpCount++
            }
        }

        if (followUpCount > 0) {
            sendNotification(followUpCount)
        }

        return Result.success()
    }

    private fun sendNotification(count: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "sanam_followup_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SANAM Job Follow-Up Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SANAM Follow-Up Reminder")
            .setContentText("Boss! $count applied jobs are 4 weeks old. Tap to send auto follow-up email!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}

object TriggerManager {
    fun scheduleDailyFollowUpCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<FollowUpWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sanam_daily_followup_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
