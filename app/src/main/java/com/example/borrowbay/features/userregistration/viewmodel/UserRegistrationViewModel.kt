package com.example.borrowbay.features.userregistration.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.core.supabase
import com.example.borrowbay.core.ui.components.Country
import com.example.borrowbay.core.ui.components.countries
import com.example.borrowbay.data.model.User
import com.example.borrowbay.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import io.github.jan.supabase.storage.storage
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
    val selectedCountry: Country = countries.first(),
    val address: String = "",
    val latitude: Double? = 19.1235, // Default: L&T Main Building, Mumbai
    val longitude: Double? = 73.0135,
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
            val firebasePhone = currentUser.phoneNumber ?: ""
            // Try to match country code from firebase phone
            val matchingCountry = countries.find { firebasePhone.startsWith(it.code) }
            val phoneWithoutCode = if (matchingCountry != null) {
                firebasePhone.removePrefix(matchingCountry.code)
            } else {
                firebasePhone
            }

            _uiState.update { it.copy(
                email = currentUser.email ?: "",
                phone = phoneWithoutCode,
                selectedCountry = matchingCountry ?: countries.first(),
                name = currentUser.displayName ?: "",
                avatarUri = currentUser.photoUrl?.toString()
            ) }
        }
    }

    fun updateName(name: String) {
        if (name.length <= 50) {
            _uiState.update { it.copy(name = name) }
        }
    }

    fun updateEmail(email: String) {
        if (email.length <= 100) {
            _uiState.update { it.copy(email = email) }
        }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun updateCountry(country: Country) {
        _uiState.update { it.copy(selectedCountry = country) }
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

    fun nextStep(context: Context) {
        val current = _uiState.value.currentStep
        val next = RegistrationStep.entries.find { it.stepNumber == current.stepNumber + 1 }
        if (next != null) {
            _uiState.update { it.copy(currentStep = next) }
        } else {
            registerUser(context)
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
            RegistrationStep.NAME -> state.name.isNotBlank() && state.name.length >= 2
            RegistrationStep.EMAIL -> isValidEmail(state.email)
            RegistrationStep.PHONE -> state.phone.length == state.selectedCountry.maxLength
            RegistrationStep.LOCATION -> state.locationName.isNotBlank() && state.latitude != null
            RegistrationStep.RAZORPAY -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun registerUser(context: Context) {
        val currentUser = auth.currentUser ?: run {
            _uiState.update { it.copy(error = "No authenticated user found") }
            return
        }
        val state = _uiState.value
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                // 1. Upload Avatar to Supabase if it's a local Uri
                var avatarUrl = state.avatarUri
                
                if (avatarUrl != null && (avatarUrl.startsWith("content://") || avatarUrl.startsWith("file://"))) {
                    avatarUrl = uploadAvatarToSupabase(context, Uri.parse(avatarUrl), currentUser.uid)
                }

                if (avatarUrl == null) {
                    avatarUrl = generateInitialsAvatar(state.name, state.email)
                }
                
                // 2. Update Firebase Auth Profile (DisplayName and Photo)
                val profileUpdates = userProfileChangeRequest {
                    displayName = state.name
                    photoUri = Uri.parse(avatarUrl)
                }
                currentUser.updateProfile(profileUpdates).await()

                // 3. Create User Object for Firestore
                val fullPhone = if (state.phone.isNotBlank()) state.selectedCountry.code + state.phone else null
                
                val user = User(
                    id = currentUser.uid,
                    name = state.name,
                    email = state.email,
                    phone = fullPhone,
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

    private suspend fun uploadAvatarToSupabase(context: Context, uri: Uri, userId: String): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val fileName = "avatars/$userId.jpg"
            val bucket = supabase.storage.from("item-images")
            
            // Upload the file
            bucket.upload(fileName, bytes) {
                upsert = true
            }
            
            // Get public URL
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            Log.e("UserRegistrationVM", "Supabase upload error", e)
            null
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
