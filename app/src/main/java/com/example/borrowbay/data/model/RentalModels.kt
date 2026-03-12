package com.example.borrowbay.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RentalItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val pricePerDay: Double,
    val distance: Double = 0.0,
    val rating: Double = 0.0,
    val location: String,
    val imageUrls: List<String> = emptyList(), // List of images, 0th is the main image
    val isAvailable: Boolean = true,
    val ownerId: String,
    val categoryId: String,
    val owner: Owner? = null
)

@Serializable
data class Owner(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val phone: String,
    val email: String
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val icon: String? = null // Emoji or icon name
)
