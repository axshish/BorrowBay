package com.example.borrowbay.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.features.home.viewmodel.SortOption
import com.example.borrowbay.util.LocationUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class PaginatedResult<T>(
    val items: List<T>,
    val lastDoc: DocumentSnapshot?,
    val hasMore: Boolean
)

class RentalRepository {
    private val firestore = FirebaseFirestore.getInstance()

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
        trySend(staticCategories)
        awaitClose { }
    }

    fun getNearbyRentals(
        lat: Double?,
        lng: Double?,
        category: String?,
        query: String?,
        sort: SortOption,
        excludeUserId: String? = null
    ): Flow<List<RentalItem>> = callbackFlow {
        var baseQuery: Query = firestore.collection("products")
        
        if (category != null && category != "All") {
            baseQuery = baseQuery.whereEqualTo("categoryId", category)
        }

        val listener = baseQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            var items = snapshot?.documents?.mapNotNull { doc ->
                val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                val isAvail = doc.getBoolean("available") ?: doc.getBoolean("isAvailable") ?: true
                item?.copy(isAvailable = isAvail)
            } ?: emptyList()

            items = items.filter { it.isAvailable }
            if (excludeUserId != null) {
                items = items.filter { it.ownerId != excludeUserId }
            }

            if (!query.isNullOrBlank()) {
                items = items.filter { it.name.contains(query, ignoreCase = true) }
            }

            val nearbyItems = items.map { item ->
                val distance = if (lat != null && lng != null && item.latitude != null && item.longitude != null) {
                    LocationUtils.calculateDistance(lat, lng, item.latitude, item.longitude)
                } else 0.0
                item.copy(distance = distance)
            }

            val filteredNearby = if (lat != null && lng != null) {
                nearbyItems.filter { it.distance <= 50.0 }
            } else {
                nearbyItems
            }

            val sortedItems = when (sort) {
                SortOption.DISTANCE -> filteredNearby.sortedBy { it.distance }
                SortOption.PRICE_LOW_HIGH -> filteredNearby.sortedBy { it.pricePerDay }
                SortOption.PRICE_HIGH_LOW -> filteredNearby.sortedByDescending { it.pricePerDay }
                else -> filteredNearby
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
        lastDoc: Any?,
        excludeUserId: String? = null
    ): PaginatedResult<RentalItem> {
        return try {
            var baseQuery: Query = firestore.collection("products")
            
            if (category != null && category != "All") {
                baseQuery = baseQuery.whereEqualTo("categoryId", category)
            }

            if (lastDoc != null && lastDoc is DocumentSnapshot) {
                baseQuery = baseQuery.startAfter(lastDoc)
            }

            val snapshot = baseQuery.limit(limit * 3).get().await()
            var items = snapshot.documents.mapNotNull { doc ->
                val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                val isAvail = doc.getBoolean("available") ?: doc.getBoolean("isAvailable") ?: true
                item?.copy(isAvailable = isAvail)
            }

            items = items.filter { it.isAvailable }
            if (excludeUserId != null) {
                items = items.filter { it.ownerId != excludeUserId }
            }

            val finalItems = items.take(limit.toInt())

            val updatedItems = finalItems.map { item ->
                val distance = if (lat != null && lng != null && item.latitude != null && item.longitude != null) {
                    LocationUtils.calculateDistance(lat, lng, item.latitude, item.longitude)
                } else 0.0
                item.copy(distance = distance)
            }
            
            PaginatedResult(updatedItems, snapshot.documents.lastOrNull(), snapshot.size() >= limit)
        } catch (e: Exception) {
            PaginatedResult(emptyList(), null, false)
        }
    }

    suspend fun rentItem(itemId: String, renterId: String, durationDays: Int): Boolean {
        return try {
            firestore.collection("products").document(itemId).update(
                mapOf(
                    "available" to false,
                    "isAvailable" to false,
                    "renterId" to renterId,
                    "rentedAt" to System.currentTimeMillis(),
                    "rentalDurationDays" to durationDays
                )
            ).await()
            true
        } catch (e: Exception) {
            Log.e("RentalRepository", "Error renting item", e)
            false
        }
    }

    fun getUserListings(userId: String): Flow<List<RentalItem>> = callbackFlow {
        val listener = firestore.collection("products")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                    val isAvail = doc.getBoolean("available") ?: doc.getBoolean("isAvailable") ?: true
                    val rentalDays = doc.getLong("rentalDurationDays")?.toInt()
                    item?.copy(isAvailable = isAvail, rentalDurationDays = rentalDays)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun getUserRentals(userId: String): Flow<List<RentalItem>> = callbackFlow {
        val listener = firestore.collection("products")
            .whereEqualTo("renterId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyOf())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                    val isAvail = doc.getBoolean("available") ?: doc.getBoolean("isAvailable") ?: true
                    val rentalDays = doc.getLong("rentalDurationDays")?.toInt()
                    item?.copy(isAvailable = isAvail, rentalDurationDays = rentalDays)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}

private fun <T> emptyOf(): List<T> = emptyList()
