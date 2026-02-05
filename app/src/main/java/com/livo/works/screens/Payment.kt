package com.livo.works.screens

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.livo.works.Payment.data.PaymentInitData
import com.livo.works.R
import com.livo.works.ViewModel.PaymentViewModel
import com.livo.works.databinding.ActivityPaymentBinding
import com.livo.works.util.UiState
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject

@AndroidEntryPoint
class Payment : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var binding: ActivityPaymentBinding
    private val viewModel: PaymentViewModel by viewModels()
    private var razorpayOrderId: String = ""
    private val dotAnimators = mutableListOf<ObjectAnimator>()
    private var isProcessing = false

    // FIX 1: Make bookingId a class property
    private var bookingId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToHotelDetails()
            }
        })

        startDotAnimation()

        // FIX 2: Assign value to class property
        bookingId = intent.getLongExtra("BOOKING_ID", -1)

        if (bookingId != -1L && !isProcessing) {
            isProcessing = true
            binding.tvStatus.text = "Securing Connection..."
            viewModel.startPayment(bookingId)
        } else if (bookingId == -1L) {
            Toast.makeText(this, "Invalid Booking Data", Toast.LENGTH_SHORT).show()
            finish()
        }

        observeStates()
    }

    private fun observeStates() {
        lifecycleScope.launch {
            viewModel.initParamsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.tvStatus.text = "Initializing Payment..."
                    }
                    is UiState.Success -> {
                        startRazorpayCheckout(state.data!!)
                    }
                    is UiState.Error -> {
                        isProcessing = false
                        binding.tvStatus.text = "Initialization Failed"
                        binding.tvSubStatus.text = state.message
                        Toast.makeText(this@Payment, state.message, Toast.LENGTH_LONG).show()

                        if (state.message.contains("BAD_REQUEST")) {
                            finish()
                        }
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.verifyState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.tvStatus.text = "Verifying Payment..."
                        binding.tvSubStatus.text = "Confirming with bank"
                    }
                    is UiState.Success -> {
                        binding.tvStatus.text = "Payment Confirmed!"

                        val intent = Intent(this@Payment, BookingSuccess::class.java)

                        // FIX 3: Pass the ID here!
                        intent.putExtra("BOOKING_ID", bookingId)

                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    is UiState.Error -> {
                        binding.tvStatus.text = "Verification Failed"
                        binding.tvSubStatus.text = "Please contact support"
                        Toast.makeText(this@Payment, "Verification Failed", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToHotelDetails() {
        val intent = Intent(this, HotelDetails::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun startDotAnimation() {
        stopDotAnimation()
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)

        dots.forEachIndexed { index, dot ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f, 1f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f, 1f)

            val animator = ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha)
            animator.duration = 800
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.startDelay = (index * 150).toLong()

            animator.start()
            dotAnimators.add(animator)
        }
    }

    private fun stopDotAnimation() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()
    }

    override fun onDestroy() {
        stopDotAnimation()
        super.onDestroy()
    }

    private fun startRazorpayCheckout(data: PaymentInitData) {
        razorpayOrderId = data.razorpayOrderId
        val checkout = Checkout()
        checkout.setKeyID(data.razorpayKeyId)
        checkout.setImage(R.mipmap.ic_launcher)

        try {
            val options = JSONObject()
            options.put("name", data.companyName)
            options.put("description", data.description)
            options.put("currency", data.currency)
            options.put("amount", (data.amount * 100).toLong())
            options.put("order_id", data.razorpayOrderId)
            options.put("prefill.email", data.userEmail)

            val primaryColor = getColor(R.color.razorpay)
            val hexColor = String.format("#%06X", (0xFFFFFF and primaryColor))
            options.put("theme.color", hexColor)

            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("Razorpay", "Error starting checkout", e)
            Toast.makeText(this, "Error starting payment: ${e.message}", Toast.LENGTH_SHORT).show()
            isProcessing = false
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        try {
            val paymentId = paymentData?.paymentId ?: ""
            val signature = paymentData?.signature ?: ""
            val orderId = paymentData?.orderId ?: razorpayOrderId

            binding.tvStatus.text = "Processing..."
            viewModel.verifyPayment(orderId, paymentId, signature)
        } catch (e: Exception) {
            Toast.makeText(this, "Payment Data Error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
        navigateToHotelDetails()
    }
}