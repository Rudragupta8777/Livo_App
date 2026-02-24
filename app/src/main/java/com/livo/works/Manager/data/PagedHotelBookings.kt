package com.livo.works.Manager.data

import com.livo.works.Search.data.PageInfo

data class PagedHotelBookings(
    val content: List<HotelBookingDto> = emptyList(),
    val page: PageInfo? = null
)