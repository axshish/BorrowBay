package com.example.borrowbay.data.repository

import com.example.borrowbay.core.supabase
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.RentalItem
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RentalRepository {
    fun getCategories(): Flow<List<Category>> = flow {
        try {
            val response = supabase.postgrest["categories"].select().decodeList<Category>()
            emit(response)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getNearbyRentals(): Flow<List<RentalItem>> = flow {
        try {
            val response = supabase.postgrest["items"].select().decodeList<RentalItem>()
            emit(response)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getTrendingRentals(): Flow<List<RentalItem>> = flow {
        try {
            val response = supabase.postgrest["items"].select().decodeList<RentalItem>()
            // For now, returning all items as trending since rating is removed
            emit(response)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getMyListings(userId: String): List<RentalItem> {
        return try {
            supabase.postgrest["items"].select {
                filter {
                    eq("ownerId", userId)
                }
            }.decodeList<RentalItem>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRentedItems(userId: String): List<RentalItem> {
        return try {
            supabase.postgrest["items"].select {
                filter {
                    eq("rented_by", userId)
                }
            }.decodeList<RentalItem>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
