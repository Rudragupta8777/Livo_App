package com.livo.works.screens.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.livo.works.ViewModel.BookingViewModel
import com.livo.works.databinding.FragmentBookingBinding
import com.livo.works.screens.BookingDetails
import com.livo.works.screens.adapter.BookingAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookingFragment : Fragment() {

    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingViewModel by viewModels()
    private lateinit var adapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchMyBookings(forceRefresh = true)
        }

        observeData()
    }

    override fun onResume() {
        super.onResume()
        // LOGIC CHANGE: Fetch every time the screen opens/resumes to trigger refresh
        viewModel.fetchMyBookings(forceRefresh = true)
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter { bookingId ->
            val intent = Intent(requireContext(), BookingDetails::class.java)
            intent.putExtra("BOOKING_ID", bookingId)
            startActivity(intent)
        }

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@BookingFragment.adapter
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.myBookingsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        // LOGIC CHANGE: Always show shimmer when loading (unless swipe refreshing)
                        // Removed 'adapter.currentList.isEmpty()' check to force shimmer every time
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            binding.shimmerView.visibility = View.VISIBLE
                            binding.shimmerView.startShimmer()

                            // Hide the list to make it look like a fresh reload
                            binding.rvBookings.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }
                    }
                    is UiState.Success -> {
                        binding.shimmerView.stopShimmer()
                        binding.shimmerView.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false

                        val bookings = state.data ?: emptyList()

                        if (bookings.isEmpty()) {
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.rvBookings.visibility = View.GONE
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                            binding.rvBookings.visibility = View.VISIBLE
                            adapter.submitList(bookings)
                        }
                    }
                    is UiState.Error -> {
                        binding.shimmerView.stopShimmer()
                        binding.shimmerView.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}