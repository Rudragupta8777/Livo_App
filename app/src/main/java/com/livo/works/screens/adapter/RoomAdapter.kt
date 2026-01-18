package com.livo.works.screens.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livo.works.Hotel.data.Room
import com.livo.works.R
import com.livo.works.databinding.ItemRoomBinding
import java.text.NumberFormat
import java.util.Locale

class RoomAdapter(
    private val onRoomSelect: (Room) -> Unit
) : ListAdapter<Room, RoomAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoomViewHolder(private val binding: ItemRoomBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(room: Room) {
            binding.apply {
                tvRoomType.text = room.type.lowercase().replaceFirstChar { it.uppercase() }
                tvRoomDetails.text = "${room.capacity} Guests • ${room.amenities.firstOrNull() ?: "Standard"}"

                val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                tvRoomPrice.text = format.format(room.pricePerDay).replace(".00", "")

                if (room.photos.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(room.photos[0])
                        .centerCrop()
                        .into(ivRoomImage)
                }

                if (!room.available) {
                    btnBook.text = "Sold Out"
                    btnBook.isEnabled = false
                    btnBook.alpha = 0.5f
                } else {
                    btnBook.text = "Select"
                    btnBook.isEnabled = true
                    btnBook.alpha = 1f
                    btnBook.setOnClickListener { onRoomSelect(room) }
                }
            }
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<Room>() {
        override fun areItemsTheSame(oldItem: Room, newItem: Room) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Room, newItem: Room) = oldItem == newItem
    }
}