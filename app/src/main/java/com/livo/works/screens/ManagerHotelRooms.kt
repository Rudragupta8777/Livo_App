package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.livo.works.R
import com.livo.works.ViewModel.RoomViewModel
import com.livo.works.databinding.ActivityManagerHotelRoomsBinding
import com.livo.works.screens.adapter.ManagerRoomAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerHotelRooms : AppCompatActivity() {

    private lateinit var binding: ActivityManagerHotelRoomsBinding
    private val viewModel: RoomViewModel by viewModels()
    private lateinit var adapter: ManagerRoomAdapter
    private var hotelId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerHotelRoomsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        hotelId = intent.getLongExtra("HOTEL_ID", -1L)
        if (hotelId == -1L) {
            Toast.makeText(this, "Invalid Hotel ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchRoomsForHotel(hotelId)
    }

    private fun setupRecyclerView() {
        adapter = ManagerRoomAdapter(
            onRoomClick = { roomId ->
                // Navigate to the Room Details screen
                val intent = Intent(this, ManagerRoomDetails::class.java)
                intent.putExtra("ROOM_ID", roomId)
                startActivity(intent)
            },
            onDeleteClick = { roomId ->
                showDeleteConfirmation(roomId)
            }
        )
        binding.rvRooms.layoutManager = LinearLayoutManager(this)
        binding.rvRooms.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchRoomsForHotel(hotelId)
        }

        binding.fabAddRoom.setOnClickListener {
            val intent = Intent(this, ManagerCreateRoom::class.java)
            intent.putExtra("HOTEL_ID", hotelId)
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmation(roomId: Long) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_room)

        // Make the dialog background transparent so our custom rounded card shows beautifully
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmDelete)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            // Triggers the deletion and shows the loader/refresh automatically via ViewModel
            viewModel.deleteRoom(roomId, hotelId)
        }

        dialog.show()
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.roomsListState.collect { state ->
                when (state) {
                    is UiState.Loading -> binding.swipeRefreshLayout.isRefreshing = true
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        val rooms = state.data ?: emptyList()
                        adapter.submitList(rooms)

                        if (rooms.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvRooms.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvRooms.visibility = View.VISIBLE
                        }
                    }
                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(this@ManagerHotelRooms, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                if (state is UiState.Error) {
                    Toast.makeText(this@ManagerHotelRooms, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                } else if (state is UiState.Success) {
                    Toast.makeText(this@ManagerHotelRooms, "Room Deleted Successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
            }
        }
    }
}