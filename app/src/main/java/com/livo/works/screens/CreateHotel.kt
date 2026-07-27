package com.livo.works.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.livo.works.Manager.data.ManagerContactInfo
import com.livo.works.Manager.data.ManagerHotelDetailsDto
import com.livo.works.Upload.ui.MediaStripRenderer
import com.livo.works.ViewModel.ManagerViewModel
import com.livo.works.ViewModel.MediaUploadViewModel
import com.livo.works.databinding.ActivityCreateHotelBinding
import com.livo.works.util.ImageCompressor
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
    private val uploadViewModel: MediaUploadViewModel by viewModels()

    private var dotAnimators = mutableListOf<android.animation.ObjectAnimator>()
    private var isSubmitting = false
    private val photoStripRenderer by lazy { MediaStripRenderer(binding.layoutPhotoContainer) }

    // Hidden Coordinates
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    // Photo Picker
    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ImageCompressor.MAX_IMAGES)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Compression, presign and upload all start here.
            uploadViewModel.addImages(uris)
        }
    }

    // Location Permission Request
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openFullScreenMap()
        } else {
            Toast.makeText(this, "Location permission is needed to map the property.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateHotelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Implement AndroidX OnBackPressedDispatcher to handle the map overlay
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
        observeUploads()
        updateSubmitAvailability()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- PHOTOS LOGIC ---
        binding.btnAddPhotos.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // --- FULL SCREEN MAP LOGIC ---
        binding.btnSelectLocation.setOnClickListener {
            checkLocationPermissionAndStart()
        }

        binding.btnEditLocation.setOnClickListener {
            checkLocationPermissionAndStart()
        }

        // Close map without saving
        binding.btnCloseMap.setOnClickListener {
            binding.fullScreenMapOverlay.visibility = View.GONE
        }

        // Confirm pin from full screen map
        binding.btnConfirmFullMap.setOnClickListener {
            val centerPoint = binding.fullMapView.getMapboxMap().cameraState.center
            selectedLat = centerPoint.latitude()
            selectedLng = centerPoint.longitude()

            // Hide full screen map, show success inline
            binding.fullScreenMapOverlay.visibility = View.GONE
            binding.layoutLocationSetup.visibility = View.GONE
            binding.layoutLocationSuccess.visibility = View.VISIBLE
        }

        // --- SUBMIT ---
        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
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
        // Show Full Screen Overlay
        binding.fullScreenMapOverlay.visibility = View.VISIBLE

        val mapboxMap = binding.fullMapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)

        // Center on User Location
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
                // Fallback (Center of India)
                val fallbackPoint = Point.fromLngLat(78.9629, 20.5937)
                mapboxMap.setCamera(CameraOptions.Builder().center(fallbackPoint).zoom(5.0).build())
            }
        }
    }

    private fun renderPhotoStrip() {
        photoStripRenderer.render(
            uploads = uploadViewModel.items.value,
            onRemoveUpload = { uploadViewModel.remove(it.clientId) },
            onRetryUpload = { uploadViewModel.retry(it.clientId) }
        )
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
            Toast.makeText(this, "Please select a map location.", Toast.LENGTH_SHORT).show()
            return
        }

        val uploadStatus = uploadViewModel.status.value
        if (uploadStatus.total == 0) {
            Toast.makeText(this, "Please upload at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        if (uploadStatus.isUploading) {
            Toast.makeText(this, "Please wait for the photos to finish uploading", Toast.LENGTH_SHORT).show()
            return
        }

        if (uploadStatus.hasFailed) {
            Toast.makeText(this, "Some photos failed to upload. Retry or remove them.", Toast.LENGTH_SHORT).show()
            return
        }

        // Only successfully uploaded images are ever submitted.
        val mediaTempPaths = uploadViewModel.uploadedTempPaths()
        if (mediaTempPaths.isEmpty()) {
            Toast.makeText(this, "Please upload at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        val amenitiesList = amenitiesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val formattedLocation = "$selectedLat,$selectedLng"

        val newHotel = ManagerHotelDetailsDto(
            name = name,
            city = city,
            photos = mediaTempPaths,
            amenities = amenitiesList,
            contactInfo = ManagerContactInfo(
                address = address,
                phoneNumber = phone,
                email = email,
                location = formattedLocation
            )
        )

        isSubmitting = true
        viewModel.createHotel(newHotel)
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
                        isSubmitting = false
                        showLoading(false)
                        Toast.makeText(this@CreateHotel, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeUploads() {
        lifecycleScope.launch {
            uploadViewModel.items.collect { renderPhotoStrip() }
        }

        // Collected separately rather than read inside the items collector:
        // status is its own derived flow, so reading status.value from an items
        // emission can see a stale value and leave submit disabled for good.
        lifecycleScope.launch {
            uploadViewModel.status.collect { updateSubmitAvailability() }
        }

        lifecycleScope.launch {
            uploadViewModel.messages.collect { message ->
                Toast.makeText(this@CreateHotel, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Submission is only possible once every picked photo finished uploading. */
    private fun updateSubmitAvailability() {
        binding.btnSubmit.isEnabled = !isSubmitting && uploadViewModel.status.value.isReadyToSubmit
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAddPhotos.isEnabled = !isLoading
        isSubmitting = isLoading
        updateSubmitAvailability()

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