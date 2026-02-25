package com.livo.works.Room.data

data class SingleRoomResponseWrapper(
    val timeStamp: String?,
    val data: RoomDto,
    val error: String?
)