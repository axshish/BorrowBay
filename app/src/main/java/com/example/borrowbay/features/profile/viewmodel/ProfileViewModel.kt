package com.example.borrowbay.features.profile.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.core.supabase
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.data.repository.RentalRepository
import com.example.borrowbay.data.repository.UserRepository
import com.example.borrowbay.features.profile.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading.asStateFlow()

    // Keep track of the latest successful cloud URL separately
    private var lastKnownCloudAvatarUrl: String? = null

    val userListings: StateFlow<List<RentalItem>> = _currentScreen.flatMapLatest { screen ->
        if (screen == ProfileScreenState.ActiveListings) {
            val uid = auth.currentUser?.uid
            if (uid != null) rentalRepository.getUserListings(uid) else flowOf(emptyList())
        } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRentals: StateFlow<List<RentalItem>> = _currentScreen.flatMapLatest { screen ->
        if (screen == ProfileScreenState.RentalHistory) {
            val uid = auth.currentUser?.uid
            if (uid != null) rentalRepository.getUserRentals(uid) else flowOf(emptyList())
        } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val user = userRepository.getUser(currentUser.uid)
                    if (user != null) {
                        lastKnownCloudAvatarUrl = user.avatarUrl ?: currentUser.photoUrl?.toString()
                        _userProfile.value = UserProfile(
                            name = if (user.name.isNotBlank()) user.name else (currentUser.displayName ?: ""),
                            phone = user.phone ?: (currentUser.phoneNumber ?: ""),
                            email = if (user.email.isNotBlank()) user.email else (currentUser.email ?: ""),
                            address = user.address ?: "",
                            razorpayId = user.razorpayId ?: "",
                            avatarUri = lastKnownCloudAvatarUrl ?: ""
                        )
                    } else {
                        lastKnownCloudAvatarUrl = currentUser.photoUrl?.toString()
                        _userProfile.value = UserProfile(
                            name = currentUser.displayName ?: "",
                            phone = currentUser.phoneNumber ?: "",
                            email = currentUser.email ?: "",
                            address = "",
                            avatarUri = lastKnownCloudAvatarUrl ?: ""
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateProfile(updatedProfile: UserProfile) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    
                    // Use the latest cloud URL for DB, but keep local URI in the state if one exists
                    val dbAvatarUrl = if (updatedProfile.avatarUri.startsWith("content://")) {
                        lastKnownCloudAvatarUrl ?: ""
                    } else {
                        updatedProfile.avatarUri
                    }

                    val data = mapOf(
                        "name" to updatedProfile.name,
                        "phone" to updatedProfile.phone,
                        "email" to updatedProfile.email,
                        "address" to updatedProfile.address,
                        "razorpayId" to updatedProfile.razorpayId,
                        "avatarUrl" to dbAvatarUrl
                    )
                    
                    firestore.collection("users").document(currentUser.uid)
                        .set(data, com.google.firebase.firestore.SetOptions.merge())
                    
                    // Update state but preserve the local URI if an upload is in progress
                    _userProfile.value = updatedProfile
                    _isLoading.value = false
                    navigateTo(ProfileScreenState.Profile)
                } catch (e: Exception) {
                    _isLoading.value = false
                    e.printStackTrace()
                }
            }
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val currentUser = auth.currentUser ?: return
        
        // Local-first: Show selected image immediately
        _userProfile.update { it.copy(avatarUri = uri.toString()) }
        
        viewModelScope.launch {
            try {
                _isImageUploading.value = true
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val fileName = "avatars/${currentUser.uid}.jpg"
                val bucket = supabase.storage.from("item-images")
                
                bucket.upload(fileName, bytes) {
                    upsert = true
                }
                
                // Add timestamp to URL to bust Coil cache
                val freshAvatarUrl = "${bucket.publicUrl(fileName)}?t=${System.currentTimeMillis()}"
                lastKnownCloudAvatarUrl = freshAvatarUrl
                
                // Update Firestore with the new permanent URL
                firestore.collection("users").document(currentUser.uid)
                    .update("avatarUrl", freshAvatarUrl)
                
                // Final update to state with the cloud URL
                _userProfile.update { it.copy(avatarUri = freshAvatarUrl) }
                _isImageUploading.value = false
            } catch (e: Exception) {
                _isImageUploading.value = false
                Log.e("ProfileViewModel", "Avatar upload error", e)
            }
        }
    }

    fun markAsReturned(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = rentalRepository.markItemAsReturned(itemId)
            if (success) {
                // The flow will automatically update because of SnapshotListener in Repository
                _isLoading.value = false
            } else {
                _isLoading.value = false
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
    data object PaymentSetup : ProfileScreenState()
}
