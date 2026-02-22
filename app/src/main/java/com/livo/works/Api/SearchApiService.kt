package com.livo.works.Api

import com.livo.works.Auth.data.ApiResponse
import com.livo.works.Search.data.BestHotelResponse
import com.livo.works.Search.data.HotelDetailsResponse
import com.livo.works.Search.data.HotelSearchRequest
import com.livo.works.Search.data.HotelSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApiService {
    @GET("hotels/{id}")
    suspend fun getHotelDetails(
        @Path("id") id: Long,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("roomsCount") roomsCount: Int
    ): Response<ApiResponse<HotelDetailsResponse>>

    @GET("hotels/best")
    suspend fun getBestHotels(): Response<BestHotelResponse>

    @POST("hotels/search")
    suspend fun searchHotels(
        @Body request: HotelSearchRequest,
        @Query("page") page: Int,
        @Query("size") size: Int = 10
    ):Response<ApiResponse<HotelSearchResponse>>
}