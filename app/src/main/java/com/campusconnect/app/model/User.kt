package com.campusconnect.app.model

data class User(
    val uid: String,
    val fullName: String,
    val email: String,
    val role: String,
    val profileImageUrl: String,
    val accountStatus: String,
    val createdAt: String
)