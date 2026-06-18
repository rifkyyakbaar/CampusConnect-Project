package com.campusconnect.app.model

data class Event(
    var id: String = "",
    var eventName: String = "",
    var category: String = "",
    var location: String = "",
    var description: String = "",
    var organizerId: String = "",
    var organizerName: String = "",
    var capacity: Int = 0,
    var registrants: Int = 0,
    var status: String = "pending",
    var posterUrl: String = "",
    var eventDate: String = "",
    var createdAt: String? = null,
    var eventPrice: Int = 0
)
