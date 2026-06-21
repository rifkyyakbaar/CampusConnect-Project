package com.campusconnect.app.model

data class Peserta(
    val ticketId: String,
    val attendeeName: String,
    val attendeeRole: String,
    val paymentProofUrl: String,
    val status: String
)