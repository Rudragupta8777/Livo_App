package com.livo.works.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Role.data.PagedRoleRequests
import com.livo.works.Role.repository.RoleRepository
import com.livo.works.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: RoleRepository
) : ViewModel() {

    private val _requestsState = MutableStateFlow<UiState<PagedRoleRequests>>(UiState.Idle)
    val requestsState = _requestsState.asStateFlow()

    private val _processState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val processState = _processState.asStateFlow()

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private var isLoadingMore = false

    init {
        fetchRequests(forceRefresh = true)
    }

    fun fetchRequests(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            currentPage = 0
            isLastPage = false
            isLoadingMore = false
        }

        if (isLoadingMore || (isLastPage && !forceRefresh)) return
        isLoadingMore = true
        if (currentPage > 0) _isPaginating.value = true

        viewModelScope.launch {
            repository.getPendingRequests(currentPage, forceRefresh).collect { state ->
                _requestsState.value = state
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

    fun processRequest(requestId: Long, approve: Boolean) {
        viewModelScope.launch {
            repository.processRequest(requestId, approve).collect { state ->
                _processState.value = state
                if (state is UiState.Success) {
                    // Refresh the current list after processing to remove the item
                    fetchRequests(forceRefresh = true)
                }
            }
        }
    }

    fun resetProcessState() {
        _processState.value = UiState.Idle
    }
}