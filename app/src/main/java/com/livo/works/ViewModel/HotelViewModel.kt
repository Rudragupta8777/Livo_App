package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Search.data.HotelDetailsResponse
import com.livo.works.Search.data.HotelSearchResponse
import com.livo.works.Search.repository.SearchRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HotelViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel() {
    private val _searchState = MutableStateFlow<UiState<HotelSearchResponse>>(UiState.Idle)
    val searchState = _searchState.asStateFlow()

    private val _detailsState = MutableStateFlow<UiState<HotelDetailsResponse>>(UiState.Idle)
    val detailsState = _detailsState.asStateFlow()
    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private var isLoadingMore = false
    private var lastCity = ""
    private var lastStart = ""
    private var lastEnd = ""
    private var lastRooms = 1

    init {
        restoreLastSearch()
    }

    private fun restoreLastSearch() {
        val cached = repository.getLastSearch()
        if (cached != null) {
            _searchState.value = UiState.Success(cached)
        }
    }

    fun searchHotels(city: String, start: String, end: String, rooms: Int, isNewSearch: Boolean = true) {
        if (isNewSearch) {
            currentPage = 0
            isLastPage = false
            lastCity = city
            lastStart = start
            lastEnd = end
            lastRooms = rooms
        }

        // Prevent overlapping calls when scrolling fast
        if (isLoadingMore || (isLastPage && !isNewSearch)) return

        isLoadingMore = true

        // Show bottom progress bar if it's a pagination call
        if (!isNewSearch) {
            _isPaginating.value = true
        }

        viewModelScope.launch {
            repository.searchHotels(city, start, end, rooms, currentPage).collect { state ->
                _searchState.value = state

                if (state is UiState.Success) {
                    state.data?.let { data ->
                        val pageInfo = data.page
                        isLastPage = (pageInfo.number + 1) >= pageInfo.totalPages
                        currentPage++
                    }
                }

                // Reset loading flags when API finishes (Success or Error)
                if (state !is UiState.Loading) {
                    isLoadingMore = false
                    _isPaginating.value = false
                }
            }
        }
    }

    fun loadNextPage() {
        if (!isLastPage && !isLoadingMore) {
            searchHotels(lastCity, lastStart, lastEnd, lastRooms, isNewSearch = false)
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