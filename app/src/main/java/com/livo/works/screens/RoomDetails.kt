package com.livo.works.screens

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.livo.works.Hotel.data.Room
import com.livo.works.R
import com.livo.works.ViewModel.BookingViewModel
import com.livo.works.databinding.ActivityRoomDetailsBinding
import com.livo.works.screens.adapter.HotelAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class RoomDetails : AppCompatActivity() {

    private lateinit var binding: ActivityRoomDetailsBinding
    private val bookingViewModel: BookingViewModel by viewModels()
    private val imageAdapter = HotelAdapter.HotelImageAdapter()
    private val dotAnimators = mutableListOf<ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.shimmerRoomDetails.startShimmer()
        binding.shimmerRoomDetails.visibility = View.VISIBLE
        binding.mainContentScroll.visibility = View.INVISIBLE

        val room = intent.getParcelableExtra<Room>("ROOM_DATA")
        val startDate = intent.getStringExtra("START_DATE") ?: ""
        val endDate = intent.getStringExtra("END_DATE") ?: ""
        val roomsCount = intent.getIntExtra("ROOM_COUNT", 1)

        if (room != null) {
            setupUI(room)
            setupClickListeners(room, startDate, endDate, roomsCount)

            Handler(Looper.getMainLooper()).postDelayed({
                revealContent()
            }, 500)

            // PASS 'room' TO THE FUNCTION
            observeBookingState(room)
        } else {
            Toast.makeText(this, "Error loading room details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun revealContent() {
        binding.shimmerRoomDetails.stopShimmer()
        binding.shimmerRoomDetails.visibility = View.GONE

        binding.mainContentScroll.visibility = View.VISIBLE
        binding.mainContentScroll.alpha = 0f
        binding.mainContentScroll.translationY = 50f
        binding.mainContentScroll.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .start()
    }

    private fun setupUI(room: Room) {
        binding.vpRoomImages.adapter = imageAdapter
        if (room.photos.isNotEmpty()) {
            imageAdapter.submitList(room.photos)
            binding.tvImageCount.text = "1/${room.photos.size}"
        } else {
            binding.tvImageCount.visibility = View.GONE
        }

        binding.vpRoomImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (room.photos.isNotEmpty()) {
                    binding.tvImageCount.text = "${position + 1}/${room.photos.size}"
                }
            }
        })

        binding.tvRoomType.text = room.type.lowercase().replaceFirstChar { it.uppercase() }
        binding.chipCapacity.text = "${room.capacity} Guests"

        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        binding.tvBottomPrice.text = format.format(room.pricePerDay).replace(".00", "")

        binding.cgRoomAmenities.removeAllViews()
        room.amenities.forEach { amenity ->
            val chip = Chip(this).apply {
                text = amenity
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = ColorStateList.valueOf(getColor(R.color.white))
                chipStrokeWidth = 2f
                chipStrokeColor = ColorStateList.valueOf(getColor(R.color.text_input_stroke))
                setTextColor(ColorStateList.valueOf(getColor(R.color.black)))
            }
            binding.cgRoomAmenities.addView(chip)
        }

        binding.tvDescription.text = room.description ?: "Experience comfort and luxury in our spacious room featuring modern decor, a king-sized bed, and a stunning city view. Perfect for couples or business travelers."
    }

    private fun setupClickListeners(room: Room, startDate: String, endDate: String, roomsCount: Int) {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnBookNow.setOnClickListener {
            bookingViewModel.initiateBooking(room.id, startDate, endDate, roomsCount)
        }
    }

    // ACCEPT ROOM AS PARAMETER
    private fun observeBookingState(room: Room) {
        lifecycleScope.launch {
            bookingViewModel.bookingState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showCustomLoading() // Show your custom dots
                        binding.btnBookNow.isEnabled = false
                    }
                    is UiState.Success -> {
                        hideCustomLoading()
                        binding.btnBookNow.isEnabled = true

                        // FIX NULL SAFETY using '!!'
                        val bookingData = state.data!!

                        val intent = Intent(this@RoomDetails, AddGuest::class.java)
                        intent.putExtra("BOOKING_ID", bookingData.id)
                        intent.putExtra("ROOM_COUNT", bookingData.roomsCount)
                        // Room is now available here
                        intent.putExtra("CAPACITY_PER_ROOM", room.capacity)
                        startActivity(intent)
                        finish()

                        bookingViewModel.resetBookingState()
                    }
                    is UiState.Error -> {
                        hideCustomLoading()
                        binding.btnBookNow.isEnabled = true
                        bookingViewModel.resetBookingState()

                        if (state.message == "ALREADY_INITIATED") {
                            Toast.makeText(
                                this@RoomDetails,
                                "Booking pending for this room. Redirecting...",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(this@RoomDetails, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showCustomLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.loadingOverlay.alpha = 0f
        binding.loadingOverlay.animate().alpha(1f).setDuration(300).start()

        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dotAnimators.clear()

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

    private fun hideCustomLoading() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()

        binding.loadingOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.loadingOverlay.visibility = View.GONE
            }
            .start()
    }
}