package com.example.borrowbay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RentalItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val pricePerDay: Double = 0.0,
    val securityDeposit: Double = 0.0,
    val distance: Double = 0.0,
    val rating: Double = 0.0,
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val isAvailable: Boolean = true,
    val ownerId: String = "",
    val sellerEmail: String = "",
    val sellerPhone: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val owner: Owner? = null,
    val rentedAt: Long? = null,
    val renterId: String? = null,
    val rentalDurationDays: Int? = null
)

@Serializable
data class Owner(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val phone: String = "",
    val email: String = "",
    val razorpayId: String? = null // Added to support payments
)

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val icon: String? = null
)
