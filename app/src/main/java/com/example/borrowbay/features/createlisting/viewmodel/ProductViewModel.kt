package com.example.borrowbay.features.createlisting.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.core.supabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ListingUiState {
    data object Idle : ListingUiState()
    data object Loading : ListingUiState()
    data object Success : ListingUiState()
    data object BankDetailsMissing : ListingUiState()
    data class Error(val msg: String) : ListingUiState()
}

class ProductViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _listingState = mutableStateOf<ListingUiState>(ListingUiState.Idle)
    val listingState: State<ListingUiState> = _listingState

    fun checkBankDetailsAndInitialize() {
        _listingState.value = ListingUiState.Idle
    }

    fun listProduct(
        name: String,
        category: String,
        description: String,
        rentAmount: Double,
        securityDeposit: Double,
        address: String,
        lat: Double?,
        lng: Double?,
        imageByteLists: List<ByteArray>
    ) {
        viewModelScope.launch {
            _listingState.value = ListingUiState.Loading
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _listingState.value = ListingUiState.Error("User not authenticated")
                    return@launch
                }

                val imageUrls = mutableListOf<String>()
                val bucket = supabase.storage.from("products")
                
                // Upload images to Supabase Storage
                imageByteLists.forEach { bytes ->
                    val fileName = "${UUID.randomUUID()}.jpg"
                    bucket.upload(fileName, bytes)
                    imageUrls.add(bucket.publicUrl(fileName))
                }

                // Save product details to Firebase Firestore
                val product = hashMapOf(
                    "name" to name,
                    "category" to category,
                    "description" to description,
                    "rentAmount" to rentAmount,
                    "securityDeposit" to securityDeposit,
                    "imageUrls" to imageUrls,
                    "address" to address,
                    "latitude" to lat,
                    "longitude" to lng,
                    "sellerId" to currentUser.uid,
                    "sellerEmail" to (currentUser.email ?: ""),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("products")
                    .add(product)
                
                _listingState.value = ListingUiState.Success

            } catch (e: Exception) {
                _listingState.value = ListingUiState.Error(e.message ?: "Failed to list product")
            }
        }
    }

    fun resetListingState() {
        _listingState.value = ListingUiState.Idle
    }
}
