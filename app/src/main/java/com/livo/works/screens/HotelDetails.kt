package com.livo.works.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2 // IMPORT THIS
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.livo.works.Hotel.data.HotelSummary
import com.livo.works.R
import com.livo.works.ViewModel.HotelViewModel
import com.livo.works.databinding.ActivityHotelDetailsBinding
import com.livo.works.screens.adapter.HotelAdapter
import com.livo.works.screens.adapter.RoomAdapter
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
class HotelDetails : AppCompatActivity() {

    private lateinit var binding: ActivityHotelDetailsBinding
    private val viewModel: HotelViewModel by viewModels()
    private lateinit var roomAdapter: RoomAdapter
    private val headerImageAdapter = HotelAdapter.HotelImageAdapter()

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0
    private var currentHotelName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotelDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        setupHeaderViewPager() // Setup Header
        setupRoomRecyclerView()

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

        val hotelId = intent.getLongExtra("HOTEL_ID", -1)
        val start = intent.getStringExtra("START_DATE") ?: ""
        val end = intent.getStringExtra("END_DATE") ?: ""
        val rooms = intent.getIntExtra("ROOMS", 1)

        if (hotelId != -1L) {
            viewModel.getHotelDetails(hotelId, start, end, rooms)
        }

        observeDetails()

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupHeaderViewPager() {
        binding.vpHeaderImages.adapter = headerImageAdapter

        // Listen for Swipes to update 1/5 counter
        binding.vpHeaderImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val total = headerImageAdapter.itemCount
                if (total > 0) {
                    binding.tvHeaderImageCount.text = "${position + 1}/$total"
                }
            }
        })
    }

    private fun setupRoomRecyclerView() {
        roomAdapter = RoomAdapter { room ->
            // Pass Data to RoomDetailsActivity
            val intent = Intent(this, RoomDetails::class.java).apply {
                putExtra("ROOM_DATA", room) // Parcelable Object
                // We need to retrieve start/end/rooms from the original intent that opened HotelDetails
                putExtra("START_DATE", intent.getStringExtra("START_DATE"))
                putExtra("END_DATE", intent.getStringExtra("END_DATE"))
                putExtra("ROOM_COUNT", intent.getIntExtra("ROOMS", 1))
            }
            startActivity(intent)
        }

        binding.rvRooms.apply {
            layoutManager = LinearLayoutManager(this@HotelDetails)
            adapter = roomAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeDetails() {
        binding.shimmerDetails.startShimmer()
        binding.shimmerDetails.visibility = View.VISIBLE
        binding.mainContentScroll.visibility = View.INVISIBLE

        lifecycleScope.launch {
            viewModel.detailsState.collect { state ->
                when (state) {
                    is UiState.Loading -> { }
                    is UiState.Success -> {
                        val data = state.data!!
                        updateUI(data.hotel)
                        roomAdapter.submitList(data.rooms)

                        // Animation Reveal
                        binding.shimmerDetails.stopShimmer()
                        binding.shimmerDetails.visibility = View.GONE

                        binding.mainContentScroll.visibility = View.VISIBLE
                        binding.mainContentScroll.alpha = 0f
                        binding.mainContentScroll.translationY = 100f
                        binding.mainContentScroll.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(500)
                            .setInterpolator(android.view.animation.DecelerateInterpolator())
                            .start()
                    }
                    is UiState.Error -> {
                        binding.shimmerDetails.stopShimmer()
                        binding.shimmerDetails.visibility = View.GONE
                        Toast.makeText(this@HotelDetails, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateUI(hotel: HotelSummary) {
        binding.tvHotelName.text = hotel.name
        binding.tvCity.text = hotel.city
        binding.tvFullAddress.text = hotel.contactInfo.address
        binding.tvPhone.text = hotel.contactInfo.phoneNumber
        binding.tvEmail.text = hotel.contactInfo.email

        currentHotelName = hotel.name

        // UPDATE HEADER IMAGES
        if (hotel.photos.isNotEmpty()) {
            headerImageAdapter.submitList(hotel.photos)
            binding.tvHeaderImageCount.text = "1/${hotel.photos.size}"
            binding.tvHeaderImageCount.visibility = View.VISIBLE
        } else {
            binding.tvHeaderImageCount.visibility = View.GONE
        }

        binding.cgAmenities.removeAllViews()
        hotel.amenities.forEach { amenity ->
            val chip = Chip(this).apply {
                text = amenity
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = getColorStateList(R.color.white)
                chipStrokeWidth = 2f
                chipStrokeColor = getColorStateList(R.color.text_input_stroke)
                // Fix text color for dark mode
                setTextColor(getColorStateList(R.color.black))
            }
            binding.cgAmenities.addView(chip)
        }

        setupMapbox(hotel)
    }

    private fun setupMapbox(hotel: HotelSummary) {
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
}