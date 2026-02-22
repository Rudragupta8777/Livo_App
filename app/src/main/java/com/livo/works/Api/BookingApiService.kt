package com.livo.works.Api

import com.livo.works.Booking.data.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookingApiService {

    @POST("bookings/init")
    suspend fun initBooking(
        @Body request: BookingInitRequest
    ): Response<BookingResponse>

    @POST("bookings/{id}/addGuests")
    suspend fun addGuests(
        @Path("id") bookingId: Long,
        @Body guests: List<GuestDto>?
    ): Response<BookingResponse>

    @GET("bookings/{id}")
    suspend fun getBookingDetails(
        @Path("id") id: Long
    ): Response<BookingDetailsResponse>

    @POST("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") bookingId: Long
    ): Response<BookingDetailsResponse>

    @GET("bookings")
    suspend fun getMyBookings(
        @Query("page") page: Int,
        @Query("size") size: Int = 10
    ):Response<BookingListResponse>
}