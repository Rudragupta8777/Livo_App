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
import androidx.recyclerview.widget.RecyclerView
import com.livo.works.ViewModel.ManagerViewModel
import com.livo.works.databinding.ActivityManagerPropertiesBinding
import com.livo.works.screens.adapter.ManagerHotelAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerProperties : AppCompatActivity() {

    private lateinit var binding: ActivityManagerPropertiesBinding
    private val viewModel: ManagerViewModel by viewModels()
    private lateinit var adapter: ManagerHotelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerPropertiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        setupRecyclerView()
        setupListeners()
        observeData()

        // Fetch data initially
        viewModel.fetchMyHotels(forceRefresh = true)
    }

    private fun setupRecyclerView() {
        adapter = ManagerHotelAdapter { hotelId ->
            // FIXED: Uncommented navigation to details screen!
            val intent = Intent(this, ManagerHotelDetails::class.java)
            intent.putExtra("HOTEL_ID", hotelId)
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(this)
        binding.rvHotels.layoutManager = layoutManager
        binding.rvHotels.adapter = adapter

        // Pagination Scroll Listener
        binding.rvHotels.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) { // Scrolling down
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        viewModel.fetchMyHotels(forceRefresh = false)
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchMyHotels(forceRefresh = true)
        }

        binding.fabAddProperty.setOnClickListener {
            // FIXED: Uncommented the Intent and used the correct CreateHotel class
            val intent = Intent(this, CreateHotel::class.java)
            startActivity(intent)
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.hotelsListState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        if (!binding.swipeRefreshLayout.isRefreshing && adapter.itemCount == 0) {
                            binding.swipeRefreshLayout.isRefreshing = true
                        }
                    }
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        val hotels = state.data?.content ?: emptyList()
                        adapter.submitList(hotels)

                        // Handle Empty State
                        if (hotels.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvHotels.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvHotels.visibility = View.VISIBLE
                        }
                    }
                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(this@ManagerProperties, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isPaginating.collect { isPaginating ->
                binding.paginationProgressBar.visibility = if (isPaginating) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when coming back from Create/Edit screens
        viewModel.fetchMyHotels(forceRefresh = true)
    }
}