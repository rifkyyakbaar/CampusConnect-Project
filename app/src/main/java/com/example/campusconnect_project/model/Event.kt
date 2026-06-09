package com.example.campusconnect_project.model

import com.google.firebase.Timestamp

data class Event(
    var id: String = "",
    var eventName: String = "",
    var category: String = "",
    var description: String = "",
    var organizerId: String = "",
    var organizerName: String = "",
    var capacity: Int = 0,
    var registrants: Int = 0,
    var status: String = "pending",
    var createdAt: Timestamp? = null
)