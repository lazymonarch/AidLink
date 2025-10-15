package com.aidlink.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aidlink.ui.auth.LoginScreen
import com.aidlink.ui.auth.OtpVerificationScreen
import com.aidlink.ui.auth.ProfileSetupScreen
import com.aidlink.ui.home.*
import com.aidlink.ui.profile.EditProfileScreen
import com.aidlink.viewmodel.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val appNavViewModel: AppNavViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val myActivityViewModel: MyActivityViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()

    val currentUser by appNavViewModel.repository.getAuthStateFlow().collectAsState(initial = Firebase.auth.currentUser)

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val bottomBarRoutes = setOf("home", "activity", "chats", "profile")
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val shouldShowBottomBar = currentRoute in bottomBarRoutes && currentUser != null

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (Firebase.auth.currentUser != null) "home" else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- Authentication Flow ---
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToOtp = { verificationId ->
                        navController.navigate("otp_verification/$verificationId")
                    }
                )
            }
            composable(
                route = "otp_verification/{verificationId}",
                arguments = listOf(navArgument("verificationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val verificationId = backStackEntry.arguments?.getString("verificationId")
                if (verificationId != null) {
                    OtpVerificationScreen(
                        authViewModel = authViewModel,
                        verificationId = verificationId,
                        onBackClicked = { navController.popBackStack() },
                        onNavigateToHome = {
                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                        },
                        onNavigateToProfileSetup = {
                            navController.navigate("profile_setup") { popUpTo("login") { inclusive = true } }
                        }
                    )
                }
            }
            composable("profile_setup") {
                ProfileSetupScreen(
                    authViewModel = authViewModel,
                    onProfileSetupComplete = {
                        navController.navigate("home") { popUpTo("profile_setup") { inclusive = true } }
                    }
                )
            }

            // --- Main App Flow ---
            composable("home") {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onPostRequestClicked = { navController.navigate("post_request") },
                    onRequestClicked = { requestId ->
                        navController.navigate("request_detail/$requestId")
                    }
                )
            }

            composable("activity") {
                MyActivityScreen(
                    myActivityViewModel = myActivityViewModel,
                    onNavigateToChat = { chatId, otherUserName ->
                        navController.navigate("chat/$chatId/$otherUserName")
                    },
                    navController = navController
                )
            }

            composable("chats") {
                ChatsListScreen(
                    chatViewModel = chatViewModel,
                    onChatClicked = { chatId, userName ->
                        navController.navigate("chat/$chatId/$userName")
                    }
                )
            }

            // ✅ THIS IS THE SINGLE, CORRECTED PROFILE ROUTE
            composable("profile") {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    onNavigateToEdit = { navController.navigate("edit_profile") }
                )
            }

            composable("post_request") {
                PostRequestScreen(
                    homeViewModel = homeViewModel,
                    onClose = { navController.popBackStack() },
                    onPostRequestSuccess = { navController.popBackStack() }
                )
            }

            composable(
                route = "request_detail/{requestId}",
                arguments = listOf(navArgument("requestId") { type = NavType.StringType })
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId")
                if (requestId != null) {
                    homeViewModel.getRequestById(requestId)
                    RequestDetailScreen(
                        homeViewModel = homeViewModel,
                        onBackClicked = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = "edit_request/{requestId}",
                arguments = listOf(navArgument("requestId") { type = NavType.StringType })
            ) {
                EditRequestScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("edit_profile") {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "chat/{chatId}/{userName}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val userName = backStackEntry.arguments?.getString("userName") ?: "Chat"
                ChatScreen(
                    otherUserName = userName,
                    chatViewModel = chatViewModel,
                    onBackClicked = {
                        chatViewModel.clearChatScreenState()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}