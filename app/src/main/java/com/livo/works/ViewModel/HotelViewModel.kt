package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Hotel.data.HotelDetailsResponse
import com.livo.works.Hotel.data.HotelSearchResponse
import com.livo.works.Hotel.repository.HotelRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HotelViewModel @Inject constructor(
    private val repository: HotelRepository
) : ViewModel() {

    // Search State
    private val _searchState = MutableStateFlow<UiState<HotelSearchResponse>>(UiState.Idle)
    val searchState = _searchState.asStateFlow()

    // Details State
    private val _detailsState = MutableStateFlow<UiState<HotelDetailsResponse>>(UiState.Idle)
    val detailsState = _detailsState.asStateFlow()

    fun searchHotels(city: String, start: String, end: String, rooms: Int) {
        viewModelScope.launch {
            repository.searchHotels(city, start, end, rooms).collect {
                _searchState.value = it
            }
        }
    }

    fun getHotelDetails(id: Long, start: String, end: String, rooms: Int) {
        viewModelScope.launch {
            repository.getHotelDetails(id, start, end, rooms).collect {
                _detailsState.value = it
            }
        }
    }
}