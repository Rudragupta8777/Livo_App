package com.livo.works.Room.repository

import com.livo.works.Api.RoomApiService
import com.livo.works.Room.data.CreateRoomRequestDto
import com.livo.works.Room.data.RoomDto
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RoomRepository @Inject constructor(
    private val api: RoomApiService
) {

    fun getAllRoomsForHotel(hotelId: Long): Flow<UiState<List<RoomDto>>> = flow {
        emit(UiState.Loading)
        val response = api.getAllRoomsForHotel(hotelId)
        if (response.isSuccessful && response.body() != null) {
            emit(UiState.Success(response.body()!!.data))
        } else {
            emit(UiState.Error("Failed to fetch rooms: ${response.code()}"))
        }
    }.catch { e ->
        emit(UiState.Error(e.localizedMessage ?: "Network error"))
    }

    fun createRoom(hotelId: Long, request: CreateRoomRequestDto): Flow<UiState<RoomDto>> = flow {
        emit(UiState.Loading)
        val response = api.createRoom(hotelId, request)
        if (response.isSuccessful && response.body() != null) {
            emit(UiState.Success(response.body()!!.data))
        } else {
            emit(UiState.Error("Failed to create room: ${response.code()}"))
        }
    }.catch { e ->
        emit(UiState.Error(e.localizedMessage ?: "Network error"))
    }

    fun getRoomById(roomId: Long): Flow<UiState<RoomDto>> = flow {
        emit(UiState.Loading)
        val response = api.getRoomById(roomId)
        if (response.isSuccessful && response.body() != null) {
            emit(UiState.Success(response.body()!!.data))
        } else {
            emit(UiState.Error("Failed to fetch room details: ${response.code()}"))
        }
    }.catch { e ->
        emit(UiState.Error(e.localizedMessage ?: "Network error"))
    }

    fun deleteRoom(roomId: Long): Flow<UiState<Unit>> = flow {
        emit(UiState.Loading)
        val response = api.deleteRoom(roomId)
        if (response.isSuccessful) {
            emit(UiState.Success(Unit))
        } else {
            emit(UiState.Error("Failed to delete room: ${response.code()}"))
        }
    }.catch { e ->
        emit(UiState.Error(e.localizedMessage ?: "Network error"))
    }
}