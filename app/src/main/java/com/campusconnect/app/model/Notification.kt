package com.campusconnect.app.model

data class Notification(
    val notificationId: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,       // EVENT_REMINDER_H1 | EVENT_REMINDER_1H | EVENT_REMINDER_10M
    // | PAYMENT_APPROVED | PAYMENT_REJECTED
    // | EVENT_APPROVED | EVENT_REJECTED
    val isRead: Boolean,
    val createdAt: String
)