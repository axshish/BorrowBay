package com.example.borrowbay.features.createlisting.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.data.model.Owner
import com.example.borrowbay.data.repository.UserRepository
import com.example.borrowbay.features.createlisting.data.ListingRepository
import com.example.borrowbay.features.createlisting.model.Item
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class ListingUiState {
    object Idle : ListingUiState()
    object Loading : ListingUiState()
    object Success : ListingUiState()
    object BankDetailsMissing : ListingUiState()
    data class Error(val msg: String) : ListingUiState()
}

class CreateListingViewModel(
    private val repository: ListingRepository = ListingRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    
    private val _listingState = mutableStateOf<ListingUiState>(ListingUiState.Idle)
    val listingState: State<ListingUiState> = _listingState

    private val auth = FirebaseAuth.getInstance()

    fun listProduct(
        name: String,
        category: String,
        description: String,
        rentAmount: Double,
        securityDeposit: Double,
        address: String,
        lat: Double,
        lng: Double,
        imageByteLists: List<ByteArray>
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _listingState.value = ListingUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _listingState.value = ListingUiState.Loading
            
            try {
                // Fetch the current user details to include in the product
                val userDetails = userRepository.getUser(currentUser.uid)
                val owner = if (userDetails != null) {
                    Owner(
                        id = userDetails.id,
                        name = userDetails.name,
                        avatarUrl = userDetails.avatarUrl,
                        phone = userDetails.phone ?: "",
                        email = userDetails.email
                    )
                } else {
                    Owner(id = currentUser.uid, name = "Unknown", phone = "", email = currentUser.email ?: "")
                }

                // Updated Item with Owner info for immediate display on Home screen
                val item = Item(
                    name = name,
                    categoryId = category,
                    description = description,
                    pricePerDay = rentAmount,
                    securityDeposit = securityDeposit,
                    location = address,
                    latitude = lat,
                    longitude = lng,
                    ownerId = currentUser.uid,
                    sellerEmail = currentUser.email ?: "",
                    owner = owner
                )

                val result = repository.addItem(item, imageByteLists)
                
                if (result.isSuccess) {
                    _listingState.value = ListingUiState.Success
                } else {
                    _listingState.value = ListingUiState.Error(result.exceptionOrNull()?.message ?: "Failed to list item")
                }
            } catch (e: Exception) {
                _listingState.value = ListingUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun resetListingState() {
        _listingState.value = ListingUiState.Idle
    }
}
