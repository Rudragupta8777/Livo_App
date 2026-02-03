package com.livo.works.screens

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
import com.livo.works.Booking.data.GuestDto
import com.livo.works.R
import com.livo.works.ViewModel.BookingViewModel
import com.livo.works.databinding.ActivityAddGuestBinding
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddGuest : AppCompatActivity() {

    private lateinit var binding: ActivityAddGuestBinding
    private val bookingViewModel: BookingViewModel by viewModels()

    private var bookingId: Long = -1
    private var maxCapacity: Int = 2
    private var currentGuestCount = 0
    private val dotAnimators = mutableListOf<ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        bookingId = intent.getLongExtra("BOOKING_ID", -1)
        val roomCount = intent.getIntExtra("ROOM_COUNT", 1)
        val capacityPerRoom = intent.getIntExtra("CAPACITY_PER_ROOM", 2)

        maxCapacity = roomCount * capacityPerRoom

        setupUI()
        setupCapacityDialog()
        addNewGuestRow()
        observeViewModel() // Start observing state
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.maxCapacity.text = "Add up to $maxCapacity Guests"

        // Enable smooth animations for adding/removing rows
        binding.containerGuests.layoutTransition = LayoutTransition()

        binding.btnAddAnother.setOnClickListener {
            if (currentGuestCount < maxCapacity) {
                addNewGuestRow()
            } else {
                showCapacityLimitDialog()
            }
        }

        binding.btnContinue.setOnClickListener {
            collectDataAndSubmit()
        }
    }

    private fun setupCapacityDialog() {
        binding.tvCapacityMessage.text = "You can add maximum $maxCapacity guest${if (maxCapacity > 1) "s" else ""} for this booking."
        binding.btnCloseCapacityDialog.setOnClickListener { hideCapacityLimitDialog() }
        binding.capacityLimitOverlay.setOnClickListener { hideCapacityLimitDialog() }
    }

    private fun showCapacityLimitDialog() {
        binding.capacityLimitOverlay.visibility = View.VISIBLE
        binding.capacityLimitOverlay.alpha = 0f
        binding.capacityLimitOverlay.animate()
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        val card = (binding.capacityLimitOverlay.getChildAt(0) as? View)
        card?.apply {
            scaleX = 0.8f; scaleY = 0.8f; alpha = 0f
            animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
        }
    }

    private fun hideCapacityLimitDialog() {
        val card = (binding.capacityLimitOverlay.getChildAt(0) as? View)
        card?.animate()?.scaleX(0.8f)?.scaleY(0.8f)?.alpha(0f)?.setDuration(200)?.start()

        binding.capacityLimitOverlay.animate()
            .alpha(0f).setDuration(250)
            .withEndAction { binding.capacityLimitOverlay.visibility = View.GONE }
            .start()
    }

    private fun addNewGuestRow() {
        currentGuestCount++
        val guestView = LayoutInflater.from(this).inflate(R.layout.layout_guest_item, binding.containerGuests, false)

        val tvTitle = guestView.findViewById<TextView>(R.id.tvGuestTitle)
        tvTitle.text = "Guest $currentGuestCount"

        val btnRemove = guestView.findViewById<ImageView>(R.id.btnRemove)
        if (currentGuestCount > 1) {
            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener { removeGuestRow(guestView) }
        } else {
            btnRemove.visibility = View.GONE
        }

        binding.containerGuests.addView(guestView)
        binding.mainScrollView.post { binding.mainScrollView.fullScroll(View.FOCUS_DOWN) }
        updateAddButtonState()
    }

    private fun removeGuestRow(view: View) {
        binding.containerGuests.removeView(view)
        currentGuestCount--
        renumberGuests()
        updateAddButtonState()
    }

    private fun updateAddButtonState() {
        if (currentGuestCount >= maxCapacity) {
            binding.btnAddAnother.alpha = 0.5f
            binding.btnAddAnother.isEnabled = true
        } else {
            binding.btnAddAnother.alpha = 1f
            binding.btnAddAnother.isEnabled = true
        }
    }

    private fun renumberGuests() {
        for (i in 0 until binding.containerGuests.childCount) {
            val child = binding.containerGuests.getChildAt(i)
            val tvTitle = child.findViewById<TextView>(R.id.tvGuestTitle)
            tvTitle.text = "Guest ${i + 1}"
        }
    }

    private fun collectDataAndSubmit() {
        val guestList = ArrayList<GuestDto>()
        var isValid = true

        for (i in 0 until binding.containerGuests.childCount) {
            val child = binding.containerGuests.getChildAt(i)
            val etName = child.findViewById<EditText>(R.id.etGuestName)
            val etAge = child.findViewById<EditText>(R.id.etGuestAge)
            val cgGender = child.findViewById<ChipGroup>(R.id.cgGender)

            val name = etName.text.toString().trim()
            val ageStr = etAge.text.toString().trim()

            if (name.isEmpty()) { etName.error = "Name required"; isValid = false }
            if (ageStr.isEmpty()) { etAge.error = "Age required"; isValid = false }

            val gender = when (cgGender.checkedChipId) {
                R.id.chipMale -> "MALE"
                R.id.chipFemale -> "FEMALE"
                else -> "OTHER"
            }

            if (isValid) {
                // IMPORTANT: We use null for userId/id here as they are created by backend
                guestList.add(GuestDto(name = name, age = ageStr.toIntOrNull() ?: 18, gender = gender))
            }
        }

        if (isValid) {
            // Call API to add guests
            bookingViewModel.addGuests(bookingId, guestList)
        } else {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            bookingViewModel.addGuestState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showCustomLoading()
                        binding.btnContinue.isEnabled = false
                    }
                    is UiState.Success -> {
                        hideCustomLoading()
                        binding.btnContinue.isEnabled = true

                        // Navigate to BookingReview with the API response data
                        val intent = Intent(this@AddGuest, BookingReview::class.java)
                        intent.putExtra("BOOKING_DATA", state.data)
                        startActivity(intent)
                        // Do not finish() here if you want them to be able to go back and edit guests
                    }
                    is UiState.Error -> {
                        hideCustomLoading()
                        binding.btnContinue.isEnabled = true
                        bookingViewModel.resetAddGuestState()

                        if (state.message == "BAD_REQUEST") {
                            Toast.makeText(this@AddGuest, "Session Expired. Please restart booking.", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@AddGuest, HotelDetails::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@AddGuest, state.message, Toast.LENGTH_SHORT).show()
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

    override fun onBackPressed() {
        if (binding.capacityLimitOverlay.visibility == View.VISIBLE) {
            hideCapacityLimitDialog()
        } else {
            super.onBackPressed()
        }
    }
}