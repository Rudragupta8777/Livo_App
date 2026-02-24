package com.livo.works.Api

import com.livo.works.Auth.data.ApiResponse
import com.livo.works.Manager.data.BookingsResponseWrapper
import com.livo.works.Manager.data.ManagerHotelDetailsDto
import com.livo.works.Manager.data.PagedManagerHotels
import com.livo.works.Manager.data.ReportResponseWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ManagerApiService {

    // 1. Get All Hotels for Manager (Paginated)
    @GET("admin/hotels")
    suspend fun getMyHotels(
        @Query("page") page: Int,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<PagedManagerHotels>>

    // 2. Get Specific Hotel Details
    @GET("admin/hotels/{id}")
    suspend fun getHotelById(
        @Path("id") hotelId: Long
    ): Response<ApiResponse<ManagerHotelDetailsDto>>

    // 3. Create New Hotel
    @POST("admin/hotels")
    suspend fun createHotel(
        @Body request: ManagerHotelDetailsDto
    ): Response<ApiResponse<ManagerHotelDetailsDto>>

    // 4. Update Existing Hotel
    @PUT("admin/hotels/{id}")
    suspend fun updateHotel(
        @Path("id") hotelId: Long,
        @Body request: ManagerHotelDetailsDto
    ): Response<ApiResponse<ManagerHotelDetailsDto>>

    // 5. Activate Hotel
    @PATCH("admin/hotels/{id}")
    suspend fun activateHotel(
        @Path("id") hotelId: Long
    ): Response<Void>

    // 6. Delete Hotel
    @DELETE("admin/hotels/{id}")
    suspend fun deleteHotel(
        @Path("id") hotelId: Long
    ): Response<Void>

    @GET("admin/hotels/{hotelId}/bookings")
    suspend fun getHotelBookings(
        @Path("hotelId") hotelId: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<BookingsResponseWrapper>

    @GET("admin/hotels/{hotelId}/report")
    suspend fun getHotelReport(
        @Path("hotelId") hotelId: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ReportResponseWrapper>
}