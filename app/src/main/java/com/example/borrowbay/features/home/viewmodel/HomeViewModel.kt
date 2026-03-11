package com.example.borrowbay.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.data.repository.RentalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val nearbyRentals: List<RentalItem> = emptyList(),
    val trendingRentals: List<RentalItem> = emptyList(),
    val selectedCategory: String = "1",
    val userLocation: String = "San Francisco, CA"
)

class HomeViewModel(private val repository: RentalRepository = RentalRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            combine(
                repository.getCategories(),
                repository.getNearbyRentals(),
                repository.getTrendingRentals()
            ) { categories, nearby, trending ->
                HomeUiState(
                    isLoading = false,
                    categories = categories,
                    nearbyRentals = nearby,
                    trendingRentals = trending,
                    selectedCategory = categories.firstOrNull()?.id ?: "1"
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = categoryId)
    }
}
