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
import com.livo.works.databinding.ActivityCreateHotelBinding
import com.livo.works.util.CloudinaryHelper
import com.livo.works.util.UiState
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateHotel : AppCompatActivity() {

    private lateinit var binding: ActivityCreateHotelBinding
    private val viewModel: ManagerViewModel by viewModels()

    private val selectedImageUris = mutableListOf<Uri>()
    private var dotAnimators = mutableListOf<android.animation.ObjectAnimator>()

    // Hidden Coordinates
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    // Photo Picker
    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updatePhotoUI()
        }
    }

    // Location Permission Request
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            activateMapPicker()
        } else {
            Toast.makeText(this, "Location permission is needed to map the property.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateHotelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        setupListeners()
        observeData()
    }

    private fun setupListeners() {
        binding.btnAddPhotos.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // --- MAP LOGIC ---
        binding.btnSelectLocation.setOnClickListener {
            checkLocationPermissionAndStart()
        }

        binding.btnEditLocation.setOnClickListener {
            binding.layoutLocationSuccess.visibility = View.GONE
            binding.layoutMapContainer.visibility = View.VISIBLE
        }

        binding.btnConfirmLocation.setOnClickListener {
            // Get center coordinate from map where the crosshair is
            val centerPoint = binding.mapPickerView.getMapboxMap().cameraState.center
            selectedLat = centerPoint.latitude()
            selectedLng = centerPoint.longitude()

            // Hide Map, Show Success
            binding.layoutMapContainer.visibility = View.GONE
            binding.layoutLocationSetup.visibility = View.GONE
            binding.layoutLocationSuccess.visibility = View.VISIBLE
        }
        // -----------------

        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun checkLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            activateMapPicker()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun activateMapPicker() {
        // UI Changes
        binding.layoutLocationSetup.visibility = View.GONE
        binding.layoutMapContainer.visibility = View.VISIBLE

        val mapboxMap = binding.mapPickerView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)

        // Fetch User's Current Location to center the map
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Explicitly defining the type 'Location?' fixes the "Cannot infer type" error
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val userPoint = Point.fromLngLat(location.longitude, location.latitude)
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(userPoint)
                        .zoom(15.0)
                        .build()
                )
            } else {
                // Fallback (e.g., Center of India or previous location)
                val fallbackPoint = Point.fromLngLat(78.9629, 20.5937)
                mapboxMap.setCamera(CameraOptions.Builder().center(fallbackPoint).zoom(4.0).build())
                Toast.makeText(this, "Searching for location...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePhotoUI() {
        val childCount = binding.layoutPhotoContainer.childCount
        if (childCount > 1) {
            binding.layoutPhotoContainer.removeViews(1, childCount - 1)
        }

        selectedImageUris.forEach { uri ->
            val cardView = MaterialCardView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.photo_size),
                    resources.getDimensionPixelSize(R.dimen.photo_size)
                ).apply { marginEnd = 24 }
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

            Glide.with(this).load(uri).into(imageView)
            cardView.addView(imageView)
            binding.layoutPhotoContainer.addView(cardView)
        }
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

        // Enforce exactly 10 digits
        if (phone.length != 10) {
            binding.etPhone.error = "Phone number must be exactly 10 digits"
            return
        } else {
            binding.etPhone.error = null
        }

        // Validate Coordinates are set
        if (selectedLat == 0.0 && selectedLng == 0.0) {
            Toast.makeText(this, "Please set the map location.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please upload at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            val uploadedUrls = mutableListOf<String>()

            for (uri in selectedImageUris) {
                val url = CloudinaryHelper.uploadImage(this@CreateHotel, uri)
                if (url != null) {
                    uploadedUrls.add(url)
                } else {
                    Toast.makeText(this@CreateHotel, "Failed to upload an image", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }
            }

            val amenitiesList = amenitiesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // Format location as required by backend "lat,lng"
            val formattedLocation = "$selectedLat,$selectedLng"

            val newHotel = ManagerHotelDetailsDto(
                name = name,
                city = city,
                photos = uploadedUrls,
                amenities = amenitiesList,
                contactInfo = ManagerContactInfo(
                    address = address,
                    phoneNumber = phone,
                    email = email,
                    location = formattedLocation
                )
            )

            viewModel.createHotel(newHotel)
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading(true)
                    is UiState.Success -> {
                        showLoading(false)
                        Toast.makeText(this@CreateHotel, "Hotel Listed Successfully!", Toast.LENGTH_LONG).show()
                        viewModel.resetActionState()
                        finish()
                    }
                    is UiState.Error -> {
                        showLoading(false)
                        Toast.makeText(this@CreateHotel, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                    }
                    else -> {}
                }
            }
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