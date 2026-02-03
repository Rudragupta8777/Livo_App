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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.livo.works.R
import com.livo.works.ViewModel.HomeViewModel
import com.livo.works.databinding.FragmentHomeBinding
import com.livo.works.screens.HotelDetails
import com.livo.works.screens.adapter.BestHotelAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: BestHotelAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etCity.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)

            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.nav_search
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, SearchFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchBestHotels(forceRefresh = true)
        }

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = BestHotelAdapter { hotelId ->
            val calendar = Calendar.getInstance()
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val start = apiFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val end = apiFormat.format(calendar.time)

            val intent = Intent(context, HotelDetails::class.java)
            intent.putExtra("HOTEL_ID", hotelId)
            intent.putExtra("START_DATE", start)
            intent.putExtra("END_DATE", end)
            intent.putExtra("ROOMS", 1)
            startActivity(intent)
        }

        binding.rvBestHotels.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.bestHotelsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            binding.shimmerView.visibility = View.VISIBLE
                            binding.rvBestHotels.visibility = View.GONE
                        }
                    }
                    is UiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.shimmerView.visibility = View.GONE
                        binding.rvBestHotels.visibility = View.VISIBLE
                        adapter.submitList(state.data)
                    }
                    is UiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.shimmerView.visibility = View.GONE
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