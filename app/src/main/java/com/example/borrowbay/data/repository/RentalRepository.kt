package com.example.borrowbay.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.data.model.Owner
import com.example.borrowbay.features.home.viewmodel.SortOption
import com.example.borrowbay.util.LocationUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

data class PaginatedResult<T>(
    val items: List<T>,
    val lastDoc: DocumentSnapshot?,
    val hasMore: Boolean
)

class RentalRepository {
    private val firestore = FirebaseFirestore.getInstance()

    val staticCategories = listOf(
        Category("Electronics", "Electronics", "📱"),
        Category("Photography", "Photography", "📷"),
        Category("Sports", "Sports", "⚽"),
        Category("Fitness", "Fitness", "🏋️"),
        Category("Tools", "Tools", "🔧"),
        Category("Outdoors", "Outdoors", "⛺"),
        Category("Gardening", "Gardening", "🌻"),
        Category("Vehicles", "Vehicles", "🚗"),
        Category("Music", "Music", "🎸"),
        Category("Gaming", "Gaming", "🎮"),
        Category("Books", "Books", "📖"),
        Category("Appliances", "Appliances", "🍳"),
        Category("Clothing", "Clothing", "👕"),
        Category("Toys", "Toys", "🧸"),
        Category("Party", "Party", "🎉"),
        Category("Office", "Office", "💼")
    )

    private val dummyRentals = listOf(
        RentalItem(
            id = "dummy1",
            name = "Sony A7III Camera",
            description = "Full-frame mirrorless camera with 28-70mm lens. Perfect for photography and 4K video.",
            categoryId = "Photography",
            pricePerDay = 800.0,
            securityDeposit = 5000.0,
            distance = 1.2,
            location = "Connaught Place, Delhi",
            imageUrls = listOf("https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=800&q=80"),
            owner = Owner(id = "o1", name = "John Carter")
        ),
        RentalItem(
            id = "dummy2",
            name = "Mountain Trek Bike",
            description = "High-performance mountain bike with front suspension and disc brakes.",
            categoryId = "Sports",
            pricePerDay = 450.0,
            securityDeposit = 2000.0,
            distance = 3.5,
            location = "Andheri, Mumbai",
            imageUrls = listOf("https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=800&q=80"),
            owner = Owner(id = "o2", name = "Sarah Smith")
        ),
        RentalItem(
            id = "dummy3",
            name = "Power Drill & Set",
            description = "Cordless power drill with 2 batteries and a complete bit set for home repairs.",
            categoryId = "Tools",
            pricePerDay = 250.0,
            securityDeposit = 1500.0,
            distance = 0.8,
            location = "Whitefield, Bangalore",
            imageUrls = listOf("https://images.unsplash.com/photo-1504148455328-497c5ef215d0?auto=format&fit=crop&w=800&q=80"),
            owner = Owner(id = "o3", name = "Mike Ross")
        )
    )

    fun getCategories(): Flow<List<Category>> = flow {
        emit(staticCategories)
    }

    fun getNearbyRentals(
        lat: Double?,
        lng: Double?,
        category: String?,
        query: String?,
        sort: SortOption,
        excludeUserId: String? = null,
        radiusKm: Double? = null
    ): Flow<List<RentalItem>> = callbackFlow {
        var baseQuery: Query = firestore.collection("products")
        
        if (category != null && category != "All") {
            baseQuery = baseQuery.whereEqualTo("categoryId", category)
        }

        val listener = baseQuery.addSnapshotListener { snapshot, error ->
            var items = snapshot?.documents?.mapNotNull { doc ->
                val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                val isAvail = doc.getBoolean("available") ?: doc.getBoolean("isAvailable") ?: true
                item?.copy(isAvailable = isAvail)
            } ?: emptyList()

            val processedItems = items.map { item ->
                val distance = if (lat != null && lng != null && item.latitude != null && item.longitude != null) {
                    LocationUtils.calculateDistance(lat, lng, item.latitude, item.longitude)
                } else 0.0
                item.copy(distance = distance)
            }

            val combined = (processedItems + dummyRentals).distinctBy { it.id }
            
            var filtered = combined.filter { it.isAvailable }
            if (excludeUserId != null) {
                filtered = filtered.filter { it.ownerId != excludeUserId }
            }
            if (!query.isNullOrBlank()) {
                filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
            }
            if (category != null && category != "All") {
                filtered = filtered.filter { it.categoryId == category }
            }
            
            if (radiusKm != null && lat != null && lng != null) {
                // When searching, we relax the radius constraint if a query exists
                if (query.isNullOrBlank()) {
                    filtered = filtered.filter { it.distance <= radiusKm }
                }
            }

            val sorted = when (sort) {
                SortOption.DISTANCE -> filtered.sortedBy { it.distance }
                SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.pricePerDay }
                SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.pricePerDay }
                else -> filtered
            }

            trySend(sorted)
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
        excludeUserId: String? = null,
        minDistanceKm: Double? = null
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
                val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                val isAvail = doc.getBoolean("available") ?: doc.getBoolean("isAvailable") ?: true
                item?.copy(isAvailable = isAvail)
            }

            val processedItems = items.map { item ->
                val distance = if (lat != null && lng != null && item.latitude != null && item.longitude != null) {
                    LocationUtils.calculateDistance(lat, lng, item.latitude, item.longitude)
                } else 0.0
                item.copy(distance = distance)
            }

            val combined = if (lastDoc == null) {
                (processedItems + dummyRentals).distinctBy { it.id }
            } else processedItems

            var filtered = combined.filter { it.isAvailable }
            
            if (excludeUserId != null) {
                filtered = filtered.filter { it.ownerId != excludeUserId }
            }
            
            // When searching, we don't apply the minimum distance barrier for global results
            // so that search results show everything regardless of distance
            if (query.isNullOrBlank() && minDistanceKm != null && lat != null && lng != null) {
                filtered = filtered.filter { it.distance > minDistanceKm }
            }
            
            if (!query.isNullOrBlank()) {
                filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
            }
            if (category != null && category != "All") {
                filtered = filtered.filter { it.categoryId == category }
            }

            PaginatedResult(filtered, snapshot.documents.lastOrNull(), snapshot.size() >= limit)
        } catch (e: Exception) {
            PaginatedResult(dummyRentals, null, false)
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

    suspend fun markItemAsReturned(itemId: String): Boolean {
        return try {
            firestore.collection("products").document(itemId).update(
                mapOf(
                    "available" to true,
                    "isAvailable" to true,
                    "renterId" to null,
                    "rentedAt" to null,
                    "rentalDurationDays" to null
                )
            ).await()
            true
        } catch (e: Exception) {
            Log.e("RentalRepository", "Error returning item", e)
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
                    item?.copy(isAvailable = isAvail)
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
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                    val isAvail = doc.getBoolean("available") ?: doc.getBoolean("isAvailable") ?: true
                    item?.copy(isAvailable = isAvail)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}
