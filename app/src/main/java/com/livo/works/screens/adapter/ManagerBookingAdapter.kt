package com.livo.works.Manager.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.livo.works.Manager.data.HotelBookingDto
import com.livo.works.databinding.ItemManagerBookingBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ManagerBookingAdapter : ListAdapter<HotelBookingDto, ManagerBookingAdapter.BookingViewHolder>(DiffCallback) {
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemManagerBookingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(private val binding: ItemManagerBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: HotelBookingDto) {
            binding.apply {
                tvBookingId.text = "Booking #${booking.bookingId}"
                tvRoomType.text = booking.roomType
                tvRoomCount.text = "${booking.roomsCount} Room(s) Booked"

                // Format Dates (e.g., "2026-03-01" -> "01 Mar")
                try {
                    val start = inputFormat.parse(booking.startDate)
                    val end = inputFormat.parse(booking.endDate)
                    if (start != null && end != null) {
                        tvStartDate.text = outputFormat.format(start)
                        tvEndDate.text = outputFormat.format(end)
                    }
                } catch (e: Exception) {
                    // Fallback to raw string if parsing fails
                    tvStartDate.text = booking.startDate
                    tvEndDate.text = booking.endDate
                }

                // Dynamic Status Colors
                when (booking.bookingStatus) {
                    "CONFIRMED" -> {
                        tvStatusText.text = "CONFIRMED"
                        tvStatusText.setTextColor(Color.parseColor("#1B5E20"))
                        cvStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                    }
                    "CANCELLED" -> {
                        tvStatusText.text = "CANCELLED"
                        tvStatusText.setTextColor(Color.parseColor("#C62828"))
                        cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    }
                    else -> {
                        tvStatusText.text = booking.bookingStatus
                        tvStatusText.setTextColor(Color.parseColor("#E65100"))
                        cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                    }
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HotelBookingDto>() {
        override fun areItemsTheSame(oldItem: HotelBookingDto, newItem: HotelBookingDto) =
            oldItem.bookingId == newItem.bookingId
        override fun areContentsTheSame(oldItem: HotelBookingDto, newItem: HotelBookingDto) =
            oldItem == newItem
    }
}