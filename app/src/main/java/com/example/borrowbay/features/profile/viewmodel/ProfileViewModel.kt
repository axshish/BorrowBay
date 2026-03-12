package com.example.borrowbay.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.features.profile.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            name = "",
            phone = "",
            email = "",
            address = ""
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _currentScreen = MutableStateFlow<ProfileScreenState>(ProfileScreenState.Profile)
    val currentScreen: StateFlow<ProfileScreenState> = _currentScreen.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val doc = firestore.collection("users").document(currentUser.uid).get().await()
                    if (doc.exists()) {
                        _userProfile.value = UserProfile(
                            name = doc.getString("name") ?: currentUser.displayName ?: "",
                            phone = doc.getString("phone") ?: currentUser.phoneNumber ?: "",
                            email = doc.getString("email") ?: currentUser.email ?: "",
                            address = doc.getString("address") ?: ""
                        )
                    } else {
                        // Use Firebase Auth info if doc doesn't exist yet
                        _userProfile.value = UserProfile(
                            name = currentUser.displayName ?: "",
                            phone = currentUser.phoneNumber ?: "",
                            email = currentUser.email ?: "",
                            address = ""
                        )
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    fun updateProfile(updatedProfile: UserProfile) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    firestore.collection("users").document(currentUser.uid)
                        .set(updatedProfile)
                        .await()
                    _userProfile.value = updatedProfile
                    navigateTo(ProfileScreenState.Profile)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
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
    
    fun signOut() {
        auth.signOut()
    }
}

sealed class ProfileScreenState {
    data object Profile : ProfileScreenState()
    data object Details : ProfileScreenState()
    data object ActiveListings : ProfileScreenState()
    data object RentalHistory : ProfileScreenState()
}
