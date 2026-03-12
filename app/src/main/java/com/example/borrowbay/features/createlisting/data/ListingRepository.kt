package com.example.borrowbay.features.createlisting.data

import com.example.borrowbay.core.supabase
import com.example.borrowbay.features.createlisting.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ListingRepository {
    private val client = supabase
    private val firestore = FirebaseFirestore.getInstance()
    private val itemsCollection = firestore.collection("items")

    suspend fun addItem(item: Item, imageBytesList: List<ByteArray>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val imageUrls = mutableListOf<String>()
            val bucket = client.storage.from("item-images")

            // 1. Upload images to Supabase Storage
            imageBytesList.forEach { bytes ->
                val fileName = "${UUID.randomUUID()}.jpg"
                bucket.upload(fileName, bytes)
                imageUrls.add(bucket.publicUrl(fileName))
            }

            // 2. Save Item details to Firebase Firestore
            val finalItem = item.copy(imageUrls = imageUrls)
            val documentRef = if (finalItem.id.isNullOrBlank()) {
                itemsCollection.document()
            } else {
                itemsCollection.document(finalItem.id)
            }
            
            val itemToSave = finalItem.copy(id = documentRef.id)
            documentRef.set(itemToSave).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
