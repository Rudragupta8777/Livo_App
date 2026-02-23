package com.livo.works.screens.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livo.works.Manager.data.ManagerHotelDto
import com.livo.works.databinding.ItemManagerHotelBinding

class ManagerHotelAdapter(
    private val onHotelClick: (Long) -> Unit
) : ListAdapter<ManagerHotelDto, ManagerHotelAdapter.HotelViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val binding = ItemManagerHotelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HotelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HotelViewHolder(private val binding: ItemManagerHotelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(hotel: ManagerHotelDto) {
            binding.apply {
                tvHotelName.text = hotel.name
                tvHotelCity.text = hotel.city

                // Load first photo if available
                if (!hotel.photos.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(hotel.photos[0])
                        .centerCrop()
                        .into(ivHotelThumbnail)
                } else {
                    ivHotelThumbnail.setImageDrawable(null) // Or set a placeholder
                }

                // Status Badge Logic
                if (hotel.active) {
                    tvStatusText.text = "ACTIVE"
                    tvStatusText.setTextColor(Color.parseColor("#1B5E20")) // Dark Green
                    cvStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green
                } else {
                    tvStatusText.text = "PENDING APPROVAL"
                    tvStatusText.setTextColor(Color.parseColor("#E65100")) // Dark Orange
                    cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3E0")) // Light Orange
                }

                root.setOnClickListener {
                    onHotelClick(hotel.id)
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ManagerHotelDto>() {
        override fun areItemsTheSame(oldItem: ManagerHotelDto, newItem: ManagerHotelDto) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ManagerHotelDto, newItem: ManagerHotelDto) =
            oldItem == newItem
    }
}