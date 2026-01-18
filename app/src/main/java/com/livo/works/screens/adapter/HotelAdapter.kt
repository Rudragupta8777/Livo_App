package com.livo.works.screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.livo.works.Hotel.data.HotelSummary
import com.livo.works.R
import com.livo.works.databinding.ItemHotelBinding
import java.text.NumberFormat
import java.util.Locale

class HotelAdapter(
    private val onHotelClick: (Long) -> Unit
) : ListAdapter<HotelSummary, HotelAdapter.HotelViewHolder>(HotelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val binding = ItemHotelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HotelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HotelImageAdapter : ListAdapter<String, HotelImageAdapter.ImageViewHolder>(ImageDiffCallback()) {
        inner class ImageHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            // Re-using the simple ImageView Layout
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hotel_image, parent, false)
            return ImageViewHolder(view as ImageView)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val url = getItem(position)
            Glide.with(holder.itemView.context)
                .load(url)
                .centerCrop()
                .placeholder(R.color.text_input_stroke)
                .into(holder.imageView)
        }

        class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }

    inner class HotelViewHolder(private val binding: ItemHotelBinding) : RecyclerView.ViewHolder(binding.root) {

        private val imageAdapter = HotelImageAdapter()

        init {
            binding.vpHotelImages.adapter = imageAdapter

            // Listen for swipes to update "1/X" text
            binding.vpHotelImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val total = imageAdapter.itemCount
                    if (total > 0) {
                        binding.tvImageCount.text = "${position + 1}/$total"
                    }
                }
            })
        }

        fun bind(hotel: HotelSummary) {
            binding.apply {
                tvHotelName.text = hotel.name
                tvCity.text = hotel.city

                val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                tvPrice.text = format.format(hotel.pricePerDay).replace(".00", "")

                tvAmenitiesPreview.text = hotel.amenities.take(3).joinToString(" • ")

                // Handle Image List
                val photos = if (hotel.photos.isNotEmpty()) hotel.photos else emptyList()
                imageAdapter.submitList(photos)

                // Initial Counter Set
                val total = photos.size
                if (total > 0) {
                    tvImageCount.text = "1/$total"
                    tvImageCount.visibility = android.view.View.VISIBLE
                } else {
                    tvImageCount.visibility = android.view.View.GONE
                }

                root.setOnClickListener { onHotelClick(hotel.id) }
            }
        }
    }

    class HotelDiffCallback : DiffUtil.ItemCallback<HotelSummary>() {
        override fun areItemsTheSame(oldItem: HotelSummary, newItem: HotelSummary) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HotelSummary, newItem: HotelSummary) = oldItem == newItem
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}