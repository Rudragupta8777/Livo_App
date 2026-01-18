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
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.livo.works.R
import com.livo.works.ViewModel.HotelViewModel
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
    private val viewModel: HotelViewModel by viewModels()
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

        initDefaultDates()
        setupRecyclerView()
        setupClickListeners()
        observeSearch()
    }

    private fun initDefaultDates() {
        val calendar = Calendar.getInstance()
        val start = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.time

        selectedStartDate = apiDateFormat.format(start)
        selectedEndDate = apiDateFormat.format(end)

        binding.tvCheckInDate.text = uiDateFormat.format(start)
        binding.tvCheckOutDate.text = uiDateFormat.format(end)
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

        binding.rvHotels.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hotelAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        // Date Picker
        binding.cvDateRange.setOnClickListener {
            showDateRangePicker()
        }

        // Guest Popup
        binding.layoutGuests.setOnClickListener {
            showGuestSelectorDialog()
        }

        binding.btnSearch.setOnClickListener {
            val city = binding.etCity.text.toString().trim()
            if (city.isNotEmpty()) {
                hideKeyboard()
                toggleSearchCard(expanded = false)

                val adjustedEndDate = calculateAdjustedEndDate(selectedEndDate)

                viewModel.searchHotels(city, selectedStartDate, adjustedEndDate, roomCount)
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

    private fun calculateAdjustedEndDate(originalEndDate: String): String {
        return try {
            val date = apiDateFormat.parse(originalEndDate)

            val calendar = Calendar.getInstance()
            if (date != null) {
                calendar.time = date
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                apiDateFormat.format(calendar.time)
            } else {
                originalEndDate
            }
        } catch (e: Exception) {
            e.printStackTrace()
            originalEndDate
        }
    }
    private fun toggleSearchCard(expanded: Boolean) {
        val transition = AutoTransition()
        transition.duration = 300 // 300ms animation

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
        val tvAdultCount = dialog.findViewById<TextView>(R.id.tvAdultCount)
        val btnMinusRoom = dialog.findViewById<ImageView>(R.id.btnMinusRoom)
        val btnPlusRoom = dialog.findViewById<ImageView>(R.id.btnPlusRoom)
        val btnMinusAdult = dialog.findViewById<ImageView>(R.id.btnMinusAdult)
        val btnPlusAdult = dialog.findViewById<ImageView>(R.id.btnPlusAdult)
        val btnApply = dialog.findViewById<MaterialButton>(R.id.btnDoneGuests)

        var tempRooms = roomCount
        var tempAdults = adultCount

        tvRoomCount.text = tempRooms.toString()
        tvAdultCount.text = tempAdults.toString()

        btnPlusRoom.setOnClickListener {
            if (tempRooms < 10) {
                tempRooms++
                tempAdults += 2
                tvRoomCount.text = tempRooms.toString()
                tvAdultCount.text = tempAdults.toString()
            }
        }
        btnMinusRoom.setOnClickListener {
            if (tempRooms > 1) {
                tempRooms--
                if (tempAdults > (tempRooms * 2)) {
                    tempAdults = tempRooms * 2
                }
                tvRoomCount.text = tempRooms.toString()
                tvAdultCount.text = tempAdults.toString()
            }
        }

        btnPlusAdult.setOnClickListener {
            if (tempAdults < 30) {
                tempAdults++
                tvAdultCount.text = tempAdults.toString()
            }
        }
        btnMinusAdult.setOnClickListener {
            if (tempAdults > 1) {
                tempAdults--
                tvAdultCount.text = tempAdults.toString()
            }
        }

        btnApply.setOnClickListener {
            roomCount = tempRooms
            adultCount = tempAdults
            binding.tvGuestDisplay.text = "$roomCount Room, $adultCount Adults"
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDateRangePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())

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
            selectedEndDate = apiDateFormat.format(endDate)

            binding.tvCheckInDate.text = uiDateFormat.format(startDate)
            binding.tvCheckOutDate.text = uiDateFormat.format(endDate)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
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
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
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
            // Scale Up and Down
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f, 1f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f, 1f)
            // Fade slightly
            val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f, 1f)

            val animator = ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha)
            animator.duration = 800 // Speed of one wave
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.interpolator = AccelerateDecelerateInterpolator()

            // Stagger the start times to create the "Wave" effect
            animator.startDelay = (index * 150).toLong()

            animator.start()
            dotAnimators.add(animator)
        }
    }

    private fun hideLoading() {
        // 1. Stop Animations
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.rvHotels.setRenderEffect(null)
        }

        // 3. Fade out overlay smoothly
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