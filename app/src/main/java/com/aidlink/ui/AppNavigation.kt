package com.aidlink.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aidlink.ui.auth.LoginScreen
import com.aidlink.ui.auth.OtpVerificationScreen
import com.aidlink.ui.auth.ProfileSetupScreen
import com.aidlink.ui.home.HomeScreen
import com.aidlink.viewmodel.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
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

        composable("home") {
            HomeScreen()
        }
    }
}