package com.livo.works.Payment.data

data class PaymentInitData(
    val bookingId: Long,
    val amount: Double,
    val currency: String,
    val razorpayOrderId: String,
    val razorpayKeyId: String,
    val companyName: String,
    val description: String,
    val userEmail: String
)