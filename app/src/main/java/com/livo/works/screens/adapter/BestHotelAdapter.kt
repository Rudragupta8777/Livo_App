package com.livo.works.screens.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.livo.works.R
import com.livo.works.Search.data.BestHotelDto
import com.livo.works.databinding.ItemBestHotelBinding

class BestHotelAdapter(
    private val onHotelClick: (Long) -> Unit
) : ListAdapter<BestHotelDto, BestHotelAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBestHotelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // --- Inner Image Adapter for the Carousel ---
    class InnerImageAdapter(private val images: List<String>) : RecyclerView.Adapter<InnerImageAdapter.ImgViewHolder>() {
        inner class ImgViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImgViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hotel_image, parent, false)
            return ImgViewHolder(view as ImageView)
        }

        override fun onBindViewHolder(holder: ImgViewHolder, position: Int) {
            Glide.with(holder.itemView)
                .load(images[position])
                .centerCrop()
                .placeholder(R.color.text_input_stroke)
                .into(holder.imageView)
        }

        override fun getItemCount() = images.size
    }

    inner class ViewHolder(private val binding: ItemBestHotelBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BestHotelDto) {
            binding.tvHotelName.text = item.name
            binding.tvCity.text = item.city

            // Setup Image Carousel
            if (item.photos.isNotEmpty()) {
                val imgAdapter = InnerImageAdapter(item.photos)
                binding.vpBestHotelImages.adapter = imgAdapter
                binding.tvImageCount.text = "1/${item.photos.size}"

                // Update counter on swipe
                binding.vpBestHotelImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        binding.tvImageCount.text = "${position + 1}/${item.photos.size}"
                    }
                })
            }

            // Click Listener
            binding.root.setOnClickListener { onHotelClick(item.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BestHotelDto>() {
        override fun areItemsTheSame(oldItem: BestHotelDto, newItem: BestHotelDto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BestHotelDto, newItem: BestHotelDto) = oldItem == newItem
    }
}