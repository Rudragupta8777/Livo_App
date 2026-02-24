package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Manager.data.ManagerHotelDetailsDto
import com.livo.works.Manager.data.PagedManagerHotels
import com.livo.works.Manager.repository.ManagerRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagerViewModel @Inject constructor(
    private val repository: ManagerRepository
) : ViewModel() {

    private val _hotelsListState = MutableStateFlow<UiState<PagedManagerHotels>>(UiState.Idle)
    val hotelsListState = _hotelsListState.asStateFlow()

    private val _hotelDetailsState = MutableStateFlow<UiState<ManagerHotelDetailsDto>>(UiState.Idle)
    val hotelDetailsState = _hotelDetailsState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState = _actionState.asStateFlow()

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private var isLoadingMore = false

    init {
        fetchMyHotels(forceRefresh = true)
    }

    fun fetchMyHotels(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            currentPage = 0
            isLastPage = false
            isLoadingMore = false

            _hotelsListState.value = UiState.Loading
        }

        if (isLoadingMore || (isLastPage && !forceRefresh)) return
        isLoadingMore = true
        if (currentPage > 0) _isPaginating.value = true

        viewModelScope.launch {
            repository.getMyHotels(currentPage, forceRefresh).collect { state ->
                _hotelsListState.value = state
                if (state is UiState.Success) {
                    state.data?.let {
                        isLastPage = (it.page.number + 1) >= it.page.totalPages
                        currentPage++
                    }
                }
                if (state !is UiState.Loading) {
                    isLoadingMore = false
                    _isPaginating.value = false
                }
            }
        }
    }

    fun fetchHotelDetails(id: Long) {
        viewModelScope.launch {
            repository.getHotelDetails(id).collect { _hotelDetailsState.value = it }
        }
    }

    fun createHotel(hotel: ManagerHotelDetailsDto) {
        viewModelScope.launch {
            repository.createHotel(hotel).collect { state ->
                if (state is UiState.Success) {
                    _actionState.value = UiState.Success(Unit)
                    fetchMyHotels(true)
                } else if (state is UiState.Error) _actionState.value = state
                else if (state is UiState.Loading) _actionState.value = UiState.Loading
            }
        }
    }

    fun updateHotel(id: Long, hotel: ManagerHotelDetailsDto) {
        viewModelScope.launch {
            repository.updateHotel(id, hotel).collect { state ->
                if (state is UiState.Success) {
                    _actionState.value = UiState.Success(Unit)
                    // Refresh the list so the updated data shows up
                    fetchMyHotels(true)
                } else if (state is UiState.Error) {
                    _actionState.value = state
                } else if (state is UiState.Loading) {
                    _actionState.value = UiState.Loading
                }
            }
        }
    }

    fun activateHotel(id: Long) {
        viewModelScope.launch {
            repository.activateHotel(id).collect { state ->
                _actionState.value = state
                if (state is UiState.Success) fetchMyHotels(true)
            }
        }
    }

    fun deleteHotel(id: Long) {
        viewModelScope.launch {
            repository.deleteHotel(id).collect { state ->
                _actionState.value = state
                if (state is UiState.Success) fetchMyHotels(true)
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}