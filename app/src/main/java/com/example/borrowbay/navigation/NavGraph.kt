package com.example.borrowbay.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.borrowbay.features.auth.ui.LoginScreen
import com.example.borrowbay.features.createlisting.ui.AddProductScreen
import com.example.borrowbay.features.home.ui.HomeScreen
import com.example.borrowbay.features.onboarding.ui.OnboardingScreen
import com.example.borrowbay.features.profile.ui.ProfileApp
import com.example.borrowbay.features.userregistration.ui.UserRegistrationScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    
    // Check current auth status on start
    val startDestination = if (auth.currentUser != null) "home" else "onboarding"

    NavHost(
        navController = navController, 
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNeedsRegistration = {
                    navController.navigate("registration") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("registration") {
            UserRegistrationScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = {
                    navController.navigate("home") {
                        popUpTo("registration") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onProfileClick = {
                    navController.navigate("profile")
                },
                onAddClick = {
                    navController.navigate("add_product")
                }
            )
        }
        composable("add_product") {
            AddProductScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSuccess = {
                    navController.popBackStack()
                }
            )
        }
        composable("profile") {
            ProfileApp(
                onSignOut = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
