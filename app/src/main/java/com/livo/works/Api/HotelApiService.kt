package com.livo.works.Api

import com.livo.works.Auth.data.ApiResponse
import com.livo.works.Hotel.data.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface HotelApiService {

    @POST("api/v1/hotels/search")
    suspend fun searchHotels(
        @Body request: HotelSearchRequest
    ): Response<ApiResponse<HotelSearchResponse>>

    @GET("api/v1/hotels/{id}")
    suspend fun getHotelDetails(
        @Path("id") id: Long,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("roomsCount") roomsCount: Int
    ): Response<ApiResponse<HotelDetailsResponse>>
}