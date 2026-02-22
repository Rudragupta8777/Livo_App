package com.livo.works.screens.fragments

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.livo.works.R
import com.livo.works.ViewModel.SearchViewModel
import com.livo.works.databinding.FragmentSearchBinding
import com.livo.works.screens.HotelDetails
import com.livo.works.screens.adapter.HotelAdapter
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var hotelAdapter: HotelAdapter
    private val uiDateFormat = SimpleDateFormat("EEE, MMM dd", Locale.US)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var selectedStartDate: String = ""
    private var selectedEndDate: String = ""

    private var roomCount = 1
    private var adultCount = 2
    private val dotAnimators = mutableListOf<ObjectAnimator>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvGuestDisplay.text = "1 Room"

        initDefaultDates()
        setupRecyclerView()
        setupClickListeners()
        observeSearch()
        observePagination() // NEW
    }

    private fun initDefaultDates() {
        val calendar = Calendar.getInstance()
        val start = calendar.time

        // 1. Store Start Date
        selectedStartDate = apiDateFormat.format(start)
        binding.tvCheckInDate.text = uiDateFormat.format(start)

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val uiEnd = calendar.time
        binding.tvCheckOutDate.text = uiDateFormat.format(uiEnd)

        calendar.add(Calendar.DAY_OF_MONTH, -1)
        selectedEndDate = apiDateFormat.format(calendar.time)
    }

    private fun setupRecyclerView() {
        hotelAdapter = HotelAdapter { hotelId ->
            val intent = Intent(context, HotelDetails::class.java).apply {
                putExtra("HOTEL_ID", hotelId)
                putExtra("START_DATE", selectedStartDate)
                putExtra("END_DATE", selectedEndDate)
                putExtra("ROOMS", roomCount)
            }
            startActivity(intent)
        }

        val layoutManager = LinearLayoutManager(context)
        binding.rvHotels.layoutManager = layoutManager
        binding.rvHotels.adapter = hotelAdapter
        binding.rvHotels.setHasFixedSize(true)

        // SCROLL LISTENER
        binding.rvHotels.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                        && firstVisibleItemPosition >= 0) {
                        viewModel.loadNextPage()
                    }
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.cvDateRange.setOnClickListener {
            showDateRangePicker()
        }

        binding.layoutGuests.setOnClickListener {
            showGuestSelectorDialog()
        }

        binding.btnSearch.setOnClickListener {
            val city = binding.etCity.text.toString().trim()
            if (city.isNotEmpty()) {
                hideKeyboard()
                toggleSearchCard(expanded = false)

                viewModel.searchHotels(city, selectedStartDate, selectedEndDate, roomCount)
            } else {
                Toast.makeText(context, "Please enter a city", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etCity.setOnClickListener {
            if (binding.expandableLayout.visibility == View.GONE) {
                toggleSearchCard(expanded = true)
            }
        }

        binding.etCity.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.expandableLayout.visibility == View.GONE) {
                toggleSearchCard(expanded = true)
            }
        }
    }

    private fun toggleSearchCard(expanded: Boolean) {
        val transition = AutoTransition()
        transition.duration = 300

        TransitionManager.beginDelayedTransition(binding.appBarLayout, transition)

        if (expanded) {
            binding.expandableLayout.visibility = View.VISIBLE
        } else {
            binding.expandableLayout.visibility = View.GONE
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showGuestSelectorDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_guest_selector)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)

        val tvRoomCount = dialog.findViewById<TextView>(R.id.tvRoomCount)
        val btnMinusRoom = dialog.findViewById<ImageView>(R.id.btnMinusRoom)
        val btnPlusRoom = dialog.findViewById<ImageView>(R.id.btnPlusRoom)
        val btnApply = dialog.findViewById<MaterialButton>(R.id.btnDoneGuests)

        var tempRooms = roomCount

        tvRoomCount.text = tempRooms.toString()

        btnPlusRoom.setOnClickListener {
            if (tempRooms < 10) {
                tempRooms++
                tvRoomCount.text = tempRooms.toString()
            }
        }
        btnMinusRoom.setOnClickListener {
            if (tempRooms > 1) {
                tempRooms--
                tvRoomCount.text = tempRooms.toString()
            }
        }

        btnApply.setOnClickListener {
            roomCount = tempRooms
            adultCount = tempRooms * 2

            val roomText = if (roomCount == 1) "1 Room" else "$roomCount Rooms"
            binding.tvGuestDisplay.text = roomText

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDateRangePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.from(MaterialDatePicker.todayInUtcMilliseconds()))

        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Dates")
            .setCalendarConstraints(constraintsBuilder.build())
            .setTheme(R.style.MaterialCalendar)
            .setSelection(
                Pair(MaterialDatePicker.todayInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds() + 86400000)
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = Date(selection.first)
            val endDate = Date(selection.second)

            selectedStartDate = apiDateFormat.format(startDate)
            binding.tvCheckInDate.text = uiDateFormat.format(startDate)

            binding.tvCheckOutDate.text = uiDateFormat.format(endDate)

            val cal = Calendar.getInstance()
            cal.time = endDate
            cal.add(Calendar.DAY_OF_MONTH, -1)
            selectedEndDate = apiDateFormat.format(cal.time)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun observePagination() {
        lifecycleScope.launch {
            viewModel.isPaginating.collect { isPaginating ->
                binding.paginationProgressBar.visibility = if (isPaginating) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeSearch() {
        lifecycleScope.launch {
            viewModel.searchState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        showLoading()
                        binding.rvHotels.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        hideLoading()
                        binding.rvHotels.visibility = View.VISIBLE
                        hotelAdapter.submitList(state.data?.content ?: emptyList())
                        toggleSearchCard(expanded = false)
                    }
                    is UiState.Error -> {
                        hideLoading()
                        if (hotelAdapter.itemCount == 0) {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.loadingOverlay.alpha = 0f
        binding.loadingOverlay.animate().alpha(1f).setDuration(300).start()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.MIRROR)
            binding.rvHotels.setRenderEffect(blurEffect)
        }

        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dotAnimators.clear()

        dots.forEachIndexed { index, dot ->
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f, 1f)
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f, 1f)

            val animator = ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha)
            animator.duration = 800
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.startDelay = (index * 150).toLong()

            animator.start()
            dotAnimators.add(animator)
        }
    }

    private fun hideLoading() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.rvHotels.setRenderEffect(null)
        }

        binding.loadingOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.loadingOverlay.visibility = View.GONE
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}