package com.example.borrowbay.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.borrowbay.core.supabase
import com.example.borrowbay.features.auth.ui.LoginScreen
import com.example.borrowbay.features.home.ui.HomeScreen
import com.example.borrowbay.features.onboarding.ui.OnboardingScreen
import com.example.borrowbay.features.profile.ui.ProfileApp
import io.github.jan.supabase.auth.auth

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    
    // Check for existing session
    val initialDestination = remember {
        if (supabase.auth.currentSessionOrNull() != null) "home" else "onboarding"
    }

    NavHost(navController = navController, startDestination = initialDestination) {
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
                }
            )
        }
        composable("home") {
            HomeScreen(
                onProfileClick = {
                    navController.navigate("profile")
                }
            )
        }
        composable("profile") {
            ProfileApp()
        }
    }
}
