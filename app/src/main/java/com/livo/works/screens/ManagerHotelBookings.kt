package com.livo.works.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.livo.works.Manager.adapter.ManagerBookingAdapter
import com.livo.works.ViewModel.ManagerViewModel
import com.livo.works.databinding.ActivityManagerHotelBookingsBinding
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class ManagerHotelBookings : AppCompatActivity() {

    private lateinit var binding: ActivityManagerHotelBookingsBinding
    private val viewModel: ManagerViewModel by viewModels()
    private lateinit var adapter: ManagerBookingAdapter

    private var hotelId: Long = -1L
    private var fromDateFilter: String? = null
    private var toDateFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerHotelBookingsBinding.inflate(layoutInflater)
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

        // Fetch initially
        fetchData(forceRefresh = true)
    }

    private fun setupRecyclerView() {
        adapter = ManagerBookingAdapter()

        val layoutManager = LinearLayoutManager(this)
        binding.rvBookings.layoutManager = layoutManager
        binding.rvBookings.adapter = adapter

        // Pagination
        binding.rvBookings.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        fetchData(forceRefresh = false)
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchData(forceRefresh = true)
        }

        binding.btnDateFilter.setOnClickListener {
            showDateRangePicker()
        }

        binding.fabClearFilter.setOnClickListener {
            fromDateFilter = null
            toDateFilter = null
            binding.tvDateRange.text = "All Time"
            binding.fabClearFilter.visibility = View.GONE
            fetchData(forceRefresh = true)
        }
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Booking Dates")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second

            // MaterialDatePicker returns UTC timestamps. We format them to the ISO string required by API.
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")

            fromDateFilter = sdf.format(Date(startDate))
            toDateFilter = sdf.format(Date(endDate))

            // Update UI
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            displayFormat.timeZone = TimeZone.getTimeZone("UTC")
            binding.tvDateRange.text = "${displayFormat.format(Date(startDate))} - ${displayFormat.format(Date(endDate))}"

            binding.fabClearFilter.visibility = View.VISIBLE

            // Fetch filtered data
            fetchData(forceRefresh = true)
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun fetchData(forceRefresh: Boolean) {
        viewModel.fetchHotelBookings(hotelId, fromDateFilter, toDateFilter, forceRefresh)
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.hotelBookingsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        if (!binding.swipeRefreshLayout.isRefreshing && adapter.itemCount == 0) {
                            binding.swipeRefreshLayout.isRefreshing = true
                        }
                    }
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        val bookings = state.data?.content ?: emptyList()
                        adapter.submitList(bookings)

                        if (bookings.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvBookings.visibility = View.GONE

                            // Adjust empty text based on filter
                            if (fromDateFilter != null) {
                                binding.tvEmptyStateDesc.text = "No bookings found for the selected date range."
                            } else {
                                binding.tvEmptyStateDesc.text = "There are no bookings for this property yet."
                            }
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvBookings.visibility = View.VISIBLE
                        }
                    }
                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(this@ManagerHotelBookings, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isBookingsPaginating.collect { isPaginating ->
                binding.paginationProgressBar.visibility = if (isPaginating) View.VISIBLE else View.GONE
            }
        }
    }
}