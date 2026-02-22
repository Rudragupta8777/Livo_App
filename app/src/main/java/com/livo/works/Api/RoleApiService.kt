package com.livo.works.Api

import com.livo.works.Auth.data.ApiResponse
import com.livo.works.Role.data.PagedRoleRequests
import com.livo.works.Role.data.ProcessRequestCommand
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RoleApiService {
    @POST("roles/request/hotel_manager")
    suspend fun requestHotelManagerRole(): Response<Void>

    @GET("roles/requests")
    suspend fun getPendingRequests(
        @Query("page") page: Int,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PagedRoleRequests>>

    @PUT("roles/request/{requestId}/process")
    suspend fun processRoleRequest(
        @Path("requestId") requestId: Long,
        @Body request: ProcessRequestCommand
    ): Response<Void>
}