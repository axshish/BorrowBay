package com.example.borrowbay.features.userregistration.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.data.model.User
import com.example.borrowbay.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class RegistrationStep(val stepNumber: Int) {
    PROFILE_PICTURE(1),
    NAME(2),
    EMAIL(3),
    PHONE(4),
    LOCATION(5),
    RAZORPAY(6)
}

data class UserRegistrationUiState(
    val currentStep: RegistrationStep = RegistrationStep.PROFILE_PICTURE,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val razorpayId: String = "",
    val avatarUri: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistrationSuccess: Boolean = false
)

class UserRegistrationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserRegistrationUiState())
    val uiState: StateFlow<UserRegistrationUiState> = _uiState.asStateFlow()
    
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    init {
        // Pre-fill email and phone if available from Firebase Auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _uiState.update { it.copy(
                email = currentUser.email ?: "",
                phone = currentUser.phoneNumber ?: "",
                name = currentUser.displayName ?: "",
                avatarUri = currentUser.photoUrl?.toString()
            ) }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun updateLocation(lat: Double, lng: Double, name: String, address: String) {
        _uiState.update { it.copy(
            latitude = lat,
            longitude = lng,
            locationName = name,
            address = address
        ) }
    }

    fun updateRazorpayId(id: String) {
        _uiState.update { it.copy(razorpayId = id) }
    }

    fun updateAvatarUri(uri: String?) {
        _uiState.update { it.copy(avatarUri = uri) }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        val next = RegistrationStep.entries.find { it.stepNumber == current.stepNumber + 1 }
        if (next != null) {
            _uiState.update { it.copy(currentStep = next) }
        } else {
            registerUser()
        }
    }

    fun previousStep(): Boolean {
        val current = _uiState.value.currentStep
        val prev = RegistrationStep.entries.find { it.stepNumber == current.stepNumber - 1 }
        return if (prev != null) {
            _uiState.update { it.copy(currentStep = prev) }
            true
        } else {
            false
        }
    }

    fun canGoNext(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            RegistrationStep.PROFILE_PICTURE -> true
            RegistrationStep.NAME -> state.name.isNotBlank()
            RegistrationStep.EMAIL -> isValidEmail(state.email)
            RegistrationStep.PHONE -> state.phone.isNotBlank()
            RegistrationStep.LOCATION -> state.locationName.isNotBlank() && state.latitude != null
            RegistrationStep.RAZORPAY -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun registerUser() {
        val currentUser = auth.currentUser ?: run {
            _uiState.update { it.copy(error = "No authenticated user found") }
            return
        }
        val state = _uiState.value
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                // 1. Generate/Set Avatar URL
                val avatarUrl = state.avatarUri ?: generateInitialsAvatar(state.name, state.email)
                
                // 2. Update Firebase Auth Profile (DisplayName and Photo)
                val profileUpdates = userProfileChangeRequest {
                    displayName = state.name
                    photoUri = android.net.Uri.parse(avatarUrl)
                }
                currentUser.updateProfile(profileUpdates).await()

                // 3. Create User Object for Firestore
                val user = User(
                    id = currentUser.uid,
                    name = state.name,
                    email = state.email,
                    phone = state.phone.ifBlank { null },
                    avatarUrl = avatarUrl,
                    address = state.address.ifBlank { null },
                    latitude = state.latitude,
                    longitude = state.longitude,
                    locationName = state.locationName,
                    razorpayId = state.razorpayId.ifBlank { null }
                )
                
                // 4. Save to Firestore
                val success = userRepository.createUser(user)
                if (success) {
                    _uiState.update { it.copy(isLoading = false, isRegistrationSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to create profile in database") }
                }
            } catch (e: Exception) {
                Log.e("UserRegistrationVM", "Registration error", e)
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "An error occurred") }
            }
        }
    }

    private fun generateInitialsAvatar(name: String, email: String): String {
        val initials = if (name.isNotBlank()) {
            name.split(" ").filter { it.isNotBlank() }.take(2).map { it[0].uppercaseChar() }.joinToString("")
        } else {
            email.take(1).uppercase()
        }
        return "https://ui-avatars.com/api/?name=$initials&background=0066FF&color=fff&size=256"
    }
}
