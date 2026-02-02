package com.livo.works.Payment.data

data class PaymentVerifyRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String
)