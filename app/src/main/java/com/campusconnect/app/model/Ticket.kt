package com.campusconnect.app.model

data class Ticket(
    val ticketId: String,
    val userId: String,
    val eventId: String,
    val eventName: String,
    val category: String,
    val eventDate: String,
    val eventLocation: String,
    val attendeeName: String,
    val attendeeRole: String,
    val status: String,
    val paymentProofUrl: String = "",
    val createdAt: String = ""
)