package com.livo.works.Api

import com.livo.works.Room.data.CreateRoomRequestDto
import com.livo.works.Room.data.ListRoomResponseWrapper
import com.livo.works.Room.data.SingleRoomResponseWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RoomApiService {

    @GET("admin/rooms/hotel/{hotelId}")
    suspend fun getAllRoomsForHotel(
        @Path("hotelId") hotelId: Long
    ): Response<ListRoomResponseWrapper>

    @POST("admin/rooms/hotel/{hotelId}")
    suspend fun createRoom(
        @Path("hotelId") hotelId: Long,
        @Body request: CreateRoomRequestDto
    ): Response<SingleRoomResponseWrapper>

    @GET("admin/rooms/{roomId}")
    suspend fun getRoomById(
        @Path("roomId") roomId: Long
    ): Response<SingleRoomResponseWrapper>

    @DELETE("admin/rooms/{roomId}")
    suspend fun deleteRoom(
        @Path("roomId") roomId: Long
    ): Response<Unit> // 204 No Content has no body
}