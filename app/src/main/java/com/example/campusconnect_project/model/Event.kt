package com.example.campusconnect_project.model

data class Event(
    val eventName: String = "",
    val category: String = "",
    val capacity: Int = 0,
    val description: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val registrants: Int = 0
)