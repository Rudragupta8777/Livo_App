package com.livo.works.screens.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.livo.works.Booking.data.BookingSummaryDto
import com.livo.works.R
import com.livo.works.databinding.ItemBookingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookingAdapter(
    private val onBookingClick: (Long) -> Unit
) : ListAdapter<BookingSummaryDto, BookingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemBookingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BookingSummaryDto) {
            binding.tvHotelName.text = item.hotelName
            binding.tvRoute.text = item.hotelCity

            try {
                val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US) // Input
                val dayMonthFormat = SimpleDateFormat("d MMM, yyyy", Locale.US) // Output
                val uiDayFormat = SimpleDateFormat("dd", Locale.US)
                val uiMonthFormat = SimpleDateFormat("MMM", Locale.US)

                val startDate = apiFormat.parse(item.startDate)
                val originalEnd = apiFormat.parse(item.endDate)

                if (startDate != null && originalEnd != null) {
                    val cal = Calendar.getInstance()
                    cal.time = originalEnd
                    cal.add(Calendar.DAY_OF_YEAR, 1) // Add 1 Day
                    val adjustedEnd = cal.time

                    // 3. Set Left Side Date (Big "27 OCT")
                    binding.tvDay.text = uiDayFormat.format(startDate)
                    binding.tvMonth.text = uiMonthFormat.format(startDate).uppercase()

                    val startStr = dayMonthFormat.format(startDate)
                    val endStr = dayMonthFormat.format(adjustedEnd) // Uses +1 date

                    binding.tvDateRange.text = "$startStr  →  $endStr"
                }
            } catch (e: Exception) {
                // Fallback
                binding.tvDay.text = "--"
                binding.tvMonth.text = "---"
                binding.tvDateRange.text = "${item.startDate} -> ${item.endDate}"
            }

            // Status Logic
            binding.tvStatusBadge.visibility = View.VISIBLE
            when (item.bookingStatus) {
                "CONFIRMED" -> {
                    binding.tvStatusBadge.text = "CONFIRMED"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed)
                }
                "CANCELLED" -> {
                    binding.tvStatusBadge.text = "CANCELLED"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_failed)
                }
                "PAYMENT_FAILED" -> {
                    binding.tvStatusBadge.text = "PAYMENT FAILED"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_failed)
                }
                else -> {
                    binding.tvStatusBadge.text = item.bookingStatus
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_failed)
                }
            }

            // Click Listener
            binding.root.setOnClickListener {
                onBookingClick(item.bookingId)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookingSummaryDto>() {
        override fun areItemsTheSame(oldItem: BookingSummaryDto, newItem: BookingSummaryDto) = oldItem.bookingId == newItem.bookingId
        override fun areContentsTheSame(oldItem: BookingSummaryDto, newItem: BookingSummaryDto) = oldItem == newItem
    }
}