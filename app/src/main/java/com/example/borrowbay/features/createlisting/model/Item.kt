package com.example.borrowbay.features.createlisting.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String = "",
    val name: String = "",
    @SerialName("categoryId") val category: String = "",
    val description: String = "",
    @SerialName("pricePerDay") val rentAmount: Double = 0.0,
    val securityDeposit: Double = 0.0,
    val imageUrls: List<String> = emptyList(),
    @SerialName("location") val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("ownerId") val sellerId: String = "",
    val sellerEmail: String = "",
    val sellerPhone: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
