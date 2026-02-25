package com.livo.works.Room.data

data class ListRoomResponseWrapper(
    val timeStamp: String?,
    val data: List<RoomDto>,
    val error: String?
)