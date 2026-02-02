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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.ChipGroup
import com.livo.works.Booking.data.GuestDto
import com.livo.works.R
import com.livo.works.databinding.ActivityAddGuestBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddGuest : AppCompatActivity() {

    private lateinit var binding: ActivityAddGuestBinding
    // Note: BookingViewModel removed, logic moved to Payment screen

    private var bookingId: Long = -1
    private var maxCapacity: Int = 2
    private var currentGuestCount = 0

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
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.maxCapacity.text = "Add up to $maxCapacity Guests"
        binding.containerGuests.layoutTransition = LayoutTransition()

        binding.btnAddAnother.setOnClickListener {
            if (currentGuestCount < maxCapacity) {
                addNewGuestRow()
            } else {
                showCapacityLimitDialog()
            }
        }

        binding.btnContinue.setOnClickListener {
            binding.btnContinue.isEnabled = false
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
        // CHANGED: Use ArrayList for Intent passing
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
                guestList.add(GuestDto(name, ageStr.toIntOrNull() ?: 18, gender))
            }
        }

        if (isValid) {
            val intent = Intent(this, Payment::class.java)
            intent.putExtra("BOOKING_ID", bookingId)
            intent.putParcelableArrayListExtra("GUEST_LIST", guestList) // Pass the list
            startActivity(intent)
            // No finish() if you want back navigation support
        } else {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
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