package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.livo.works.Booking.data.BookingData
import com.livo.works.databinding.ActivityBookingReviewBinding
import com.livo.works.databinding.DialogGuestListBinding
import com.livo.works.databinding.ItemGuestSummaryBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class BookingReview : AppCompatActivity() {

    private lateinit var binding: ActivityBookingReviewBinding
    private var bookingData: BookingData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingData = intent.getParcelableExtra("BOOKING_DATA")

        if (bookingData == null) {
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        val data = bookingData!!

        // Fill Data
        binding.tvHotelName.text = data.hotelName
        binding.tvCity.text = data.hotelCity
        binding.tvRoomType.text = data.roomType.uppercase()

        // --- DATES LOGIC ---
        binding.tvCheckInDate.text = formatDate(data.startDate)
        // Apply the +1 Day logic here for Check-out
        binding.tvCheckOutDate.text = formatDatePlusOne(data.endDate)

        // Guest Names Summary (e.g. "John Doe + 1")
        val guestCount = data.guests.size
        val firstGuest = data.guests.firstOrNull()?.name ?: "Guest"
        val remaining = guestCount - 1
        binding.tvGuestNames.text = if (remaining > 0) "$firstGuest +$remaining" else firstGuest

        // Price
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val formattedPrice = format.format(data.amount)
        binding.tvTotalAmount.text = formattedPrice
        binding.tvBasePrice.text = formattedPrice

        // CLICK LISTENER FOR GUEST POPUP
        binding.layoutGuests.setOnClickListener {
            showGuestDetailsDialog()
        }

        binding.btnPayNow.setOnClickListener {
            val intent = Intent(this, Payment::class.java)
            intent.putExtra("BOOKING_ID", data.id)
            intent.putExtra("USER_EMAIL", "user@example.com")
            startActivity(intent)
        }
    }

    private fun showGuestDetailsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(com.livo.works.R.layout.dialog_guest_list, null)
        val dialogBinding = com.livo.works.databinding.DialogGuestListBinding.bind(dialogView)

        val dialog = MaterialAlertDialogBuilder(this, com.livo.works.R.style.RoundedDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Populate List
        bookingData?.guests?.forEach { guest ->
            val itemBinding = com.livo.works.databinding.ItemGuestSummaryBinding.inflate(layoutInflater, dialogBinding.containerGuestList, false)
            itemBinding.tvName.text = guest.name

            // Format Gender (e.g., Male)
            val gender = guest.gender.lowercase().replaceFirstChar { it.uppercase() }
            itemBinding.tvDetails.text = "$gender, ${guest.age} years"

            dialogBinding.containerGuestList.addView(itemBinding.root)
        }

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun formatDate(dateStr: String): String {
        try {
            val input = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = java.text.SimpleDateFormat("dd MMM", Locale.US)
            val date = input.parse(dateStr)
            return output.format(date!!)
        } catch (e: Exception) {
            return dateStr
        }
    }

    private fun formatDatePlusOne(dateStr: String): String {
        try {
            val input = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = java.text.SimpleDateFormat("dd MMM", Locale.US)
            val date = input.parse(dateStr)

            // Add 1 Day
            val cal = Calendar.getInstance()
            cal.time = date!!
            cal.add(Calendar.DAY_OF_MONTH, 1)

            return output.format(cal.time)
        } catch (e: Exception) {
            return dateStr
        }
    }
}