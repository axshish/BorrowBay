package com.example.borrowbay.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import com.example.borrowbay.features.profile.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
    private val _userProfile = MutableStateFlow(
        UserProfile(
            name = "Mourya",
            phone = "+91 87884838383",
            email = "mourya@example.com",
            address = "Navi Mumbai,Mumbai"
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _currentScreen = MutableStateFlow<ProfileScreenState>(ProfileScreenState.Profile)
    val currentScreen: StateFlow<ProfileScreenState> = _currentScreen.asStateFlow()

    fun updateProfile(updatedProfile: UserProfile) {
        _userProfile.value = updatedProfile
        navigateTo(ProfileScreenState.Profile)
    }

    fun navigateTo(screen: ProfileScreenState) {
        _currentScreen.value = screen
    }

    fun handleBackPress(): Boolean {
        return if (_currentScreen.value != ProfileScreenState.Profile) {
            _currentScreen.value = ProfileScreenState.Profile
            true
        } else {
            false
        }
    }
}

sealed class ProfileScreenState {
    data object Profile : ProfileScreenState()
    data object Details : ProfileScreenState()
    data object ActiveListings : ProfileScreenState()
    data object RentalHistory : ProfileScreenState()
}
