package com.example.giuaky.data.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val role: String = "user", // "user" or "admin"
    val createdAt: Long = 0L
)
