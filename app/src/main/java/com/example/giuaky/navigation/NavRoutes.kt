package com.example.giuaky.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CREATE = "create"
    const val EDIT = "edit/{postId}"
    const val PROFILE = "profile"
    const val COMMENTS = "comments/{postId}"
    const val MAP = "map?lat={lat}&lon={lon}"
    const val NOTIFICATIONS = "notifications"
    const val ADMIN = "admin"

    fun editRoute(postId: String) = "edit/$postId"
    fun commentsRoute(postId: String) = "comments/$postId"
    fun mapRoute(lat: Double? = null, lon: Double? = null): String {
        return if (lat != null && lon != null) "map?lat=$lat&lon=$lon" else "map"
    }
}
