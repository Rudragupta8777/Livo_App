package com.livo.works.Manager.data

data class ReportResponseWrapper(
    val timeStamp: String?,
    val data: HotelReportDto,
    val error: String?
)