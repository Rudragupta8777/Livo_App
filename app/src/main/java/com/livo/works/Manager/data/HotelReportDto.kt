package com.livo.works.Manager.data

data class HotelReportDto(
    val confirmedBookings: Int,
    val confirmedRevenue: Double,
    val avgRevenuePerConfirmedBooking: Double,
    val cancelledBookings: Int,
    val revenueLostToCancellations: Double,
    val totalRefundsProcessed: Double,
    val cancellationRate: Double
)