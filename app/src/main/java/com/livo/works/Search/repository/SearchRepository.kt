package com.livo.works.Search.repository

import com.livo.works.Api.SearchApiService
import com.livo.works.Search.data.BestHotelDto
import com.livo.works.Search.data.HotelSearchRequest
import com.livo.works.Search.data.HotelSearchResponse
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val api: SearchApiService
) {
    private var cachedHotels: List<BestHotelDto>? = null
    private var cachedSearchResponse: HotelSearchResponse? = null
    fun getLastSearch(): HotelSearchResponse? {
        return cachedSearchResponse
    }
    suspend fun getBestHotels(forceRefresh: Boolean = false): Flow<UiState<List<BestHotelDto>>> = flow {
        // Return Cache if available and NOT forcing a refresh
        if (!forceRefresh && cachedHotels != null) {
            emit(UiState.Success(cachedHotels!!))
            return@flow
        }

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

    // 2. Search Hotels (Search Screen)
    suspend fun searchHotels(city: String, start: String, end: String, rooms: Int) = flow {
        emit(UiState.Loading)
        try {
            val request = HotelSearchRequest(city, start, end, rooms)
            val response = api.searchHotels(request)

            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!

                // SAVE to Cache
                cachedSearchResponse = data

                emit(UiState.Success(data))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Search Failed"
                if(response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }

    // 3. Get Hotel Details (Details Screen)
    suspend fun getHotelDetails(id: Long, start: String, end: String, rooms: Int) = flow {
        emit(UiState.Loading)
        try {
            val response = api.getHotelDetails(id, start, end, rooms)

            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()!!.data))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Failed to fetch details"
                if(response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }
}