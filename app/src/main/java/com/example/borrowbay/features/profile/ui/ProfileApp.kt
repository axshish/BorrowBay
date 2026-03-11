package com.example.borrowbay.features.profile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.borrowbay.features.profile.viewmodel.ProfileViewModel
import com.example.borrowbay.features.profile.viewmodel.ProfileScreenState

@Composable
fun ProfileApp(viewModel: ProfileViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    BackHandler(enabled = currentScreen != ProfileScreenState.Profile) {
        viewModel.handleBackPress()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (currentScreen) {
            ProfileScreenState.Profile -> {
                ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                    profile = userProfile,
                    onProfileClick = { viewModel.navigateTo(ProfileScreenState.Details) },
                    onActiveListingsClick = { viewModel.navigateTo(ProfileScreenState.ActiveListings) },
                    onRentalHistoryClick = { viewModel.navigateTo(ProfileScreenState.RentalHistory) }
                )
            }
            ProfileScreenState.Details -> {
                DetailsScreen(
                    modifier = Modifier.padding(innerPadding),
                    profile = userProfile,
                    onSave = { updatedProfile ->
                        viewModel.updateProfile(updatedProfile)
                    },
                    onBack = { viewModel.navigateTo(ProfileScreenState.Profile) }
                )
            }
            ProfileScreenState.ActiveListings -> {
                ActiveListingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { viewModel.navigateTo(ProfileScreenState.Profile) }
                )
            }
            ProfileScreenState.RentalHistory -> {
                RentalHistoryScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { viewModel.navigateTo(ProfileScreenState.Profile) }
                )
            }
        }
    }
}
