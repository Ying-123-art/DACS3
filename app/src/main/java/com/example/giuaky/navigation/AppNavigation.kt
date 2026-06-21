package com.example.giuaky.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.giuaky.AdminHomescreen
import com.example.giuaky.CreateScreen
import com.example.giuaky.EditScreen
import com.example.giuaky.HomeScreen
import com.example.giuaky.LoginScreen
import com.example.giuaky.RegisterScreen
import com.example.giuaky.ui.screen.*
import com.example.giuaky.viewmodel.*
import java.net.URLDecoder

@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = NavRoutes.LOGIN) {

        composable(NavRoutes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { role ->
                    if (role == "admin") {
                        navController.navigate(NavRoutes.ADMIN) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) }
            )
        }

        composable(NavRoutes.REGISTER) {
            val authViewModel: AuthViewModel = viewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToCreate = { navController.navigate(NavRoutes.CREATE) },
                onNavigateToEdit = { postId -> navController.navigate(NavRoutes.editRoute(postId)) },
                onNavigateToComments = { postId -> navController.navigate(NavRoutes.commentsRoute(postId)) },
                onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) },
                onNavigateToOtherProfile = { userId -> navController.navigate(NavRoutes.profileRoute(userId)) },
                onNavigateToMap = { lat, lon ->
                    navController.navigate(NavRoutes.mapRoute(lat, lon))
                },
                onNavigateToNotifications = { navController.navigate(NavRoutes.NOTIFICATIONS) },
                onNavigateToChatList = { navController.navigate(NavRoutes.CHAT_LIST) }
            )
        }

        composable(NavRoutes.CREATE) {
            val postViewModel: PostViewModel = viewModel()
            CreateScreen(
                viewModel = postViewModel,
                onBackToHome = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.EDIT,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val postViewModel: PostViewModel = viewModel()
            EditScreen(
                postId = postId,
                viewModel = postViewModel,
                onBackToHome = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.COMMENTS,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val commentViewModel: CommentViewModel = viewModel()
            CommentScreen(
                postId = postId,
                viewModel = commentViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROFILE) {
            val profileViewModel: ProfileViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                userId = null,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                onNavigateToMap = { lat, lon ->
                    navController.navigate(NavRoutes.mapRoute(lat, lon))
                },
                onNavigateToOtherProfile = { userId ->
                    navController.navigate(NavRoutes.profileRoute(userId))
                },
                onNavigateToChat = { roomId: String, otherUserId: String, otherUserName: String ->
                    navController.navigate(NavRoutes.chatDetailRoute(roomId, otherUserId, otherUserName))
                },
                onShareClick = { originalPost, sharedText ->
                    profileViewModel.sharePost(originalPost, sharedText)
                }
            )
        }

        composable(
            route = NavRoutes.OTHERS_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                userId = userId,
                onLogout = {},
                onBack = { navController.popBackStack() },
                onNavigateToMap = { lat, lon ->
                    navController.navigate(NavRoutes.mapRoute(lat, lon))
                },
                onNavigateToOtherProfile = { otherId ->
                    navController.navigate(NavRoutes.profileRoute(otherId))
                },
                onNavigateToChat = { roomId: String, otherUserId: String, otherUserName: String ->
                    navController.navigate(NavRoutes.chatDetailRoute(roomId, otherUserId, otherUserName))
                },
                onShareClick = { originalPost, sharedText ->
                    profileViewModel.sharePost(originalPost, sharedText)
                }
            )
        }

        composable(
            route = NavRoutes.MAP,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; nullable = true },
                navArgument("lon") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()

            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))

            LaunchedEffect(lat, lon) {
                if (lat != null && lon != null) {
                    homeViewModel.setMapFocus(lat, lon)
                } else {
                    homeViewModel.clearMapFocus()
                }
            }

            MapScreen(
                viewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.NOTIFICATIONS) {
            val notificationViewModel: NotificationViewModel = viewModel()
            NotificationScreen(
                viewModel = notificationViewModel,
                onNavigateToPost = { postId ->
                    navController.navigate(NavRoutes.commentsRoute(postId))
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(NavRoutes.profileRoute(userId))
                },
                onBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(NavRoutes.HOME) },
                onNavigateToMap = { navController.navigate(NavRoutes.mapRoute()) },
                onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) }
            )
        }

        composable(NavRoutes.ADMIN) {
            val adminViewModel: AdminViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            AdminHomescreen(
                viewModel = adminViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.CHAT_LIST) {
            val chatViewModel: ChatViewModel = viewModel()
            ChatListScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToChat = { roomId: String, otherUserId: String, otherUserName: String ->
                    navController.navigate(NavRoutes.chatDetailRoute(roomId, otherUserId, otherUserName))
                }
            )
        }

        composable(
            route = NavRoutes.CHAT_DETAIL,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            val chatViewModel: ChatViewModel = viewModel()
            
            val decodedName = try {
                URLDecoder.decode(otherUserName, "UTF-8")
            } catch (e: Exception) {
                otherUserName
            }

            ChatDetailScreen(
                viewModel = chatViewModel,
                roomId = roomId,
                otherUserId = otherUserId,
                initialOtherUserName = decodedName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
