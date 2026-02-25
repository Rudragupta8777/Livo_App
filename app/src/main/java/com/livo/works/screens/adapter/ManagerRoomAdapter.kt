package com.livo.works.screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livo.works.Room.data.RoomDto
import com.livo.works.databinding.ItemManagerRoomBinding

class ManagerRoomAdapter(
    private val onRoomClick: (Long) -> Unit,
    private val onDeleteClick: (Long) -> Unit
) : ListAdapter<RoomDto, ManagerRoomAdapter.RoomViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemManagerRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoomViewHolder(private val binding: ItemManagerRoomBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(room: RoomDto) {
            binding.apply {
                tvRoomType.text = room.type
                tvCapacity.text = "Up to ${room.capacity} Guests"
                tvAmenities.text = room.amenities.joinToString(" • ")

                if (room.photos.isNotEmpty()) {
                    Glide.with(itemView.context).load(room.photos[0]).into(ivRoomPhoto)
                } else {
                    ivRoomPhoto.setImageDrawable(null)
                }

                // Delete Button Click
                btnDeleteRoom.setOnClickListener {
                    onDeleteClick(room.id)
                }

                // ADDED: Entire Card Click
                root.setOnClickListener {
                    onRoomClick(room.id)
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RoomDto>() {
        override fun areItemsTheSame(oldItem: RoomDto, newItem: RoomDto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RoomDto, newItem: RoomDto) = oldItem == newItem
    }
}