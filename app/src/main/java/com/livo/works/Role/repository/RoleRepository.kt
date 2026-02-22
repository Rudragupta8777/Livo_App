package com.livo.works.Role.repository

import com.livo.works.Api.RoleApiService
import com.livo.works.Role.data.PagedRoleRequests
import com.livo.works.Role.data.ProcessRequestCommand
import com.livo.works.Role.data.RoleRequestDto
import com.livo.works.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleRepository @Inject constructor(
    private val api: RoleApiService
) {
    private var cachedRequests: MutableList<RoleRequestDto> = mutableListOf()

    suspend fun requestHotelManager() = flow {
        emit(UiState.Loading)
        try {
            val response = api.requestHotelManagerRole()
            android.util.Log.d("RoleRepository", "Response Code: ${response.code()}")

            if (response.isSuccessful) {
                emit(UiState.Success(Unit))
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("RoleRepository", "Error Body: $errorBody")
                emit(UiState.Error("Server rejected request: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("RoleRepository", "Exception: ${e.message}")
            emit(UiState.Error("Network Error: ${e.message}"))
        }
    }

    suspend fun getPendingRequests(page: Int, forceRefresh: Boolean = false): Flow<UiState<PagedRoleRequests>> = flow {
        if (page == 0 && cachedRequests.isEmpty()) {
            emit(UiState.Loading)
        }

        try {
            val response = api.getPendingRequests(page, 10)

            if (response.isSuccessful && response.body()?.data != null) {
                val pagedData = response.body()!!.data!!

                if (page == 0 || forceRefresh) {
                    cachedRequests.clear()
                }

                cachedRequests.addAll(pagedData.content)
                val updatedResponse = pagedData.copy(content = ArrayList(cachedRequests))
                emit(UiState.Success(updatedResponse))

            } else {
                val errorMsg = response.body()?.error?.toString() ?: "No pending requests found"

                if (response.code() == 401) emit(UiState.SessionExpired)
                else if (response.code() == 404) emit(UiState.Success(PagedRoleRequests(emptyList(), com.livo.works.Search.data.PageInfo(10, 0, 0, 0))))
                else emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Network Error: Check your connection"))
        }
    }

    suspend fun processRequest(requestId: Long, approve: Boolean) = flow {
        emit(UiState.Loading)
        try {
            val requestBody = ProcessRequestCommand(approve)
            val response = api.processRoleRequest(requestId, requestBody)

            if (response.isSuccessful) {
                cachedRequests.removeAll { it.id == requestId }
                emit(UiState.Success(Unit))
            } else {
                val errorMsg = "Action failed: ${response.code()}"
                emit(UiState.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(UiState.Error("Connection lost"))
        }
    }
}