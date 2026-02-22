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
import com.livo.works.ViewModel.AdminViewModel
import com.livo.works.databinding.ActivityAdminRequestsBinding
import com.livo.works.screens.adapter.RoleRequestAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminRequests : AppCompatActivity() {

    private lateinit var binding: ActivityAdminRequestsBinding
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var adapter: RoleRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        setupRecyclerView()
        observeData()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchRequests(forceRefresh = true)
        }
    }

    private fun setupRecyclerView() {
        adapter = RoleRequestAdapter(
            onApprove = { id -> viewModel.processRequest(id, true) },
            onReject = { id -> viewModel.processRequest(id, false) }
        )
        val layoutManager = LinearLayoutManager(this)
        binding.rvRequests.layoutManager = layoutManager
        binding.rvRequests.adapter = adapter

        binding.rvRequests.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        viewModel.fetchRequests(forceRefresh = false)
                    }
                }
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.requestsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        // Show your custom dots animation
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            showLoading()
                        }
                    }
                    is UiState.Success -> {
                        hideLoading()
                        binding.swipeRefreshLayout.isRefreshing = false
                        adapter.submitList(state.data?.content ?: emptyList())
                    }
                    is UiState.Error -> {
                        hideLoading()
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(this@AdminRequests, state.message, Toast.LENGTH_SHORT).show()
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

        lifecycleScope.launch {
            viewModel.processState.collect { state ->
                if (state is UiState.Success) {
                    Toast.makeText(this@AdminRequests, "Request processed successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetProcessState()
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        // Add your dot animation logic here (copy from SearchFragment)
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }
}