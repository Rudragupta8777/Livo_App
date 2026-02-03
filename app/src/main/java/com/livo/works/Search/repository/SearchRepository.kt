package com.livo.works.Search.repository

import com.livo.works.Api.SearchApiService
import com.livo.works.Search.data.BestHotelDto
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Keeps this instance alive while the app is running
class SearchRepository @Inject constructor(
    private val api: SearchApiService
) {
    // This variable holds the data in memory
    private var cachedHotels: List<BestHotelDto>? = null

    suspend fun getBestHotels(forceRefresh: Boolean = false): Flow<UiState<List<BestHotelDto>>> = flow {
        // 1. Return Cache if available and NOT forcing a refresh
        if (!forceRefresh && cachedHotels != null) {
            emit(UiState.Success(cachedHotels!!))
            return@flow
        }

        // 2. Otherwise, fetch from Network
        emit(UiState.Loading)
        try {
            val response = api.getBestHotels()

            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!

                // SAVE to Cache
                cachedHotels = data

                emit(UiState.Success(data))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to fetch best hotels"
                emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }
}