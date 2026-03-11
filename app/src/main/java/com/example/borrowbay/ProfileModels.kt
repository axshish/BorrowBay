package com.example.borrowbay

data class UserProfile(
    val name: String,
    val phone: String,
    val email: String,
    val address: String
)

sealed class Screen {
    data object Profile : Screen()
    data object Details : Screen()
    data object ActiveListings : Screen()
    data object RentalHistory : Screen()
}
