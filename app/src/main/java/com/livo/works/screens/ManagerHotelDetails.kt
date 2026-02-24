package com.livo.works.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.livo.works.Manager.data.ManagerHotelDetailsDto
import com.livo.works.R
import com.livo.works.ViewModel.ManagerViewModel
import com.livo.works.databinding.ActivityManagerHotelDetailsBinding
import com.livo.works.screens.adapter.HotelAdapter
import com.livo.works.util.UiState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerHotelDetails : AppCompatActivity() {

    private lateinit var binding: ActivityManagerHotelDetailsBinding
    private val viewModel: ManagerViewModel by viewModels()

    // ViewPager Adapter from your existing HotelAdapter
    private val headerImageAdapter = HotelAdapter.HotelImageAdapter { }

    private var hotelId: Long = -1L
    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0
    private var currentHotelName: String = ""

    // For the loading animation
    private var dotAnimators = mutableListOf<android.animation.ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerHotelDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        hotelId = intent.getLongExtra("HOTEL_ID", -1L)

        if (hotelId == -1L) {
            Toast.makeText(this, "Invalid Hotel ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupHeaderViewPager()
        setupListeners()
        observeData()

        // Fetch Data
        viewModel.fetchHotelDetails(hotelId)
    }

    private fun setupHeaderViewPager() {
        binding.vpHeaderImages.adapter = headerImageAdapter

        binding.vpHeaderImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val total = headerImageAdapter.itemCount
                if (total > 0) {
                    binding.tvHeaderImageCount.text = "${position + 1}/$total"
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Map Click Listener
        binding.mapClickOverlay.setOnClickListener {
            if (currentLat != 0.0 && currentLng != 0.0) {
                val intent = Intent(this, FullMap::class.java).apply {
                    putExtra("LAT", currentLat)
                    putExtra("LNG", currentLng)
                    putExtra("NAME", currentHotelName)
                }
                startActivity(intent)
            }
        }

        // Manager Action Listeners
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnActivate.setOnClickListener {
            binding.textViewFindingHotels.text = "Activating Property..."
            viewModel.activateHotel(hotelId)
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, UpdateHotel::class.java)
            intent.putExtra("HOTEL_ID", hotelId)
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_confirmation)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Find buttons inside the custom layout
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmDelete)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            binding.textViewFindingHotels.text = "Deleting Property..."
            viewModel.deleteHotel(hotelId)
        }

        dialog.show()
    }

    private fun observeData() {
        // Observe Hotel Details
        lifecycleScope.launch {
            viewModel.hotelDetailsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.textViewFindingHotels.text = "Loading Details..."
                        showLoading(true)
                    }
                    is UiState.Success -> {
                        showLoading(false)
                        state.data?.let { setupUI(it) }
                    }
                    is UiState.Error -> {
                        showLoading(false)
                        Toast.makeText(this@ManagerHotelDetails, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        // Observe Action Responses (Activate / Delete)
        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading(true)
                    is UiState.Success -> {
                        showLoading(false)
                        Toast.makeText(this@ManagerHotelDetails, "Action completed successfully!", Toast.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                        finish() // Go back to the list
                    }
                    is UiState.Error -> {
                        showLoading(false)
                        Toast.makeText(this@ManagerHotelDetails, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupUI(hotel: ManagerHotelDetailsDto) {
        binding.apply {
            tvHotelName.text = hotel.name
            tvHotelCity.text = hotel.city
            currentHotelName = hotel.name

            // ViewPager Header Images
            if (hotel.photos.isNotEmpty()) {
                headerImageAdapter.submitList(hotel.photos)
                tvHeaderImageCount.text = "1/${hotel.photos.size}"
                cvImageCounter.visibility = View.VISIBLE
            } else {
                cvImageCounter.visibility = View.GONE
            }

            // Status logic
            if (hotel.active) {
                tvStatusText.text = "ACTIVE"
                tvStatusText.setTextColor(Color.parseColor("#1B5E20"))
                cvStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                btnActivate.visibility = View.GONE
            } else {
                tvStatusText.text = "PENDING APPROVAL"
                tvStatusText.setTextColor(Color.parseColor("#E65100"))
                cvStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                btnActivate.visibility = View.VISIBLE
            }

            // Contact Info
            tvAddress.text = hotel.contactInfo.address
            tvPhone.text = hotel.contactInfo.phoneNumber
            tvEmail.text = hotel.contactInfo.email

            // Amenities ChipGroup
            cgAmenities.removeAllViews()
            hotel.amenities.forEach { amenity ->
                val chip = Chip(this@ManagerHotelDetails).apply {
                    text = amenity
                    isCheckable = false
                    setEnsureMinTouchTargetSize(false)
                    chipBackgroundColor = getColorStateList(R.color.white)
                    chipStrokeWidth = 2f
                    chipStrokeColor = getColorStateList(R.color.text_input_stroke)
                    setTextColor(getColorStateList(R.color.black))
                }
                cgAmenities.addView(chip)
            }

            // Setup Map
            setupMapbox(hotel)
        }
    }

    private fun setupMapbox(hotel: ManagerHotelDetailsDto) {
        try {
            val locParts = hotel.contactInfo.location.split(",")
            if (locParts.size == 2) {
                currentLat = locParts[0].trim().toDouble()
                currentLng = locParts[1].trim().toDouble()

                val point = Point.fromLngLat(currentLng, currentLat)
                val mapboxMap = binding.mapView.getMapboxMap()

                val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val styleUri = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    Style.DARK
                } else {
                    Style.MAPBOX_STREETS
                }

                mapboxMap.loadStyleUri(styleUri) {
                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(14.5)
                            .build()
                    )
                    addAnnotationToMap(point)

                    binding.mapView.gestures.scrollEnabled = false
                    binding.mapView.gestures.pitchEnabled = false
                    binding.mapView.gestures.rotateEnabled = false
                    binding.mapView.gestures.doubleTapToZoomInEnabled = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addAnnotationToMap(point: Point) {
        bitmapFromDrawableRes(this, R.drawable.ic_map)?.let { bitmap ->
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.5)
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int): Bitmap? {
        var drawable = AppCompatResources.getDrawable(context, resourceId) ?: return null
        drawable = DrawableCompat.wrap(drawable).mutate()
        DrawableCompat.setTint(drawable, context.getColor(R.color.text_primary))

        if (drawable is BitmapDrawable) return drawable.bitmap

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnActivate.isEnabled = !isLoading
        binding.btnDelete.isEnabled = !isLoading
        binding.btnEdit.isEnabled = !isLoading

        if (isLoading) {
            startDotAnimation()
        } else {
            stopDotAnimation()
        }
    }

    private fun startDotAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dotAnimators.clear()

        dots.forEachIndexed { index, dot ->
            val scaleX = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f, 1f)
            val scaleY = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f, 1f)
            val alpha = android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f, 1f)

            val animator = android.animation.ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha)
            animator.duration = 800
            animator.repeatCount = android.animation.ObjectAnimator.INFINITE
            animator.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            animator.startDelay = (index * 150).toLong()

            animator.start()
            dotAnimators.add(animator)
        }
    }

    private fun stopDotAnimation() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDotAnimation()
    }
}