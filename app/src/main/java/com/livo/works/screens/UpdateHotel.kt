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
import com.livo.works.databinding.ActivityUpdateHotelBinding
import com.livo.works.util.ImageCompressor
import com.livo.works.util.UiState
import com.livo.works.util.toRelativeMediaPath
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpdateHotel : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateHotelBinding
    private val viewModel: ManagerViewModel by viewModels()
    private val uploadViewModel: MediaUploadViewModel by viewModels()

    private var hotelId: Long = -1L

    // Photos already saved on the hotel; kept as URLs and never re-uploaded.
    private var existingImageUrls = mutableListOf<String>()

    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    private var dotAnimators = mutableListOf<android.animation.ObjectAnimator>()
    private var isSubmitting = false
    private val photoStripRenderer by lazy { MediaStripRenderer(binding.layoutPhotoContainer) }

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ImageCompressor.MAX_IMAGES)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Compression, presign and upload all start here.
            uploadViewModel.addImages(uris)
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
        observeUploads()
        updateSubmitAvailability()

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
                        isSubmitting = false
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
        renderPhotoStrip()
        updateSubmitAvailability()
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

    private fun renderPhotoStrip() {
        photoStripRenderer.render(
            uploads = uploadViewModel.items.value,
            existingUrls = existingImageUrls,
            onRemoveExisting = { url ->
                existingImageUrls.remove(url)
                renderPhotoStrip()
                updateSubmitAvailability()
            },
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
            Toast.makeText(this, "Please set the map location.", Toast.LENGTH_SHORT).show()
            return
        }

        val uploadStatus = uploadViewModel.status.value

        if (existingImageUrls.isEmpty() && uploadStatus.total == 0) {
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

        val amenitiesList = amenitiesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val formattedLocation = "$selectedLat,$selectedLng"

        val updatedHotel = ManagerHotelDetailsDto(
            id = hotelId,
            name = name,
            city = city,
            // Already-saved photos come back as full CDN URLs - the backend
            // wants its own relative paths back, not the resolved URL - plus
            // the newly uploaded temp paths, as-is.
            photos = existingImageUrls.map { it.toRelativeMediaPath() } + uploadViewModel.uploadedTempPaths(),
            amenities = amenitiesList,
            contactInfo = ManagerContactInfo(
                address = address,
                phoneNumber = phone,
                email = email,
                location = formattedLocation
            ),
            active = true
        )

        isSubmitting = true
        viewModel.updateHotel(hotelId, updatedHotel)
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
                Toast.makeText(this@UpdateHotel, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Updating is allowed as long as no upload is running or failed, and at
     * least one photo (kept or newly uploaded) remains.
     */
    private fun updateSubmitAvailability() {
        val status = uploadViewModel.status.value
        val hasPhoto = existingImageUrls.isNotEmpty() || status.uploaded > 0
        binding.btnSubmit.isEnabled =
            !isSubmitting && hasPhoto && !status.isUploading && !status.hasFailed
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