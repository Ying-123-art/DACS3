package com.example.giuaky.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CREATE = "create"
    const val EDIT = "edit/{postId}"
    const val PROFILE = "profile"
    const val COMMENTS = "comments/{postId}"
    const val MAP = "map"
    const val ADMIN = "admin"

    fun editRoute(postId: String) = "edit/$postId"
    fun commentsRoute(postId: String) = "comments/$postId"
}
