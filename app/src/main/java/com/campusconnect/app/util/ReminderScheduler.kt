package com.campusconnect.app.util

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.campusconnect.app.worker.EventReminderWorker
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    /**
     * Schedule H-1 (one day before) event reminder notification
     *
     * @param context Application context
     * @param eventName Name of the event
     * @param eventDateTimestamp Event date timestamp in milliseconds
     */
    fun scheduleH1Reminder(
        context: Context,
        eventName: String,
        eventDateTimestamp: Long
    ) {
        // Calculate H-1 timestamp (24 hours before event)
        val h1Timestamp = eventDateTimestamp - (24 * 60 * 60 * 1000) // 24 hours in milliseconds

        // Calculate delay from now to H-1
        val currentTime = System.currentTimeMillis()
        val delayInMillis = h1Timestamp - currentTime

        // If delay is negative or too small, don't schedule
        if (delayInMillis <= 0) {
            return
        }

        // Convert milliseconds to minutes for WorkManager
        val delayInMinutes = TimeUnit.MILLISECONDS.toMinutes(delayInMillis)

        // Prepare input data for worker
        val inputData = Data.Builder()
            .putString(EventReminderWorker.EVENT_NAME_KEY, eventName)
            .build()

        // Create one-time work request
        val reminderWorkRequest = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        // Enqueue the work
        WorkManager.getInstance(context).enqueueUniqueWork(
            "event_reminder_$eventName",
            androidx.work.ExistingWorkPolicy.REPLACE,
            reminderWorkRequest
        )
    }
}

