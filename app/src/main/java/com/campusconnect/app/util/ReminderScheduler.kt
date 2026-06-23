package com.campusconnect.app.util

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.campusconnect.app.worker.EventReminderWorker
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    /**
     * Menjadwalkan tiga pengingat sekaligus:
     *   H-1 hari, H-1 jam, H-10 menit
     *
     * Dipanggil dari CheckoutActivity (event gratis) dan
     * PaymentConfirmationActivity (event berbayar, tiket masih PENDING).
     */
    fun scheduleAllReminders(
        context: Context,
        userId: String,
        eventId: String,
        eventName: String,
        eventDateTimestamp: Long
    ) {
        val now = System.currentTimeMillis()

        scheduleOne(
            context   = context,
            workName  = "reminder_H1_$eventId",
            delayMs   = eventDateTimestamp - TimeUnit.HOURS.toMillis(24) - now,
            userId    = userId,
            eventName = eventName,
            type      = EventReminderWorker.TYPE_H1_DAY
        )

        scheduleOne(
            context   = context,
            workName  = "reminder_1H_$eventId",
            delayMs   = eventDateTimestamp - TimeUnit.HOURS.toMillis(1) - now,
            userId    = userId,
            eventName = eventName,
            type      = EventReminderWorker.TYPE_1H
        )

        scheduleOne(
            context   = context,
            workName  = "reminder_10M_$eventId",
            delayMs   = eventDateTimestamp - TimeUnit.MINUTES.toMillis(10) - now,
            userId    = userId,
            eventName = eventName,
            type      = EventReminderWorker.TYPE_10M
        )
    }

    // ---------- internal ----------

    private fun scheduleOne(
        context: Context,
        workName: String,
        delayMs: Long,
        userId: String,
        eventName: String,
        type: String
    ) {
        // Lewati jika waktunya sudah lampau
        if (delayMs <= 0) return

        val inputData = Data.Builder()
            .putString(EventReminderWorker.KEY_EVENT_NAME,    eventName)
            .putString(EventReminderWorker.KEY_USER_ID,       userId)
            .putString(EventReminderWorker.KEY_REMINDER_TYPE, type)
            .build()

        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}