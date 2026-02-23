package com.livo.works.Manager.repository

import com.livo.works.Api.ManagerApiService
import com.livo.works.Manager.data.ManagerHotelDetailsDto
import com.livo.works.Manager.data.ManagerHotelDto
import com.livo.works.Manager.data.PagedManagerHotels
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagerRepository @Inject constructor(
    private val api: ManagerApiService
) {
    private var cachedHotels: MutableList<ManagerHotelDto> = mutableListOf()

    suspend fun getMyHotels(page: Int, forceRefresh: Boolean = false): Flow<UiState<PagedManagerHotels>> = flow {
        if (page == 0 && cachedHotels.isEmpty()) emit(UiState.Loading)

        try {
            val response = api.getMyHotels(page, 10)
            if (response.isSuccessful && response.body()?.data != null) {
                val pagedData = response.body()!!.data!!

                if (page == 0 || forceRefresh) cachedHotels.clear()

                cachedHotels.addAll(pagedData.content)
                emit(UiState.Success(pagedData.copy(content = ArrayList(cachedHotels))))
            } else {
                val errorMsg = response.body()?.error?.message ?: "Failed to fetch hotels"
                if (response.code() == 401) emit(UiState.SessionExpired)
                else if (response.code() == 404) emit(UiState.Success(PagedManagerHotels(emptyList(), com.livo.works.Search.data.PageInfo(10,0,0,0))))
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun getHotelDetails(id: Long) = flow {
        emit(UiState.Loading)
        try {
            val response = api.getHotelById(id)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()!!.data!!))
            } else {
                if (response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(response.body()?.error?.message ?: "Failed to fetch details"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun createHotel(hotel: ManagerHotelDetailsDto) = flow {
        emit(UiState.Loading)
        try {
            val response = api.createHotel(hotel)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()!!.data!!))
            } else {
                if (response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(response.body()?.error?.message ?: "Failed to create hotel"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun updateHotel(id: Long, hotel: ManagerHotelDetailsDto) = flow {
        emit(UiState.Loading)
        try {
            val response = api.updateHotel(id, hotel)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(UiState.Success(response.body()!!.data!!))
            } else {
                if (response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error(response.body()?.error?.message ?: "Failed to update hotel"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun activateHotel(id: Long) = flow {
        emit(UiState.Loading)
        try {
            val response = api.activateHotel(id)
            if (response.isSuccessful) emit(UiState.Success(Unit))
            else {
                if (response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error("Failed to activate hotel"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }

    suspend fun deleteHotel(id: Long) = flow {
        emit(UiState.Loading)
        try {
            val response = api.deleteHotel(id)
            if (response.isSuccessful) {
                cachedHotels.removeAll { it.id == id }
                emit(UiState.Success(Unit))
            } else {
                if (response.code() == 401) emit(UiState.SessionExpired)
                else emit(UiState.Error("Failed to delete hotel"))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error"))
        }
    }
}