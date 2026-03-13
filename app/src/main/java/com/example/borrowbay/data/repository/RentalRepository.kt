package com.example.borrowbay.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.data.model.Owner
import com.example.borrowbay.features.home.viewmodel.SortOption
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.*

data class PaginatedResult<T>(
    val items: List<T>,
    val lastDoc: DocumentSnapshot?,
    val hasMore: Boolean
)

class RentalRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Standard categories for the app
    val staticCategories = listOf(
        Category("Electronics", "Electronics", "📷"),
        Category("Sports", "Sports", "🚲"),
        Category("Tools", "Tools", "🔧"),
        Category("Outdoors", "Outdoors", "⛺"),
        Category("Vehicles", "Vehicles", "🚗"),
        Category("Music", "Music", "🎸"),
        Category("Gaming", "Gaming", "🎮")
    )

    fun getCategories(): Flow<List<Category>> = callbackFlow {
        // We use hardcoded categories as requested, but keep the flow for consistency
        trySend(staticCategories)
        awaitClose { }
    }

    fun getNearbyRentals(
        lat: Double?,
        lng: Double?,
        category: String?,
        query: String?,
        sort: SortOption
    ): Flow<List<RentalItem>> = callbackFlow {
        var baseQuery: Query = firestore.collection("products")
        
        // Filter by category if one is selected (and it's not "All")
        if (category != null && category != "All") {
            baseQuery = baseQuery.whereEqualTo("categoryId", category)
        }

        val listener = baseQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            var items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            // Filter by search query
            if (!query.isNullOrBlank()) {
                items = items.filter { it.name.contains(query, ignoreCase = true) }
            }

            // Real distance calculation and 10km filtering
            val nearbyItems = items.map { item ->
                val distance = if (lat != null && lng != null && item.latitude != null && item.longitude != null) {
                    calculateDistance(lat, lng, item.latitude, item.longitude)
                } else 100.0
                item.copy(distance = distance)
            }.filter { it.distance <= 10.0 }

            val sortedItems = when (sort) {
                SortOption.DISTANCE -> nearbyItems.sortedBy { it.distance }
                SortOption.PRICE_LOW_HIGH -> nearbyItems.sortedBy { it.pricePerDay }
                SortOption.PRICE_HIGH_LOW -> nearbyItems.sortedByDescending { it.pricePerDay }
                else -> nearbyItems
            }

            trySend(sortedItems)
        }
        awaitClose { listener.remove() }
    }

    suspend fun getGlobalRentals(
        lat: Double?,
        lng: Double?,
        category: String?,
        query: String?,
        sort: SortOption,
        limit: Long,
        lastDoc: Any?
    ): PaginatedResult<RentalItem> {
        return try {
            var baseQuery: Query = firestore.collection("products")
            
            if (category != null && category != "All") {
                baseQuery = baseQuery.whereEqualTo("categoryId", category)
            }

            if (lastDoc != null && lastDoc is DocumentSnapshot) {
                baseQuery = baseQuery.startAfter(lastDoc)
            }

            val snapshot = baseQuery.limit(limit).get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
            }

            val updatedItems = items.map { item ->
                val distance = if (lat != null && lng != null && item.latitude != null && item.longitude != null) {
                    calculateDistance(lat, lng, item.latitude, item.longitude)
                } else 0.0
                item.copy(distance = distance)
            }
            
            PaginatedResult(updatedItems, snapshot.documents.lastOrNull(), items.size >= limit)
        } catch (e: Exception) {
            PaginatedResult(emptyList(), null, false)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    suspend fun seedTestData() { }
}
