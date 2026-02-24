package com.livo.works.adapter

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.livo.works.Manager.data.ManagerHotelDto
import com.livo.works.databinding.ItemManagerHotelBinding
import com.livo.works.screens.adapter.HotelAdapter

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

    override fun onViewRecycled(holder: HotelViewHolder) {
        super.onViewRecycled(holder)
        holder.stopBlinking()
    }

    inner class HotelViewHolder(private val binding: ItemManagerHotelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val imageAdapter = HotelAdapter.HotelImageAdapter {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onHotelClick(getItem(position).id)
            }
        }

        private var dotAnimator: ObjectAnimator? = null

        init {
            binding.vpHotelImages.adapter = imageAdapter

            binding.vpHotelImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val total = imageAdapter.itemCount
                    if (total > 0) {
                        binding.tvImageCount.text = "${position + 1}/$total"
                    }
                }
            })

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onHotelClick(getItem(position).id)
                }
            }
        }

        fun bind(hotel: ManagerHotelDto) {
            binding.apply {
                tvHotelName.text = hotel.name
                tvCity.text = hotel.city

                if (!hotel.photos.isNullOrEmpty()) {
                    imageAdapter.submitList(hotel.photos)
                    tvImageCount.text = "1/${hotel.photos.size}"
                    tvImageCount.visibility = View.VISIBLE
                } else {
                    imageAdapter.submitList(emptyList())
                    tvImageCount.visibility = View.GONE
                }

                stopBlinking()

                if (hotel.active) {
                    tvStatusText.text = "ACTIVE"
                    tvStatusText.setTextColor(Color.parseColor("#1B5E20")) // Dark Green
                    cvStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green
                    indicatorDot.setCardBackgroundColor(Color.parseColor("#4CAF50"))

                    startBlinking()
                } else {
                    tvStatusText.text = "INACTIVE"
                    tvStatusText.setTextColor(Color.parseColor("#E65100")) // Dark Orange
                    cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3E0")) // Light Orange
                    indicatorDot.setCardBackgroundColor(Color.parseColor("#FF9800"))
                }
            }
        }

        private fun startBlinking() {
            dotAnimator = ObjectAnimator.ofFloat(binding.indicatorDot, View.ALPHA, 1f, 0.2f).apply {
                duration = 800
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }

        fun stopBlinking() {
            dotAnimator?.cancel()
            dotAnimator = null
            binding.indicatorDot.alpha = 1f // Ensure it's fully visible when static
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ManagerHotelDto>() {
        override fun areItemsTheSame(oldItem: ManagerHotelDto, newItem: ManagerHotelDto) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ManagerHotelDto, newItem: ManagerHotelDto) =
            oldItem == newItem
    }
}