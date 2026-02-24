package com.livo.works.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.livo.works.Manager.data.ManagerContactInfo
import com.livo.works.Manager.data.ManagerHotelDetailsDto
import com.livo.works.R
import com.livo.works.ViewModel.ManagerViewModel
import com.livo.works.databinding.ActivityUpdateHotelBinding
import com.livo.works.util.CloudinaryHelper
import com.livo.works.util.UiState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpdateHotel : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateHotelBinding
    private val viewModel: ManagerViewModel by viewModels()

    private var hotelId: Long = -1L

    private var existingImageUrls = mutableListOf<String>()
    private val newImageUris = mutableListOf<Uri>()

    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    private var dotAnimators = mutableListOf<android.animation.ObjectAnimator>()

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            newImageUris.addAll(uris)
            updatePhotoUI()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openFullScreenMap()
        else Toast.makeText(this, "Location permission is needed to map the property.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateHotelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        hotelId = intent.getLongExtra("HOTEL_ID", -1L)
        if (hotelId == -1L) {
            Toast.makeText(this, "Invalid Hotel ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Back Navigation Handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.fullScreenMapOverlay.visibility == View.VISIBLE) {
                    binding.fullScreenMapOverlay.visibility = View.GONE
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        setupListeners()
        observeData()

        viewModel.fetchHotelDetails(hotelId)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddPhotos.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // --- MAP LOGIC ---
        binding.btnSelectLocation.setOnClickListener {
            checkLocationPermissionAndStart()
        }

        binding.btnEditLocation.setOnClickListener {
            checkLocationPermissionAndStart()
        }

        binding.btnCloseMap.setOnClickListener {
            binding.fullScreenMapOverlay.visibility = View.GONE
        }

        binding.btnConfirmFullMap.setOnClickListener {
            val centerPoint = binding.fullMapView.getMapboxMap().cameraState.center
            selectedLat = centerPoint.latitude()
            selectedLng = centerPoint.longitude()

            binding.fullScreenMapOverlay.visibility = View.GONE
            binding.layoutLocationSetup.visibility = View.GONE
            binding.layoutLocationSuccess.visibility = View.VISIBLE
        }

        // --- SUBMIT ---
        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.hotelDetailsState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading(true)
                    is UiState.Success -> {
                        showLoading(false)
                        state.data?.let { populateFields(it) }
                    }
                    is UiState.Error -> {
                        showLoading(false)
                        Toast.makeText(this@UpdateHotel, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading(true)
                    is UiState.Success -> {
                        showLoading(false)
                        Toast.makeText(this@UpdateHotel, "Property Updated Successfully!", Toast.LENGTH_LONG).show()
                        viewModel.resetActionState()
                        finish()
                    }
                    is UiState.Error -> {
                        showLoading(false)
                        Toast.makeText(this@UpdateHotel, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun populateFields(hotel: ManagerHotelDetailsDto) {
        binding.etName.setText(hotel.name)
        binding.etCity.setText(hotel.city)
        binding.etAddress.setText(hotel.contactInfo.address)
        binding.etPhone.setText(hotel.contactInfo.phoneNumber)
        binding.etEmail.setText(hotel.contactInfo.email)
        binding.etAmenities.setText(hotel.amenities.joinToString(", "))

        try {
            val locParts = hotel.contactInfo.location.split(",")
            if (locParts.size == 2) {
                selectedLat = locParts[0].trim().toDouble()
                selectedLng = locParts[1].trim().toDouble()
                binding.layoutLocationSetup.visibility = View.GONE
                binding.layoutLocationSuccess.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        existingImageUrls.clear()
        existingImageUrls.addAll(hotel.photos)
        updatePhotoUI()
    }

    private fun checkLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            openFullScreenMap()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openFullScreenMap() {
        binding.fullScreenMapOverlay.visibility = View.VISIBLE

        val mapboxMap = binding.fullMapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)

        // If we already have a location from the hotel data, center the map there!
        if (selectedLat != 0.0 && selectedLng != 0.0) {
            val point = Point.fromLngLat(selectedLng, selectedLat)
            mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(15.0).build())
        } else {
            // Otherwise get user's current location
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                    mapboxMap.setCamera(CameraOptions.Builder().center(userPoint).zoom(15.0).build())
                }
            }
        }
    }

    private fun updatePhotoUI() {
        val childCount = binding.layoutPhotoContainer.childCount
        if (childCount > 1) {
            binding.layoutPhotoContainer.removeViews(1, childCount - 1)
        }

        val dpToPx = resources.displayMetrics.density

        // Render Existing Cloudinary URLs
        existingImageUrls.toList().forEach { url ->
            addDeletableThumbnail(url, dpToPx, isExisting = true)
        }

        // Render New Local URIs
        newImageUris.toList().forEach { uri ->
            addDeletableThumbnail(uri, dpToPx, isExisting = false)
        }
    }

    private fun addDeletableThumbnail(imageSource: Any, dpToPx: Float, isExisting: Boolean) {
        val frameLayout = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.photo_size),
                resources.getDimensionPixelSize(R.dimen.photo_size)
            ).apply { marginEnd = 24 }
        }

        val imageCard = MaterialCardView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            radius = 40f
            strokeWidth = 0
        }

        val imageView = ImageView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        Glide.with(this).load(imageSource).into(imageView)
        imageCard.addView(imageView)
        frameLayout.addView(imageCard)

        // Delete Button Overlay
        val deleteButtonCard = MaterialCardView(this).apply {
            val cardSize = (24 * dpToPx).toInt()
            layoutParams = android.widget.FrameLayout.LayoutParams(cardSize, cardSize).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = (6 * dpToPx).toInt()
                marginEnd = (6 * dpToPx).toInt()
            }
            radius = (12 * dpToPx)
            setCardBackgroundColor(android.graphics.Color.parseColor("#80000000"))
            strokeWidth = 0
            cardElevation = 0f

            val deleteIcon = ImageView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding((6 * dpToPx).toInt(), (6 * dpToPx).toInt(), (6 * dpToPx).toInt(), (6 * dpToPx).toInt())
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            }
            addView(deleteIcon)

            setOnClickListener {
                if (isExisting) {
                    existingImageUrls.remove(imageSource as String)
                } else {
                    newImageUris.remove(imageSource as Uri)
                }
                updatePhotoUI()
            }
        }

        frameLayout.addView(deleteButtonCard)
        binding.layoutPhotoContainer.addView(frameLayout)
    }

    private fun validateAndSubmit() {
        val name = binding.etName.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val amenitiesStr = binding.etAmenities.text.toString().trim()

        if (name.isEmpty() || city.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (phone.length != 10) {
            binding.etPhone.error = "Phone number must be exactly 10 digits"
            return
        } else {
            binding.etPhone.error = null
        }

        if (selectedLat == 0.0 && selectedLng == 0.0) {
            Toast.makeText(this, "Please set the map location.", Toast.LENGTH_SHORT).show()
            return
        }

        if (existingImageUrls.isEmpty() && newImageUris.isEmpty()) {
            Toast.makeText(this, "Please upload at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)

            val finalPhotoUrls = mutableListOf<String>()
            finalPhotoUrls.addAll(existingImageUrls)

            for (uri in newImageUris) {
                val url = CloudinaryHelper.uploadImage(this@UpdateHotel, uri)
                if (url != null) {
                    finalPhotoUrls.add(url)
                } else {
                    Toast.makeText(this@UpdateHotel, "Failed to upload an image", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }
            }

            val amenitiesList = amenitiesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val formattedLocation = "$selectedLat,$selectedLng"

            val updatedHotel = ManagerHotelDetailsDto(
                id = hotelId,
                name = name,
                city = city,
                photos = finalPhotoUrls,
                amenities = amenitiesList,
                contactInfo = ManagerContactInfo(
                    address = address,
                    phoneNumber = phone,
                    email = email,
                    location = formattedLocation
                ),
                active = true
            )

            viewModel.updateHotel(hotelId, updatedHotel)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
        binding.btnAddPhotos.isEnabled = !isLoading

        if (isLoading) startDotAnimation() else stopDotAnimation()
    }

    private fun startDotAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dotAnimators.clear()

        dots.forEachIndexed { index, dot ->
            val scaleX = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f, 1f)
            val scaleY = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f, 1f)
            val alpha = android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f, 1f)

            val animator = android.animation.ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha).apply {
                duration = 800
                repeatCount = android.animation.ObjectAnimator.INFINITE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                startDelay = (index * 150).toLong()
            }
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