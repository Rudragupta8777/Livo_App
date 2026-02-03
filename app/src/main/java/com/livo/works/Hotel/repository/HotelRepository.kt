package com.livo.works.Hotel.repository

import com.livo.works.Api.HotelApiService
import com.livo.works.Hotel.data.HotelDetailsResponse
import com.livo.works.Hotel.data.HotelSearchRequest
import com.livo.works.Hotel.data.HotelSearchResponse
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotelRepository @Inject constructor(
    private val api: HotelApiService
) {
    private var cachedSearchResponse: HotelSearchResponse? = null

    fun getLastSearch(): HotelSearchResponse? {
        return cachedSearchResponse
    }

    suspend fun searchHotels(city: String, start: String, end: String, rooms: Int) = flow {
        emit(UiState.Loading)
        try {
            val request = HotelSearchRequest(city, start, end, rooms)
            val response = api.searchHotels(request)

            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!

                // 4. Save to Cache
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