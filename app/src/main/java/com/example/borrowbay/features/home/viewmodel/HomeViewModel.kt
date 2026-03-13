package com.example.borrowbay.features.home.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.data.repository.RentalRepository
import com.example.borrowbay.data.repository.UserRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class SortOption {
    DISTANCE, PRICE_LOW_HIGH, PRICE_HIGH_LOW, RATING
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val categories: List<Category> = emptyList(),
    val nearbyRentals: List<RentalItem> = emptyList(),
    val globalRentals: List<RentalItem> = emptyList(),
    val trendingRentals: List<RentalItem> = emptyList(),
    val selectedCategory: String? = null,
    val userAddress: String = "Detecting location...",
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val userRazorpayId: String? = null,
    val searchQuery: String = "",
    val selectedSort: SortOption = SortOption.DISTANCE,
    val isLocationPickerVisible: Boolean = false,
    val isRazorpaySetupVisible: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreGlobal: Boolean = true
)

class HomeViewModel(
    private val rentalRepository: RentalRepository = RentalRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private var lastVisibleGlobalDoc: Any? = null
    
    private var nearbyJob: Job? = null
    private var categoriesJob: Job? = null

    init {
        loadCategories()
        fetchUserData()
    }

    private fun loadCategories() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            rentalRepository.getCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val user = userRepository.getUser(uid)
                if (user != null) {
                    _uiState.update { it.copy(
                        userAddress = user.address ?: user.locationName ?: "Unknown Location",
                        userLatitude = user.latitude,
                        userLongitude = user.longitude,
                        userRazorpayId = user.razorpayId
                    ) }
                    // After we have user location, refresh items
                    refreshNearbyAndGlobal()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching user data", e)
            }
        }
    }

    fun updateLocation(lat: Double, lng: Double, address: String) {
        _uiState.update { it.copy(
            userLatitude = lat,
            userLongitude = lng,
            userAddress = address,
            isLocationPickerVisible = false
        ) }
        refreshNearbyAndGlobal()
    }

    @SuppressLint("MissingPermission")
    fun detectCurrentLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    viewModelScope.launch {
                        val address = getAddressFromCoords(context, it.latitude, it.longitude)
                        updateLocation(it.latitude, it.longitude, address)
                    }
                }
            }
    }

    private suspend fun getAddressFromCoords(context: Context, lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown Address"
            } catch (e: Exception) {
                "Unknown Address"
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refreshNearbyAndGlobal()
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategory = if (_uiState.value.selectedCategory == categoryId) null else categoryId) }
        refreshNearbyAndGlobal()
    }

    fun onSortOptionSelected(sortOption: SortOption) {
        _uiState.update { it.copy(selectedSort = sortOption) }
        refreshNearbyAndGlobal()
    }

    fun toggleLocationPicker(show: Boolean) {
        _uiState.update { it.copy(isLocationPickerVisible = show) }
    }

    fun showRazorpaySetup(show: Boolean) {
        _uiState.update { it.copy(isRazorpaySetupVisible = show) }
    }

    fun saveRazorpayId(id: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val success = userRepository.updateRazorpayId(uid, id)
            if (success) {
                _uiState.update { it.copy(userRazorpayId = id, isRazorpaySetupVisible = false) }
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            refreshNearbyAndGlobal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun refreshNearbyAndGlobal() {
        // Start nearby collection in a separate job so it doesn't block global load
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            val state = _uiState.value
            rentalRepository.getNearbyRentals(
                lat = state.userLatitude,
                lng = state.userLongitude,
                category = state.selectedCategory,
                query = state.searchQuery,
                sort = state.selectedSort
            ).collect { nearby ->
                _uiState.update { it.copy(nearbyRentals = nearby, isLoading = false) }
            }
        }

        // Reset and reload global list
        lastVisibleGlobalDoc = null
        _uiState.update { it.copy(hasMoreGlobal = true, globalRentals = emptyList()) }
        loadMoreGlobal()
    }

    fun loadMoreGlobal() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreGlobal) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            val result = rentalRepository.getGlobalRentals(
                lat = _uiState.value.userLatitude,
                lng = _uiState.value.userLongitude,
                category = _uiState.value.selectedCategory,
                query = _uiState.value.searchQuery,
                sort = _uiState.value.selectedSort,
                limit = 10,
                lastDoc = lastVisibleGlobalDoc
            )

            _uiState.update { it.copy(
                globalRentals = if (lastVisibleGlobalDoc == null) result.items else it.globalRentals + result.items,
                hasMoreGlobal = result.hasMore,
                isLoadingMore = false
            ) }
            lastVisibleGlobalDoc = result.lastDoc
        }
    }
}
