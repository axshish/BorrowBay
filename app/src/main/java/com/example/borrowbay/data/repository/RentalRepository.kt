package com.example.borrowbay.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.Owner
import com.example.borrowbay.data.model.RentalItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RentalRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getCategories(): Flow<List<Category>> = callbackFlow {
        val listener = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    Category(id = id, name = name)
                } ?: emptyList()
                trySend(categories)
            }
        awaitClose { listener.remove() }
    }

    fun getNearbyRentals(): Flow<List<RentalItem>> = callbackFlow {
        val listener = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun getTrendingRentals(): Flow<List<RentalItem>> = callbackFlow {
        // For now, just returning the same products as trending
        val listener = firestore.collection("products")
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMyListings(userId: String): List<RentalItem> {
        return try {
            val snapshot = firestore.collection("products")
                .whereEqualTo("sellerId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(RentalItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRentedItems(userId: String): List<RentalItem> {
        return try {
            val snapshot = firestore.collection("rentals")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            // This would normally join with products, but simplified for now
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
