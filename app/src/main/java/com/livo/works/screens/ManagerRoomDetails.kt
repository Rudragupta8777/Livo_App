package com.livo.works.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.livo.works.R
import com.livo.works.Room.data.RoomDto
import com.livo.works.ViewModel.RoomViewModel
import com.livo.works.databinding.ActivityManagerRoomDetailsBinding
import com.livo.works.screens.adapter.HotelAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerRoomDetails : AppCompatActivity() {

    private lateinit var binding: ActivityManagerRoomDetailsBinding
    private val viewModel: RoomViewModel by viewModels()
    private val imageAdapter = HotelAdapter.HotelImageAdapter {}

    private var roomId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerRoomDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        roomId = intent.getLongExtra("ROOM_ID", -1L)
        if (roomId == -1L) {
            Toast.makeText(this, "Invalid Room ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewPager()

        binding.btnBack.setOnClickListener { finish() }

        observeData()
        viewModel.fetchRoomById(roomId)
    }

    private fun setupViewPager() {
        binding.vpRoomImages.adapter = imageAdapter
        binding.vpRoomImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val total = imageAdapter.itemCount
                if (total > 0) {
                    binding.tvImageCount.text = "${position + 1}/$total"
                }
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.roomDetailsState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        state.data?.let { setupUI(it) }
                    }
                    is UiState.Error -> {
                        Toast.makeText(this@ManagerRoomDetails, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {} // Loading state could be handled here if needed
                }
            }
        }
    }

    private fun setupUI(room: RoomDto) {
        binding.tvRoomType.text = room.type
        binding.tvCapacity.text = "${room.capacity} Guests"

        if (room.active && !room.deleted) {
            binding.tvStatus.text = "Active"
            binding.tvStatus.setTextColor(getColor(R.color.text_primary))
        } else {
            binding.tvStatus.text = "Inactive"
            binding.tvStatus.setTextColor(android.graphics.Color.RED)
        }

        if (room.photos.isNotEmpty()) {
            imageAdapter.submitList(room.photos)
            binding.cvImageCounter.visibility = View.VISIBLE
            binding.tvImageCount.text = "1/${room.photos.size}"
        } else {
            binding.cvImageCounter.visibility = View.GONE
        }

        binding.cgAmenities.removeAllViews()
        room.amenities.forEach { amenity ->
            val chip = Chip(this).apply {
                text = amenity
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = getColorStateList(R.color.white)
                chipStrokeWidth = 2f
                chipStrokeColor = getColorStateList(R.color.text_input_stroke)
                setTextColor(getColorStateList(R.color.black))
            }
            binding.cgAmenities.addView(chip)
        }
    }
}