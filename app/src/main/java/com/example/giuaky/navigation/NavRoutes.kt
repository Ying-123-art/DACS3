package com.example.giuaky.navigation

import java.net.URLEncoder

object NavRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CREATE = "create"
    const val EDIT = "edit/{postId}"
    const val PROFILE = "profile"
    const val OTHERS_PROFILE = "profile/{userId}"
    const val COMMENTS = "comments/{postId}"
    const val MAP = "map?lat={lat}&lon={lon}"
    const val NOTIFICATIONS = "notifications"
    const val ADMIN = "admin"
    const val CHAT_LIST = "chat_list"
    const val CHAT_DETAIL = "chat_detail/{roomId}/{otherUserId}/{otherUserName}"

    fun editRoute(postId: String) = "edit/$postId"
    fun commentsRoute(postId: String) = "comments/$postId"
    fun profileRoute(userId: String) = "profile/$userId"
    
    fun chatDetailRoute(roomId: String, otherUserId: String, otherUserName: String): String {
        val encodedName = URLEncoder.encode(otherUserName, "UTF-8")
        return "chat_detail/$roomId/$otherUserId/$encodedName"
    }

    fun mapRoute(lat: Double? = null, lon: Double? = null): String {
        return if (lat != null && lon != null) "map?lat=$lat&lon=$lon" else "map"
    }
}
