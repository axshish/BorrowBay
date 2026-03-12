package com.example.borrowbay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val avatarUrl: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val razorpayId: String? = null, // Razorpay Account ID for receiving payments
    val itemsRented: List<String> = emptyList(),
    val itemsListed: List<String> = emptyList()
)
