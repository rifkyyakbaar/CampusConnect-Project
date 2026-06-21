package com.campusconnect.app.model

data class Review(
    val reviewId: String,
    val ticketId: String,
    val eventId: String,
    val userId: String,
    val attendeeName: String,
    val rating: Int,
    val comment: String,
    val createdAt: String
)
