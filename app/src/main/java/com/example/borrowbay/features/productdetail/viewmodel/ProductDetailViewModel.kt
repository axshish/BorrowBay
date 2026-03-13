package com.example.borrowbay.features.productdetail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.data.repository.RentalRepository
import com.example.borrowbay.data.repository.UserRepository
import com.example.borrowbay.util.LocationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProductDetailUiState(
    val item: RentalItem? = null,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val isLoading: Boolean = false,
    val isRenting: Boolean = false,
    val rentSuccess: Boolean = false,
    val error: String? = null,
    val rentalDays: Int = 1,
    val platformFeeRate: Double = 0.05,
    val calculatedDistance: Double = 0.0
) {
    val subtotalPrice: Double get() = (item?.pricePerDay ?: 0.0) * rentalDays
    val platformFee: Double get() = subtotalPrice * platformFeeRate
    val securityDeposit: Double get() = item?.securityDeposit ?: 0.0
    val totalPrice: Double get() = subtotalPrice + platformFee + securityDeposit
}

class ProductDetailViewModel(
    private val productId: String,
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Fetch User's Persisted Location
                val uid = auth.currentUser?.uid
                var userLat: Double? = null
                var userLng: Double? = null
                if (uid != null) {
                    val user = userRepository.getUser(uid)
                    userLat = user?.latitude
                    userLng = user?.longitude
                }

                // 2. Fetch Product Details
                val doc = firestore.collection("products").document(productId).get().await()
                val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)

                // 3. Calculate Real Distance
                val distance = if (userLat != null && userLng != null && item?.latitude != null && item.longitude != null) {
                    LocationUtils.calculateDistance(userLat, userLng, item.latitude, item.longitude)
                } else {
                    item?.distance ?: 0.0
                }

                _uiState.update { it.copy(
                    item = item,
                    userLatitude = userLat,
                    userLongitude = userLng,
                    calculatedDistance = distance,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateRentalDays(days: Int) {
        if (days >= 1) {
            _uiState.update { it.copy(rentalDays = days) }
        }
    }

    fun rentProduct(onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val days = _uiState.value.rentalDays
        viewModelScope.launch {
            _uiState.update { it.copy(isRenting = true) }
            val success = rentalRepository.rentItem(productId, uid, days)
            if (success) {
                _uiState.update { it.copy(isRenting = false, rentSuccess = true) }
                onSuccess()
            } else {
                _uiState.update { it.copy(isRenting = false, error = "Failed to process rental. Please contact support.") }
            }
        }
    }
}
