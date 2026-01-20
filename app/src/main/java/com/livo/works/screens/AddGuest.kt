package com.livo.works.screens

import android.animation.LayoutTransition
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
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
    private var maxCapacity: Int = 2 // Default fallback
    private var currentGuestCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Get Data from Previous Screen
        bookingId = intent.getLongExtra("BOOKING_ID", -1)
        val roomCount = intent.getIntExtra("ROOM_COUNT", 1)
        val capacityPerRoom = intent.getIntExtra("CAPACITY_PER_ROOM", 2)

        // Calculate Max Capacity
        maxCapacity = roomCount * capacityPerRoom

        setupUI()
        observeViewModel()
        setupCapacityDialog()

        // Add first guest automatically
        addNewGuestRow()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.maxCapacity.text = "Add up to $maxCapacity Guests"

        // Enable smooth animations for adding/removing rows
        binding.containerGuests.layoutTransition = LayoutTransition()

        // ADD BUTTON CLICK
        binding.btnAddAnother.setOnClickListener {
            if (currentGuestCount < maxCapacity) {
                addNewGuestRow()
            } else {
                // Show custom popup dialog
                showCapacityLimitDialog()
            }
        }

        // CONFIRM BUTTON CLICK
        binding.btnContinue.setOnClickListener {
            collectDataAndSubmit()
        }
    }

    private fun setupCapacityDialog() {
        // Set the capacity message dynamically
        binding.tvCapacityMessage.text = "You can add maximum $maxCapacity guest${if (maxCapacity > 1) "s" else ""} for this booking."

        // Close button click
        binding.btnCloseCapacityDialog.setOnClickListener {
            hideCapacityLimitDialog()
        }

        // Click outside to close
        binding.capacityLimitOverlay.setOnClickListener {
            hideCapacityLimitDialog()
        }
    }

    private fun showCapacityLimitDialog() {
        binding.capacityLimitOverlay.visibility = View.VISIBLE
        binding.capacityLimitOverlay.alpha = 0f
        binding.capacityLimitOverlay.animate()
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Scale animation for the card
        val card = (binding.capacityLimitOverlay.getChildAt(0) as? View)
        card?.apply {
            scaleX = 0.8f
            scaleY = 0.8f
            alpha = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun hideCapacityLimitDialog() {
        val card = (binding.capacityLimitOverlay.getChildAt(0) as? View)

        card?.animate()
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.alpha(0f)
            ?.setDuration(200)
            ?.start()

        binding.capacityLimitOverlay.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                binding.capacityLimitOverlay.visibility = View.GONE
            }
            .start()
    }

    private fun addNewGuestRow() {
        currentGuestCount++

        // Inflate the row
        val guestView = LayoutInflater.from(this).inflate(R.layout.layout_guest_item, binding.containerGuests, false)

        // Set Title (Guest 1, Guest 2...)
        val tvTitle = guestView.findViewById<TextView>(R.id.tvGuestTitle)
        tvTitle.text = "Guest $currentGuestCount"

        // Handle Remove Button
        val btnRemove = guestView.findViewById<ImageView>(R.id.btnRemove)
        if (currentGuestCount > 1) {
            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener {
                removeGuestRow(guestView)
            }
        } else {
            // First guest cannot be removed
            btnRemove.visibility = View.GONE
        }

        // Add to Container
        binding.containerGuests.addView(guestView)

        // Scroll to bottom to show new entry
        binding.mainScrollView.post {
            binding.mainScrollView.fullScroll(View.FOCUS_DOWN)
        }

        // Update "Add Another" button state
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
            binding.btnAddAnother.isEnabled = true // Keep enabled to show popup
        } else {
            binding.btnAddAnother.alpha = 1f
            binding.btnAddAnother.isEnabled = true
        }
    }

    private fun renumberGuests() {
        // Renumber "Guest 1", "Guest 2" titles after deletion
        for (i in 0 until binding.containerGuests.childCount) {
            val child = binding.containerGuests.getChildAt(i)
            val tvTitle = child.findViewById<TextView>(R.id.tvGuestTitle)
            tvTitle.text = "Guest ${i + 1}"
        }
    }

    private fun collectDataAndSubmit() {
        val guestList = mutableListOf<GuestDto>()
        var isValid = true

        // Loop through all rows
        for (i in 0 until binding.containerGuests.childCount) {
            val child = binding.containerGuests.getChildAt(i)

            val etName = child.findViewById<EditText>(R.id.etGuestName)
            val etAge = child.findViewById<EditText>(R.id.etGuestAge)
            val cgGender = child.findViewById<ChipGroup>(R.id.cgGender)

            val name = etName.text.toString().trim()
            val ageStr = etAge.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Name required"
                isValid = false
            }
            if (ageStr.isEmpty()) {
                etAge.error = "Age required"
                isValid = false
            }

            val gender = when (cgGender.checkedChipId) {
                R.id.chipMale -> "MALE"
                R.id.chipFemale -> "FEMALE"
                else -> "OTHER"
            }

            if (isValid) {
                guestList.add(GuestDto(name, ageStr.toIntOrNull() ?: 18, gender))
            }
        }

        if (isValid) {
            // Call API
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
                        binding.loadingOverlay.visibility = View.VISIBLE
                        binding.btnContinue.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.loadingOverlay.visibility = View.GONE
                        binding.btnContinue.isEnabled = true

                        // Navigate to Payment (Placeholder)
                        Toast.makeText(this@AddGuest, "Guests Added! Proceeding to payment...", Toast.LENGTH_SHORT).show()
                        // val intent = Intent(this@AddGuestActivity, PaymentActivity::class.java)
                        // startActivity(intent)
                    }
                    is UiState.Error -> {
                        binding.loadingOverlay.visibility = View.GONE
                        binding.btnContinue.isEnabled = true
                        bookingViewModel.resetAddGuestState()

                        if (state.message == "BAD_REQUEST") {
                            // *** ERROR REQUIREMENT: Redirect to Hotel Detail ***
                            Toast.makeText(this@AddGuest, "Session Expired. Please restart booking.", Toast.LENGTH_LONG).show()

                            val intent = Intent(this@AddGuest, HotelDetails::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            // You might want to pass the HOTEL_ID again if you have it
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

    override fun onBackPressed() {
        if (binding.capacityLimitOverlay.visibility == View.VISIBLE) {
            hideCapacityLimitDialog()
        } else {
            super.onBackPressed()
        }
    }
}