package com.livo.works.Manager.data

import com.livo.works.Search.data.PageInfo

data class PagedManagerHotels(
    val content: List<ManagerHotelDto>,
    val page: PageInfo
)