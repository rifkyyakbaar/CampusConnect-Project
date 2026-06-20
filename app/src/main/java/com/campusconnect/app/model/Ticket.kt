package com.campusconnect.app.model

data class Ticket(
    val ticketId: String,
    val eventId: String,
    val eventName: String,
    val category: String,
    val eventDate: String,
    val eventLocation: String,
    val status: String
)