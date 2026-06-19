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
import com.example.giuaky.ui.screen.CommentScreen
import com.example.giuaky.ui.screen.MapScreen
import com.example.giuaky.ui.screen.NotificationScreen
import com.example.giuaky.ui.screen.ProfileScreen
import com.example.giuaky.viewmodel.AdminViewModel
import com.example.giuaky.viewmodel.AuthViewModel
import com.example.giuaky.viewmodel.CommentViewModel
import com.example.giuaky.viewmodel.HomeViewModel
import com.example.giuaky.viewmodel.HomeViewModelFactory
import com.example.giuaky.viewmodel.NotificationViewModel
import com.example.giuaky.viewmodel.PostViewModel
import com.example.giuaky.viewmodel.ProfileViewModel

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
                onNavigateToMap = { lat, lon ->
                    navController.navigate(NavRoutes.mapRoute(lat, lon))
                },
                onNavigateToNotifications = { navController.navigate(NavRoutes.NOTIFICATIONS) }
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
    }
}
