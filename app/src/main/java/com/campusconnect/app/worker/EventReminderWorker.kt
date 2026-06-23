package com.campusconnect.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        // Keys untuk inputData (harus sama dengan ReminderScheduler)
        const val KEY_EVENT_NAME     = "event_name"
        const val KEY_USER_ID        = "user_id"
        const val KEY_REMINDER_TYPE  = "reminder_type"

        // Nilai tipe reminder (harus sama dengan ReminderScheduler)
        const val TYPE_H1_DAY = "EVENT_REMINDER_H1"
        const val TYPE_1H     = "EVENT_REMINDER_1H"
        const val TYPE_10M    = "EVENT_REMINDER_10M"

        // Untuk backward-compat dengan kode lama yang memakai EVENT_NAME_KEY
        const val EVENT_NAME_KEY = "event_name"

        const val NOTIFICATION_CHANNEL_ID = "event_reminder_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val eventName    = inputData.getString(KEY_EVENT_NAME) ?: "Event"
            val userId       = inputData.getString(KEY_USER_ID) ?: ""
            val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: TYPE_H1_DAY

            // 1. Tentukan teks berdasarkan tipe reminder
            val (title, message) = buildNotifContent(eventName, reminderType)

            // 2. Tampilkan push notification di sistem Android
            showPushNotification(title, message, reminderType)

            // 3. Simpan ke tabel notifications Supabase agar tampil di NotificationActivity
            if (userId.isNotBlank()) {
                SupabaseRepository.createNotification(
                    context  = applicationContext,
                    userId   = userId,
                    title    = title,
                    message  = message,
                    type     = reminderType
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // ─────────────────────────────────────────
    // Teks notifikasi per tipe reminder
    // ─────────────────────────────────────────

    private fun buildNotifContent(eventName: String, type: String): Pair<String, String> {
        return when (type) {
            TYPE_H1_DAY -> Pair(
                "Pengingat Event – Besok 📅",
                "Event \"$eventName\" akan dilaksanakan besok. Jangan lupa hadir tepat waktu!"
            )
            TYPE_1H -> Pair(
                "Pengingat Event – 1 Jam Lagi ⏰",
                "Event \"$eventName\" akan dimulai satu jam lagi. Persiapkan diri Anda."
            )
            TYPE_10M -> Pair(
                "Pengingat Event – 10 Menit Lagi 🚀",
                "Event \"$eventName\" akan dimulai dalam 10 menit. Segera menuju lokasi acara!"
            )
            else -> Pair(
                "Pengingat Event 🔔",
                "Event \"$eventName\" akan segera dimulai."
            )
        }
    }

    // ─────────────────────────────────────────
    // Push notification Android
    // ─────────────────────────────────────────

    private fun showPushNotification(title: String, message: String, type: String) {
        createNotificationChannelIfNeeded()

        // Gunakan notificationId berbeda per tipe agar tidak saling menimpa
        val notifId = when (type) {
            TYPE_H1_DAY -> 1001
            TYPE_1H     -> 1002
            TYPE_10M    -> 1003
            else        -> 1000
        }

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, notification)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Pengingat Event",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat sebelum event dimulai"
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}