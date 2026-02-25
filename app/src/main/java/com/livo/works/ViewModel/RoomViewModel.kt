package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Room.data.CreateRoomRequestDto
import com.livo.works.Room.data.RoomDto
import com.livo.works.Room.repository.RoomRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val repository: RoomRepository
) : ViewModel() {

    private val _roomsListState = MutableStateFlow<UiState<List<RoomDto>>>(UiState.Idle)
    val roomsListState = _roomsListState.asStateFlow()

    private val _roomDetailsState = MutableStateFlow<UiState<RoomDto>>(UiState.Idle)
    val roomDetailsState = _roomDetailsState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState = _actionState.asStateFlow()

    fun fetchRoomsForHotel(hotelId: Long) {
        viewModelScope.launch {
            repository.getAllRoomsForHotel(hotelId).collect { state ->
                _roomsListState.value = state
            }
        }
    }

    fun fetchRoomById(roomId: Long) {
        viewModelScope.launch {
            repository.getRoomById(roomId).collect { state ->
                _roomDetailsState.value = state
            }
        }
    }

    fun createRoom(hotelId: Long, request: CreateRoomRequestDto) {
        viewModelScope.launch {
            repository.createRoom(hotelId, request).collect { state ->
                if (state is UiState.Success) {
                    _actionState.value = UiState.Success(Unit)
                    // Refresh the list automatically after creation
                    fetchRoomsForHotel(hotelId)
                } else if (state is UiState.Error) {
                    _actionState.value = state
                } else if (state is UiState.Loading) {
                    _actionState.value = UiState.Loading
                }
            }
        }
    }

    fun deleteRoom(roomId: Long, hotelId: Long) {
        viewModelScope.launch {
            repository.deleteRoom(roomId).collect { state ->
                if (state is UiState.Success) {
                    _actionState.value = UiState.Success(Unit)
                    // Refresh the list automatically after deletion
                    fetchRoomsForHotel(hotelId)
                } else if (state is UiState.Error) {
                    _actionState.value = state
                } else if (state is UiState.Loading) {
                    _actionState.value = UiState.Loading
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}