package com.example.borrowbay.features.createlisting.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: ListingRepository = ListingRepository()
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
            
            val item = Item(
                name = name,
                category = category,
                description = description,
                rentAmount = rentAmount,
                securityDeposit = securityDeposit,
                address = address,
                latitude = lat,
                longitude = lng,
                sellerId = currentUser.uid,
                sellerEmail = currentUser.email ?: ""
            )

            val result = repository.addItem(item, imageByteLists)
            
            if (result.isSuccess) {
                _listingState.value = ListingUiState.Success
            } else {
                _listingState.value = ListingUiState.Error(result.exceptionOrNull()?.message ?: "Failed to list item")
            }
        }
    }

    fun resetListingState() {
        _listingState.value = ListingUiState.Idle
    }
}
