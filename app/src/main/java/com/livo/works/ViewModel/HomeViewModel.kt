package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Search.data.BestHotelDto
import com.livo.works.Search.repository.SearchRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel() {

    private val _bestHotelsState = MutableStateFlow<UiState<List<BestHotelDto>>>(UiState.Loading)
    val bestHotelsState: StateFlow<UiState<List<BestHotelDto>>> = _bestHotelsState

    init {
        fetchBestHotels(forceRefresh = false)
    }

    fun fetchBestHotels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getBestHotels(forceRefresh).collect {
                _bestHotelsState.value = it
            }
        }
    }
}